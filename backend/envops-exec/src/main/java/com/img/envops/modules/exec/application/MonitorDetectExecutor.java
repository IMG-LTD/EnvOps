package com.img.envops.modules.exec.application;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MonitorDetectExecutor {
  public DetectExecutionResult executeHostFacts(Long hostId, String hostName) {
    if (hostId == null || hostId <= 0) {
      throw new IllegalArgumentException("hostId is required");
    }

    if (hostId % 3 == 0) {
      throw new IllegalStateException("Probe agent did not respond in time");
    }

    int cpuCores = hostId % 2 == 0 ? 16 : 8;
    int memoryMb = hostId % 2 == 0 ? 32768 : 16384;
    String osName = hostId % 2 == 0 ? "Alibaba Cloud Linux 3" : "Ubuntu 22.04.4 LTS";
    String kernelVersion = hostId % 2 == 0 ? "5.10.134-18.al8" : "5.15.0-107-generic";
    String agentVersion = hostId % 2 == 0 ? "1.0.2" : "1.0.3";
    String resultLevel = hostId % 2 == 0 ? "warning" : "success";
    String resultMessage = hostId % 2 == 0 ? "Detected warning signals on runtime dependencies" : "Host baseline check passed";

    return new DetectExecutionResult(
        hostName,
        osName,
        kernelVersion,
        cpuCores,
        memoryMb,
        agentVersion,
        resultLevel,
        resultMessage,
        LocalDateTime.now());
  }

  public record DetectExecutionResult(String hostName,
                                      String osName,
                                      String kernelVersion,
                                      Integer cpuCores,
                                      Integer memoryMb,
                                      String agentVersion,
                                      String resultLevel,
                                      String resultMessage,
                                      LocalDateTime collectedAt) {
  }
}
