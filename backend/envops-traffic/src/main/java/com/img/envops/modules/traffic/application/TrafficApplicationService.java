package com.img.envops.modules.traffic.application;

import com.img.envops.common.exception.NotFoundException;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import com.img.envops.modules.traffic.infrastructure.mapper.TrafficPolicyMapper;
import com.img.envops.modules.traffic.plugin.TrafficActionRequest;
import com.img.envops.modules.traffic.plugin.TrafficPlugin;
import com.img.envops.modules.traffic.plugin.TrafficPluginResult;
import com.img.envops.modules.traffic.plugin.TrafficRollbackRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TrafficApplicationService {
  private static final String MVP_PLUGIN_TYPE = "REST";
  private static final String MVP_STRATEGY = "weighted_routing";
  private static final String READY_STATUS = "READY";
  private static final String TASK_TYPE = "traffic_action";
  private static final String MODULE_NAME = "traffic";
  private static final String SOURCE_ROUTE = "/traffic/controller";

  private final TrafficPolicyMapper trafficPolicyMapper;
  private final List<TrafficPlugin> trafficPlugins;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory;

  public TrafficApplicationService(TrafficPolicyMapper trafficPolicyMapper,
                                   List<TrafficPlugin> trafficPlugins,
                                   UnifiedTaskRecorder unifiedTaskRecorder,
                                   UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory) {
    this.trafficPolicyMapper = trafficPolicyMapper;
    this.trafficPlugins = trafficPlugins;
    this.unifiedTaskRecorder = unifiedTaskRecorder;
    this.unifiedTaskDetailPreviewFactory = unifiedTaskDetailPreviewFactory;
  }

  public List<TrafficPolicyRecord> getPolicies() {
    return trafficPolicyMapper.findAll().stream()
        .map(this::toTrafficPolicyRecord)
        .toList();
  }

  public List<TrafficPluginDirectoryRecord> getPlugins() {
    return trafficPlugins.stream()
        .sorted(Comparator.comparing(TrafficPlugin::pluginType))
        .map(plugin -> new TrafficPluginDirectoryRecord(
            plugin.pluginType(),
            plugin.pluginName(),
            plugin.pluginStatus(),
            plugin.supportsPreview(),
            plugin.supportsApply(),
            plugin.supportsRollback()))
        .toList();
  }

  public TrafficPolicyActionRecord previewPolicy(Long policyId) {
    TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
    validateMvpScope(policy);
    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "preview");
    Long unifiedTaskId = startTrafficTask("preview", policy, false);

    TrafficPluginResult pluginResult;
    try {
      pluginResult = plugin.preview(buildActionRequest(policy));
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "preview",
          policy,
          false,
          exception.getMessage(),
          exception);
    }

    boolean rollbackTokenAvailable = hasRollbackToken(pluginResult.rollbackToken());
    TrafficPolicyRecord updatedPolicy;
    try {
      updatedPolicy = updatePolicyState(policy.getId(), "PREVIEW", normalizeOptionalText(pluginResult.rollbackToken()));
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "preview",
          policy,
          rollbackTokenAvailable,
          exception.getMessage(),
          exception);
    }

    finishTrafficTask(unifiedTaskId, "preview", policy, rollbackTokenAvailable, "success", null);
    return new TrafficPolicyActionRecord("preview", updatedPolicy, pluginResult);
  }

  public TrafficPolicyActionRecord applyPolicy(Long policyId) {
    TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
    validateMvpScope(policy);
    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "apply");
    Long unifiedTaskId = startTrafficTask("apply", policy, false);

    TrafficPluginResult pluginResult;
    try {
      pluginResult = plugin.apply(buildActionRequest(policy));
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "apply",
          policy,
          false,
          exception.getMessage(),
          exception);
    }

    String rollbackToken = normalizeOptionalText(pluginResult.rollbackToken());
    if (rollbackToken == null) {
      IllegalArgumentException exception = new IllegalArgumentException(
          "rollbackToken is required from traffic rest service for apply: " + policyId);
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "apply",
          policy,
          false,
          "rollbackToken is required from traffic rest service",
          exception);
    }

    TrafficPolicyRecord updatedPolicy;
    try {
      updatedPolicy = updatePolicyState(policy.getId(), "ENABLED", rollbackToken);
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "apply",
          policy,
          true,
          exception.getMessage(),
          exception);
    }

    finishTrafficTask(unifiedTaskId, "apply", policy, true, "success", null);
    return new TrafficPolicyActionRecord("apply", updatedPolicy, pluginResult);
  }

  public TrafficPolicyActionRecord rollbackPolicy(Long policyId) {
    TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
    validateMvpScope(policy);
    String rollbackToken = normalizeOptionalText(policy.getRollbackToken());

    if (rollbackToken == null) {
      throw new IllegalArgumentException("rollbackToken is required for policy: " + policyId);
    }

    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "rollback");
    Long unifiedTaskId = startTrafficTask("rollback", policy, true);

    TrafficPluginResult pluginResult;
    try {
      pluginResult = plugin.rollback(new TrafficRollbackRequest(
          policy.getApp(),
          rollbackToken,
          "manual rollback"));
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "rollback",
          policy,
          true,
          exception.getMessage(),
          exception);
    }

    TrafficPolicyRecord updatedPolicy;
    try {
      updatedPolicy = updatePolicyState(policy.getId(), "ROLLED_BACK", rollbackToken);
    } catch (RuntimeException exception) {
      throw finishTrafficTaskOnFailure(
          unifiedTaskId,
          "rollback",
          policy,
          true,
          exception.getMessage(),
          exception);
    }

    finishTrafficTask(unifiedTaskId, "rollback", policy, true, "success", null);
    return new TrafficPolicyActionRecord("rollback", updatedPolicy, pluginResult);
  }

  private TrafficPolicyMapper.TrafficPolicyRow requirePolicy(Long policyId) {
    if (policyId == null) {
      throw new IllegalArgumentException("policyId is required");
    }

    TrafficPolicyMapper.TrafficPolicyRow policy = trafficPolicyMapper.findById(policyId);

    if (policy == null) {
      throw new NotFoundException("traffic policy not found: " + policyId);
    }

    return policy;
  }

  private TrafficPolicyRecord updatePolicyState(Long policyId, String status, String rollbackToken) {
    trafficPolicyMapper.updatePolicyState(policyId, status, rollbackToken, LocalDateTime.now());
    return toTrafficPolicyRecord(requirePolicy(policyId));
  }

  private TrafficActionRequest buildActionRequest(TrafficPolicyMapper.TrafficPolicyRow policy) {
    return new TrafficActionRequest(
        policy.getApp(),
        policy.getStrategy(),
        policy.getScope(),
        policy.getTrafficRatio(),
        policy.getOwner());
  }

  private void validateMvpScope(TrafficPolicyMapper.TrafficPolicyRow policy) {
    if (!MVP_PLUGIN_TYPE.equalsIgnoreCase(String.valueOf(policy.getPluginType()))) {
      throw new IllegalArgumentException("traffic plugin is not supported in v0.0.5: " + policy.getPluginType());
    }

    if (!MVP_STRATEGY.equalsIgnoreCase(String.valueOf(policy.getStrategy()))) {
      throw new IllegalArgumentException("traffic strategy is not supported in v0.0.5: " + policy.getStrategy());
    }
  }

  private TrafficPlugin requirePluginSupport(String pluginType, String action) {
    TrafficPlugin plugin = resolvePlugin(pluginType);

    if (!READY_STATUS.equals(plugin.pluginStatus())) {
      throw new IllegalArgumentException("traffic plugin is not ready: " + pluginType);
    }

    boolean supported = switch (action) {
      case "preview" -> plugin.supportsPreview();
      case "apply" -> plugin.supportsApply();
      case "rollback" -> plugin.supportsRollback();
      default -> false;
    };

    if (!supported) {
      throw new IllegalArgumentException("traffic plugin does not support " + action + ": " + pluginType);
    }

    return plugin;
  }

  private TrafficPlugin resolvePlugin(String pluginType) {
    String normalizedPluginType = normalizeLookupValue(pluginType);

    return trafficPlugins.stream()
        .filter(plugin -> normalizeLookupValue(plugin.pluginType()).equals(normalizedPluginType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("traffic plugin not found: " + pluginType));
  }

  private Long startTrafficTask(String action,
                                TrafficPolicyMapper.TrafficPolicyRow policy,
                                boolean rollbackTokenAvailable) {
    return unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
        TASK_TYPE,
        taskName(action),
        "running",
        null,
        LocalDateTime.now(),
        actionLabel(action) + " 执行中",
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
            action,
            policy.getApp(),
            policy.getStrategy(),
            policy.getPluginType(),
            rollbackTokenAvailable,
            SOURCE_ROUTE,
            null)),
        null,
        SOURCE_ROUTE,
        MODULE_NAME,
        null));
  }

  private void finishTrafficTask(Long unifiedTaskId,
                                 String action,
                                 TrafficPolicyMapper.TrafficPolicyRow policy,
                                 boolean rollbackTokenAvailable,
                                 String status,
                                 String errorSummary) {
    String summary = String.format(
        "%s %s，策略 %s，插件 %s",
        actionLabel(action),
        policy.getApp(),
        policy.getStrategy(),
        policy.getPluginType());

    unifiedTaskRecorder.update(new UnifiedTaskRecorder.UpdateCommand(
        unifiedTaskId,
        status,
        LocalDateTime.now(),
        summary,
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
            action,
            policy.getApp(),
            policy.getStrategy(),
            policy.getPluginType(),
            rollbackTokenAvailable,
            SOURCE_ROUTE,
            errorSummary)),
        errorSummary));
    updateTrafficTrackingSnapshot(unifiedTaskId, action, policy, rollbackTokenAvailable, status, errorSummary);
  }

  private void updateTrafficTrackingSnapshot(Long unifiedTaskId,
                                             String action,
                                             TrafficPolicyMapper.TrafficPolicyRow policy,
                                             boolean rollbackTokenAvailable,
                                             String status,
                                             String errorSummary) {
    UnifiedTaskRecorder.TrackingSnapshotCommand command = new UnifiedTaskRecorder.TrackingSnapshotCommand(
        unifiedTaskId,
        buildTrafficTimeline(action, status, errorSummary),
        buildTrafficLogSummary(action, policy, rollbackTokenAvailable, status, errorSummary),
        SOURCE_ROUTE);
    try {
      unifiedTaskRecorder.updateTrackingSnapshot(command);
    } catch (RuntimeException ignored) {
    }
  }

  private String buildTrafficTimeline(String action, String status, String errorSummary) {
    return unifiedTaskDetailPreviewFactory.toJsonArray(List.of(
        Map.of("label", "动作开始", "status", "success", "description", action + " 动作已提交"),
        Map.of("label", "动作完成", "status", status, "description", errorSummary == null ? action + " 动作完成" : errorSummary)));
  }

  private String buildTrafficLogSummary(String action,
                                        TrafficPolicyMapper.TrafficPolicyRow policy,
                                        boolean rollbackTokenAvailable,
                                        String status,
                                        String errorSummary) {
    String summary = action + " " + policy.getApp() + "，策略 " + policy.getStrategy()
        + "，插件 " + policy.getPluginType()
        + "，rollback token " + (rollbackTokenAvailable ? "可用" : "不可用")
        + "，状态 " + status;
    return errorSummary == null || errorSummary.isBlank() ? summary : summary + "；失败摘要：" + errorSummary;
  }

  private RuntimeException finishTrafficTaskOnFailure(Long unifiedTaskId,
                                                      String action,
                                                      TrafficPolicyMapper.TrafficPolicyRow policy,
                                                      boolean rollbackTokenAvailable,
                                                      String errorSummary,
                                                      RuntimeException exception) {
    try {
      finishTrafficTask(unifiedTaskId, action, policy, rollbackTokenAvailable, "failed", errorSummary);
    } catch (RuntimeException updateException) {
      exception.addSuppressed(updateException);
    }
    return exception;
  }

  private String taskName(String action) {
    return "Traffic " + actionLabel(action);
  }

  private String actionLabel(String action) {
    return switch (action) {
      case "preview" -> "Preview";
      case "apply" -> "Apply";
      case "rollback" -> "Rollback";
      default -> throw new IllegalArgumentException("unsupported traffic action: " + action);
    };
  }

  private TrafficPolicyRecord toTrafficPolicyRecord(TrafficPolicyMapper.TrafficPolicyRow row) {
    return new TrafficPolicyRecord(
        row.getId(),
        row.getApp(),
        row.getStrategy(),
        row.getScope(),
        row.getTrafficRatio(),
        row.getOwner(),
        row.getStatus(),
        row.getPluginType(),
        normalizeOptionalText(row.getRollbackToken()));
  }

  private String normalizeLookupValue(String value) {
    return String.valueOf(value)
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');
  }

  private boolean hasRollbackToken(String rollbackToken) {
    return normalizeOptionalText(rollbackToken) != null;
  }

  private String normalizeOptionalText(String value) {
    if (value == null) {
      return null;
    }

    String normalizedValue = value.trim();
    return normalizedValue.isEmpty() ? null : normalizedValue;
  }

  public record TrafficPolicyRecord(Long id,
                                    String app,
                                    String strategy,
                                    String scope,
                                    String trafficRatio,
                                    String owner,
                                    String status,
                                    String pluginType,
                                    String rollbackToken) {
  }

  public record TrafficPolicyActionRecord(String action,
                                          TrafficPolicyRecord policy,
                                          TrafficPluginResult pluginResult) {
  }

  public record TrafficPluginDirectoryRecord(String type,
                                             String name,
                                             String status,
                                             boolean supportsPreview,
                                             boolean supportsApply,
                                             boolean supportsRollback) {
  }
}
