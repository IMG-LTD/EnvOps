package com.img.envops.modules.deploy.executor;

public record ExecRequest(Long hostId,
                          String hostName,
                          String ipAddress,
                          String sshUser,
                          Integer sshPort,
                          String privateKeyPath,
                          String command) {
}
