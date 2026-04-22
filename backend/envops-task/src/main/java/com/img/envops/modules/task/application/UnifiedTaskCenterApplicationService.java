package com.img.envops.modules.task.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UnifiedTaskCenterApplicationService {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final Set<String> SUPPORTED_STATUSES = Set.of("pending", "running", "success", "failed");
  private static final TypeReference<LinkedHashMap<String, Object>> DETAIL_PREVIEW_TYPE =
      new TypeReference<>() {};

  private final UnifiedTaskCenterMapper unifiedTaskCenterMapper;
  private final ObjectMapper objectMapper;

  public UnifiedTaskCenterApplicationService(
      UnifiedTaskCenterMapper unifiedTaskCenterMapper,
      ObjectMapper objectMapper) {
    this.unifiedTaskCenterMapper = unifiedTaskCenterMapper;
    this.objectMapper = objectMapper;
  }

  public UnifiedTaskPage getTasks(UnifiedTaskQuery query) {
    NormalizedUnifiedTaskQuery normalized = normalizeQuery(query);
    long total = unifiedTaskCenterMapper.countByQuery(
        normalized.keyword(),
        normalized.taskType(),
        normalized.status(),
        normalized.startedFrom(),
        normalized.startedTo());
    List<UnifiedTaskRecord> records = unifiedTaskCenterMapper.findByQuery(
            normalized.keyword(),
            normalized.taskType(),
            normalized.status(),
            normalized.startedFrom(),
            normalized.startedTo(),
            normalized.pageSize(),
            normalized.offset())
        .stream()
        .map(this::toRecord)
        .toList();
    return new UnifiedTaskPage(normalized.page(), normalized.pageSize(), total, records);
  }

  public UnifiedTaskDetail getTaskDetail(Long id) {
    UnifiedTaskCenterRow row = unifiedTaskCenterMapper.findById(id);
    if (row == null) {
      throw new NotFoundException("unified task not found: " + id);
    }
    return new UnifiedTaskDetail(
        row.getId(),
        row.getTaskType(),
        row.getTaskName(),
        row.getStatus(),
        row.getTriggeredBy(),
        formatDateTime(row.getStartedAt()),
        formatDateTime(row.getFinishedAt()),
        row.getSummary(),
        parseDetailPreview(row),
        row.getSourceRoute(),
        row.getErrorSummary());
  }

  public static String normalizeStatus(String status) {
    String normalized = status == null ? "" : status.trim().toUpperCase();
    return switch (normalized) {
      case "RUNNING", "CANCEL_REQUESTED" -> "running";
      case "SUCCESS" -> "success";
      case "FAILED", "REJECTED", "CANCELLED" -> "failed";
      default -> "pending";
    };
  }

  private NormalizedUnifiedTaskQuery normalizeQuery(UnifiedTaskQuery query) {
    int page = query == null ? 1 : normalizePage(query.page());
    int pageSize = query == null ? 10 : normalizePageSize(query.pageSize());
    LocalDateTime startedFrom = query == null ? null : query.startedFrom();
    LocalDateTime startedTo = query == null ? null : query.startedTo();
    if (startedFrom != null && startedTo != null && startedFrom.isAfter(startedTo)) {
      throw new IllegalArgumentException("startedFrom must be before or equal to startedTo");
    }
    return new NormalizedUnifiedTaskQuery(
        query == null ? null : trimToNull(query.keyword()),
        normalizeTaskType(query == null ? null : query.taskType()),
        normalizeStatusFilter(query == null ? null : query.status()),
        startedFrom,
        startedTo,
        page,
        pageSize,
        (page - 1) * pageSize);
  }

  private int normalizePage(Integer page) {
    if (page == null) {
      return 1;
    }
    if (page < 1) {
      throw new IllegalArgumentException("page must be greater than 0");
    }
    return page;
  }

  private int normalizePageSize(Integer pageSize) {
    if (pageSize == null) {
      return 10;
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be greater than 0");
    }
    return pageSize;
  }

  private String normalizeTaskType(String taskType) {
    String normalized = trimToNull(taskType);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeStatusFilter(String status) {
    String normalized = trimToNull(status);
    if (normalized == null) {
      return null;
    }
    String lowerCaseStatus = normalized.toLowerCase(Locale.ROOT);
    if (!SUPPORTED_STATUSES.contains(lowerCaseStatus)) {
      throw new IllegalArgumentException("status must be one of pending, running, success, failed");
    }
    return lowerCaseStatus;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private UnifiedTaskRecord toRecord(UnifiedTaskCenterRow row) {
    return new UnifiedTaskRecord(
        row.getId(),
        row.getTaskType(),
        row.getTaskName(),
        row.getStatus(),
        row.getTriggeredBy(),
        formatDateTime(row.getStartedAt()),
        formatDateTime(row.getFinishedAt()),
        row.getSummary(),
        row.getSourceRoute(),
        row.getErrorSummary());
  }

  private Map<String, Object> parseDetailPreview(UnifiedTaskCenterRow row) {
    if (!StringUtils.hasText(row.getDetailPreview())) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(row.getDetailPreview(), DETAIL_PREVIEW_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("failed to parse detail preview for unified task: " + row.getId(), exception);
    }
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? null : DATE_TIME_FORMATTER.format(value);
  }

  public record UnifiedTaskQuery(
      String keyword,
      String taskType,
      String status,
      LocalDateTime startedFrom,
      LocalDateTime startedTo,
      Integer page,
      Integer pageSize) {}

  public record UnifiedTaskRecord(
      Long id,
      String taskType,
      String taskName,
      String status,
      String triggeredBy,
      String startedAt,
      String finishedAt,
      String summary,
      String sourceRoute,
      String errorSummary) {}

  public record UnifiedTaskDetail(
      Long id,
      String taskType,
      String taskName,
      String status,
      String triggeredBy,
      String startedAt,
      String finishedAt,
      String summary,
      Map<String, Object> detailPreview,
      String sourceRoute,
      String errorSummary) {}

  public record UnifiedTaskPage(Integer page, Integer pageSize, Long total, List<UnifiedTaskRecord> records) {}

  private record NormalizedUnifiedTaskQuery(
      String keyword,
      String taskType,
      String status,
      LocalDateTime startedFrom,
      LocalDateTime startedTo,
      int page,
      int pageSize,
      int offset) {}
}
