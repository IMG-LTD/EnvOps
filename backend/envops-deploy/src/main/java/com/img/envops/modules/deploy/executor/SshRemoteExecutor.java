package com.img.envops.modules.deploy.executor;

import org.springframework.stereotype.Component;

@Component
public class SshRemoteExecutor implements RemoteExecutor {
  public static final String EXECUTOR_TYPE = "SSH_AGENTLESS";
  public static final String EXECUTOR_NAME = "SSH Agentless Executor";
  public static final String EXECUTOR_STATUS = "READY";
  public static final String DETECT_MESSAGE = "SSH executor adapter is ready for task-scoped detect requests.";

  private final SshProcessRunner sshProcessRunner;

  public SshRemoteExecutor(SshProcessRunner sshProcessRunner) {
    this.sshProcessRunner = sshProcessRunner;
  }

  @Override
  public String executorType() {
    return EXECUTOR_TYPE;
  }

  @Override
  public String executorName() {
    return EXECUTOR_NAME;
  }

  @Override
  public String executorStatus() {
    return EXECUTOR_STATUS;
  }

  @Override
  public ExecResult exec(ExecRequest request) {
    SshConnectionOptions options = SshConnectionOptions.from(
        request.sshUser(),
        request.sshPort(),
        request.privateKeyPath());
    String output = sshProcessRunner.exec(request.ipAddress(), options, request.command());
    return new ExecResult(
        EXECUTOR_TYPE,
        "SUCCESS",
        output,
        request.hostId(),
        request.hostName(),
        request.ipAddress(),
        request.command());
  }

  @Override
  public UploadResult upload(FileUploadRequest request) {
    SshConnectionOptions options = SshConnectionOptions.from(
        request.sshUser(),
        request.sshPort(),
        request.privateKeyPath());
    String output = sshProcessRunner.upload(
        request.ipAddress(),
        options,
        request.sourcePath(),
        request.targetPath());
    return new UploadResult(
        EXECUTOR_TYPE,
        "SUCCESS",
        output,
        request.hostId(),
        request.hostName(),
        request.ipAddress(),
        request.sourcePath(),
        request.targetPath());
  }

  @Override
  public DetectResult detect(DetectRequest request) {
    return new DetectResult(
        EXECUTOR_TYPE,
        EXECUTOR_STATUS,
        DETECT_MESSAGE,
        request.hostId(),
        request.hostName(),
        request.ipAddress(),
        request.probe());
  }
}
