package com.img.envops.modules.deploy.executor;

public interface SshProcessRunner {
  String exec(String ipAddress, SshConnectionOptions options, String command);

  String upload(String ipAddress, SshConnectionOptions options, String sourcePath, String targetPath);
}
