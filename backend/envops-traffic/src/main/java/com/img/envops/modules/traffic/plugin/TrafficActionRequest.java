package com.img.envops.modules.traffic.plugin;

public record TrafficActionRequest(String app,
                                   String strategy,
                                   String scope,
                                   String trafficRatio,
                                   String owner) {
}
