package com.img.envops.modules.traffic.plugin;

import org.springframework.stereotype.Component;

@Component
public class NginxTrafficPlugin implements TrafficPlugin {
  public static final String PLUGIN_TYPE = "NGINX";
  public static final String PLUGIN_NAME = "Nginx Traffic Plugin";
  public static final String PLUGIN_STATUS = "NOT_IMPLEMENTED";
  private static final String MESSAGE = "NGINX traffic plugin skeleton only; real gateway integration is not connected yet.";

  @Override
  public String pluginType() {
    return PLUGIN_TYPE;
  }

  @Override
  public String pluginName() {
    return PLUGIN_NAME;
  }

  @Override
  public String pluginStatus() {
    return PLUGIN_STATUS;
  }

  @Override
  public boolean supportsPreview() {
    return false;
  }

  @Override
  public boolean supportsApply() {
    return false;
  }

  @Override
  public boolean supportsRollback() {
    return false;
  }

  @Override
  public TrafficPluginResult preview(TrafficActionRequest request) {
    return buildActionResult("preview", request);
  }

  @Override
  public TrafficPluginResult apply(TrafficActionRequest request) {
    return buildActionResult("apply", request);
  }

  @Override
  public TrafficPluginResult rollback(TrafficRollbackRequest request) {
    return new TrafficPluginResult(
        PLUGIN_TYPE,
        PLUGIN_STATUS,
        "rollback",
        MESSAGE,
        request.app(),
        null,
        null,
        null,
        null,
        request.rollbackToken(),
        request.reason());
  }

  private TrafficPluginResult buildActionResult(String action, TrafficActionRequest request) {
    return new TrafficPluginResult(
        PLUGIN_TYPE,
        PLUGIN_STATUS,
        action,
        MESSAGE,
        request.app(),
        request.strategy(),
        request.scope(),
        request.trafficRatio(),
        request.owner(),
        null,
        null);
  }
}
