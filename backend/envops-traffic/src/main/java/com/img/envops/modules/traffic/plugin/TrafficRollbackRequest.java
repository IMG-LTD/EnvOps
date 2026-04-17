package com.img.envops.modules.traffic.plugin;

public record TrafficRollbackRequest(String app,
                                     String rollbackToken,
                                     String reason) {
}
