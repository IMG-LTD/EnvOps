package com.img.envops.modules.deploy.executor;

import org.springframework.util.StringUtils;

public record SshConnectionOptions(String sshUser, int sshPort, String privateKeyPath) {
  private static final int DEFAULT_SSH_PORT = 22;

  public static SshConnectionOptions from(String sshUser, Integer sshPort, String privateKeyPath) {
    if (!StringUtils.hasText(sshUser)) {
      throw new IllegalArgumentException("sshUser is required");
    }
    if (!StringUtils.hasText(privateKeyPath)) {
      throw new IllegalArgumentException("privateKeyPath is required");
    }

    int resolvedPort = sshPort == null ? DEFAULT_SSH_PORT : sshPort;
    if (resolvedPort <= 0) {
      throw new IllegalArgumentException("sshPort must be greater than 0");
    }

    return new SshConnectionOptions(sshUser.trim(), resolvedPort, privateKeyPath.trim());
  }
}
