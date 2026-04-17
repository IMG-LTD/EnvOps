package com.img.envops.modules.deploy.executor;

public record ExecResult(String executorType,
                         String status,
                         String message,
                         Long hostId,
                         String hostName,
                         String ipAddress,
                         String command) {
}
