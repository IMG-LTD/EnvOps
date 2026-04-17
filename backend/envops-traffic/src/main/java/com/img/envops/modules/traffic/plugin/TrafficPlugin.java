package com.img.envops.modules.traffic.plugin;

public interface TrafficPlugin {
  String pluginType();

  String pluginName();

  String pluginStatus();

  default boolean supportsPreview() {
    return true;
  }

  default boolean supportsApply() {
    return true;
  }

  default boolean supportsRollback() {
    return true;
  }

  TrafficPluginResult preview(TrafficActionRequest request);

  TrafficPluginResult apply(TrafficActionRequest request);

  TrafficPluginResult rollback(TrafficRollbackRequest request);
}
