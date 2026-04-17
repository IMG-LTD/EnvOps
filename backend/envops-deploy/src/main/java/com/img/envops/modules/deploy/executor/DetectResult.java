package com.img.envops.modules.deploy.executor;

public record DetectResult(String executorType,
                           String status,
                           String message,
                           Long hostId,
                           String hostName,
                           String ipAddress,
                           String probe) {
}
