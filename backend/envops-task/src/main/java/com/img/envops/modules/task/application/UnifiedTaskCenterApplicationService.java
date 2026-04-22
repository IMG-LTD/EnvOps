package com.img.envops.modules.task.application;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UnifiedTaskCenterApplicationService {

  public static String normalizeStatus(String status) {
    String normalized = status == null ? "" : status.trim().toUpperCase();
    return switch (normalized) {
      case "RUNNING", "CANCEL_REQUESTED" -> "running";
      case "SUCCESS" -> "success";
      case "FAILED", "REJECTED", "CANCELLED" -> "failed";
      default -> "pending";
    };
  }

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
}
