package com.img.envops.modules.traffic.application;

import com.img.envops.modules.traffic.plugin.TrafficPlugin;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TrafficApplicationService {
  private final List<TrafficPlugin> trafficPlugins;

  public TrafficApplicationService(List<TrafficPlugin> trafficPlugins) {
    this.trafficPlugins = trafficPlugins;
  }

  public List<TrafficPolicyRecord> getPolicies() {
    return List.of(
        new TrafficPolicyRecord(
            "payment-gateway",
            "header_canary",
            "prod / cn-shanghai-a",
            "20%",
            "traffic-team",
            "ENABLED",
            "NGINX"),
        new TrafficPolicyRecord(
            "traffic-admin",
            "blue_green",
            "staging / all",
            "100%",
            "release-team",
            "PREVIEW",
            "REST"));
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

  public record TrafficPolicyRecord(String app,
                                    String strategy,
                                    String scope,
                                    String trafficRatio,
                                    String owner,
                                    String status,
                                    String pluginType) {
  }

  public record TrafficPluginDirectoryRecord(String type,
                                             String name,
                                             String status,
                                             boolean supportsPreview,
                                             boolean supportsApply,
                                             boolean supportsRollback) {
  }
}
