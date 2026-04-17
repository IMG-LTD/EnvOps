package com.img.envops.modules.deploy.application;

import com.img.envops.modules.deploy.executor.RemoteExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeployExecutorApplicationService {
  private final RemoteExecutor remoteExecutor;

  public DeployExecutorApplicationService(RemoteExecutor remoteExecutor) {
    this.remoteExecutor = remoteExecutor;
  }

  public List<ExecutorDirectoryRecord> getExecutors() {
    return List.of(new ExecutorDirectoryRecord(
        remoteExecutor.executorType(),
        remoteExecutor.executorName(),
        remoteExecutor.executorStatus(),
        true,
        true,
        true));
  }

  public record ExecutorDirectoryRecord(String type,
                                        String name,
                                        String status,
                                        boolean supportsExec,
                                        boolean supportsUpload,
                                        boolean supportsDetect) {
  }
}
