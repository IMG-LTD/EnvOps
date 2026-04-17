package com.img.envops.modules.deploy.executor;

public record FileUploadRequest(Long hostId,
                                String hostName,
                                String ipAddress,
                                String sshUser,
                                Integer sshPort,
                                String privateKeyPath,
                                String sourcePath,
                                String targetPath) {
}
