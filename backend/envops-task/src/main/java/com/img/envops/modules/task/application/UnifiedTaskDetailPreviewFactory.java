package com.img.envops.modules.task.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskDetailPreviewFactory {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<String, Object> buildDeployPreview(
      String app,
      String environment,
      long targetCount,
      long successCount,
      long failCount,
      String rawStatus,
      String sourceRoute) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("app", app);
    preview.put("environment", environment);
    preview.put("targetCount", targetCount);
    preview.put("successCount", successCount);
    preview.put("failCount", failCount);
    preview.put("rawStatus", rawStatus);
    preview.put("sourceRoute", sourceRoute);
    return preview;
  }

  public Map<String, Object> buildDatabaseConnectivityPreview(
      boolean batch,
      String summary,
      long total,
      long success,
      long failed,
      long skipped,
      String sourceRoute,
      String errorSummary) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("mode", batch ? "batch" : "single");
    preview.put("summary", summary);
    preview.put("total", total);
    preview.put("success", success);
    preview.put("failed", failed);
    preview.put("skipped", skipped);
    preview.put("sourceRoute", sourceRoute);
    preview.put("errorSummary", errorSummary);
    return preview;
  }

  public Map<String, Object> buildTrafficActionPreview(
      String action,
      String app,
      String strategy,
      String plugin,
      boolean rollbackTokenAvailable,
      String sourceRoute,
      String errorSummary) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("action", action);
    preview.put("app", app);
    preview.put("strategy", strategy);
    preview.put("plugin", plugin);
    preview.put("rollbackTokenAvailable", rollbackTokenAvailable);
    preview.put("sourceRoute", sourceRoute);
    preview.put("errorSummary", errorSummary);
    return preview;
  }

  public String toJson(Map<String, Object> preview) {
    try {
      return objectMapper.writeValueAsString(preview);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("failed to serialize detail preview", exception);
    }
  }
}
