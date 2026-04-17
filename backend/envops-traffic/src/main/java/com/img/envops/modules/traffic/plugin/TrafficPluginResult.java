package com.img.envops.modules.traffic.plugin;

public record TrafficPluginResult(String pluginType,
                                  String status,
                                  String action,
                                  String message,
                                  String app,
                                  String strategy,
                                  String scope,
                                  String trafficRatio,
                                  String owner,
                                  String rollbackToken,
                                  String reason) {
}
