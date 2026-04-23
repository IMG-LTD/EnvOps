package com.img.envops.modules.task.application.tracking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TrackingViewSupport {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final TypeReference<LinkedHashMap<String, Object>> DETAIL_PREVIEW_TYPE =
      new TypeReference<>() {};
  private static final TypeReference<List<UnifiedTaskTimelineItem>> TIMELINE_TYPE =
      new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  public TrackingViewSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> parseDetailPreview(UnifiedTaskCenterRow row) {
    if (row == null || !StringUtils.hasText(row.getDetailPreview())) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(row.getDetailPreview(), DETAIL_PREVIEW_TYPE);
    } catch (JsonProcessingException exception) {
      return Map.of();
    }
  }

  public List<UnifiedTaskTimelineItem> parseTimeline(UnifiedTaskCenterRow row) {
    if (row == null || !StringUtils.hasText(row.getTrackingTimeline())) {
      return List.of();
    }
    try {
      List<UnifiedTaskTimelineItem> timeline = objectMapper.readValue(row.getTrackingTimeline(), TIMELINE_TYPE);
      return timeline == null ? List.of() : timeline;
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  public List<UnifiedTaskTimelineItem> fallbackTimeline(UnifiedTaskCenterRow row) {
    if (row == null) {
      return List.of();
    }
    List<UnifiedTaskTimelineItem> timeline = new ArrayList<>();
    timeline.add(new UnifiedTaskTimelineItem(
        "任务开始",
        normalizeTimelineStatus(row.getStatus()),
        formatDateTime(row.getStartedAt()),
        row.getSummary()));
    if (row.getFinishedAt() != null) {
      boolean failed = "failed".equalsIgnoreCase(row.getStatus());
      timeline.add(new UnifiedTaskTimelineItem(
          failed ? "任务失败" : "任务完成",
          failed ? "failed" : "success",
          formatDateTime(row.getFinishedAt()),
          failed && StringUtils.hasText(row.getErrorSummary()) ? row.getErrorSummary() : row.getSummary()));
    }
    return List.copyOf(timeline);
  }

  public List<UnifiedTaskSourceLink> sourceLinks(String sourceRoute, String logRoute) {
    List<UnifiedTaskSourceLink> links = new ArrayList<>();
    if (StringUtils.hasText(sourceRoute)) {
      links.add(new UnifiedTaskSourceLink("source", "来源任务", sourceRoute.trim()));
    }
    if (StringUtils.hasText(logRoute)) {
      String trimmedLogRoute = logRoute.trim();
      boolean alreadyPresent = links.stream().anyMatch(link -> trimmedLogRoute.equals(link.route()));
      if (!alreadyPresent) {
        links.add(new UnifiedTaskSourceLink("log", "任务日志", trimmedLogRoute));
      }
    }
    return List.copyOf(links);
  }

  public String fallbackLogSummary(UnifiedTaskCenterRow row) {
    if (row == null) {
      return "暂无日志摘要";
    }
    if (StringUtils.hasText(row.getTrackingLogSummary())) {
      return row.getTrackingLogSummary().trim();
    }
    if (StringUtils.hasText(row.getSummary()) && StringUtils.hasText(row.getErrorSummary())) {
      return row.getSummary().trim() + "；" + row.getErrorSummary().trim();
    }
    if (StringUtils.hasText(row.getSummary())) {
      return row.getSummary().trim();
    }
    return "暂无日志摘要";
  }

  public String formatDateTime(LocalDateTime value) {
    return value == null ? null : DATE_TIME_FORMATTER.format(value);
  }

  private String normalizeTimelineStatus(String status) {
    if ("failed".equalsIgnoreCase(status)) {
      return "failed";
    }
    if ("success".equalsIgnoreCase(status)) {
      return "success";
    }
    if ("running".equalsIgnoreCase(status)) {
      return "running";
    }
    return "pending";
  }
}
