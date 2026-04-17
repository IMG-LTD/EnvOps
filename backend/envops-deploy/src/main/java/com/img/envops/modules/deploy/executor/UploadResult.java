package com.img.envops.modules.deploy.executor;

public record UploadResult(String executorType,
                           String status,
                           String message,
                           Long hostId,
                           String hostName,
                           String ipAddress,
                           String sourcePath,
                           String targetPath) {
}
