package com.img.envops.modules.deploy.executor;

public interface RemoteExecutor {
  String executorType();

  String executorName();

  String executorStatus();

  ExecResult exec(ExecRequest request);

  UploadResult upload(FileUploadRequest request);

  DetectResult detect(DetectRequest request);
}
