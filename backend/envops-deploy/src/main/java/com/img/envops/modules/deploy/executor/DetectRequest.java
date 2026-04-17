package com.img.envops.modules.deploy.executor;

public record DetectRequest(Long hostId,
                            String hostName,
                            String ipAddress,
                            String probe) {
}
