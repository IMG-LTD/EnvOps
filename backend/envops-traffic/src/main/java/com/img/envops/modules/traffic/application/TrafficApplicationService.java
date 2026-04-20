package com.img.envops.modules.traffic.application;

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

@Service
public class TrafficApplicationService {
  private final TrafficPolicyMapper trafficPolicyMapper;
  private final List<TrafficPlugin> trafficPlugins;

  public TrafficApplicationService(TrafficPolicyMapper trafficPolicyMapper, List<TrafficPlugin> trafficPlugins) {
    this.trafficPolicyMapper = trafficPolicyMapper;
    this.trafficPlugins = trafficPlugins;
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
    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "preview");
    TrafficPluginResult pluginResult = plugin.preview(buildActionRequest(policy));
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "PREVIEW", normalizeOptionalText(policy.getRollbackToken()));

    return new TrafficPolicyActionRecord("preview", updatedPolicy, pluginResult);
  }

  public TrafficPolicyActionRecord applyPolicy(Long policyId) {
    TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "apply");
    TrafficPluginResult pluginResult = plugin.apply(buildActionRequest(policy));
    String rollbackToken = getRollbackTokenOrGenerate(policy);
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ENABLED", rollbackToken);

    return new TrafficPolicyActionRecord("apply", updatedPolicy, pluginResult);
  }

  public TrafficPolicyActionRecord rollbackPolicy(Long policyId) {
    TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
    String rollbackToken = normalizeOptionalText(policy.getRollbackToken());

    if (rollbackToken == null) {
      throw new IllegalArgumentException("rollbackToken is required for policy: " + policyId);
    }

    TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "rollback");
    TrafficPluginResult pluginResult = plugin.rollback(new TrafficRollbackRequest(
        policy.getApp(),
        rollbackToken,
        "manual rollback"));
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ENABLED", rollbackToken);

    return new TrafficPolicyActionRecord("rollback", updatedPolicy, pluginResult);
  }

  private TrafficPolicyMapper.TrafficPolicyRow requirePolicy(Long policyId) {
    if (policyId == null) {
      throw new IllegalArgumentException("policyId is required");
    }

    TrafficPolicyMapper.TrafficPolicyRow policy = trafficPolicyMapper.findById(policyId);

    if (policy == null) {
      throw new IllegalArgumentException("traffic policy not found: " + policyId);
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

  private TrafficPlugin requirePluginSupport(String pluginType, String action) {
    TrafficPlugin plugin = resolvePlugin(pluginType);

    if (!"READY".equals(plugin.pluginStatus())) {
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

  private String getRollbackTokenOrGenerate(TrafficPolicyMapper.TrafficPolicyRow policy) {
    String rollbackToken = normalizeOptionalText(policy.getRollbackToken());

    if (rollbackToken != null) {
      return rollbackToken;
    }

    return "traffic-rb-" + policy.getId();
  }

  private String normalizeLookupValue(String value) {
    return String.valueOf(value)
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');
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
