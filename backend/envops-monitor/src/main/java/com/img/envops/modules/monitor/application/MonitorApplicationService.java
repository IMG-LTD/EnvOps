package com.img.envops.modules.monitor.application;

import com.img.envops.modules.exec.application.MonitorDetectExecutor;
import com.img.envops.modules.monitor.infrastructure.mapper.MonitorDetectTaskMapper;
import com.img.envops.modules.monitor.infrastructure.mapper.MonitorHostFactMapper;
import com.img.envops.modules.monitor.infrastructure.mapper.MonitorHostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorApplicationService {
  private final MonitorDetectTaskMapper monitorDetectTaskMapper;
  private final MonitorHostFactMapper monitorHostFactMapper;
  private final MonitorHostMapper monitorHostMapper;
  private final MonitorDetectExecutor monitorDetectExecutor;

  public MonitorApplicationService(MonitorDetectTaskMapper monitorDetectTaskMapper,
                                   MonitorHostFactMapper monitorHostFactMapper,
                                   MonitorHostMapper monitorHostMapper,
                                   MonitorDetectExecutor monitorDetectExecutor) {
    this.monitorDetectTaskMapper = monitorDetectTaskMapper;
    this.monitorHostFactMapper = monitorHostFactMapper;
    this.monitorHostMapper = monitorHostMapper;
    this.monitorDetectExecutor = monitorDetectExecutor;
  }

  public DetectTaskRecord createDetectTask(CreateDetectTaskCommand command) {
    if (command == null || !StringUtils.hasText(command.taskName()) || command.hostId() == null) {
      throw new IllegalArgumentException("taskName and hostId are required");
    }

    MonitorHostMapper.HostRow host = requireHost(command.hostId());

    MonitorDetectTaskMapper.DetectTaskEntity entity = new MonitorDetectTaskMapper.DetectTaskEntity();
    entity.setTaskName(command.taskName().trim());
    entity.setHostId(host.getId());
    entity.setTarget(host.getHostName());
    entity.setSchedule(StringUtils.hasText(command.schedule()) ? command.schedule().trim() : "manual");
    entity.setLastResult("pending");
    entity.setCreatedAt(LocalDateTime.now());

    monitorDetectTaskMapper.insertDetectTask(entity);

    MonitorDetectTaskMapper.DetectTaskRow created = monitorDetectTaskMapper.findById(entity.getId());
    if (created == null) {
      throw new IllegalStateException("Failed to create detect task");
    }

    return toDetectTaskRecord(created);
  }

  public List<DetectTaskRecord> getDetectTasks() {
    return monitorDetectTaskMapper.findAll().stream()
        .map(this::toDetectTaskRecord)
        .toList();
  }

  public DetectTaskRecord executeDetectTask(Long taskId) {
    MonitorDetectTaskMapper.DetectTaskRow task = requireDetectTask(taskId);
    MonitorHostMapper.HostRow host = requireHost(task.getHostId());
    LocalDateTime executedAt = LocalDateTime.now();

    try {
      MonitorDetectExecutor.DetectExecutionResult executionResult = monitorDetectExecutor.executeHostFacts(host.getId(), host.getHostName());

      MonitorHostFactMapper.HostFactEntity hostFactEntity = new MonitorHostFactMapper.HostFactEntity();
      hostFactEntity.setHostId(host.getId());
      hostFactEntity.setHostName(executionResult.hostName());
      hostFactEntity.setOsName(executionResult.osName());
      hostFactEntity.setKernelVersion(executionResult.kernelVersion());
      hostFactEntity.setCpuCores(executionResult.cpuCores());
      hostFactEntity.setMemoryMb(executionResult.memoryMb());
      hostFactEntity.setAgentVersion(executionResult.agentVersion());
      hostFactEntity.setCollectedAt(executionResult.collectedAt());
      monitorHostFactMapper.insertHostFact(hostFactEntity);

      monitorDetectTaskMapper.updateLastExecution(taskId, executedAt, executionResult.resultLevel());
    } catch (RuntimeException exception) {
      monitorDetectTaskMapper.updateLastExecution(taskId, executedAt, "failed");
      throw exception;
    }

    return toDetectTaskRecord(requireDetectTask(taskId));
  }

  public HostFactRecord getLatestHostFact(Long hostId) {
    MonitorHostMapper.HostRow host = requireHost(hostId);
    MonitorHostFactMapper.HostFactRow latest = monitorHostFactMapper.findLatestByHostId(host.getId());
    if (latest == null) {
      throw new IllegalArgumentException("latest host fact not found for hostId: " + hostId);
    }

    return new HostFactRecord(
        latest.getId(),
        latest.getHostId(),
        latest.getHostName(),
        latest.getOsName(),
        latest.getKernelVersion(),
        latest.getCpuCores(),
        latest.getMemoryMb(),
        latest.getAgentVersion(),
        latest.getCollectedAt());
  }

  private MonitorHostMapper.HostRow requireHost(Long hostId) {
    if (hostId == null) {
      throw new IllegalArgumentException("hostId is required");
    }

    MonitorHostMapper.HostRow host = monitorHostMapper.findById(hostId);
    if (host == null) {
      throw new IllegalArgumentException("host not found: " + hostId);
    }

    return host;
  }

  private MonitorDetectTaskMapper.DetectTaskRow requireDetectTask(Long taskId) {
    if (taskId == null) {
      throw new IllegalArgumentException("taskId is required");
    }

    MonitorDetectTaskMapper.DetectTaskRow task = monitorDetectTaskMapper.findById(taskId);
    if (task == null) {
      throw new IllegalArgumentException("detect task not found: " + taskId);
    }

    return task;
  }

  private DetectTaskRecord toDetectTaskRecord(MonitorDetectTaskMapper.DetectTaskRow row) {
    return new DetectTaskRecord(
        row.getId(),
        row.getTaskName(),
        row.getHostId(),
        row.getTarget(),
        row.getSchedule(),
        row.getLastRunAt(),
        row.getLastResult(),
        row.getCreatedAt());
  }

  public record CreateDetectTaskCommand(String taskName, Long hostId, String schedule) {
  }

  public record DetectTaskRecord(Long id,
                                 String taskName,
                                 Long hostId,
                                 String target,
                                 String schedule,
                                 LocalDateTime lastRunAt,
                                 String lastResult,
                                 LocalDateTime createdAt) {
  }

  public record HostFactRecord(Long id,
                               Long hostId,
                               String hostName,
                               String osName,
                               String kernelVersion,
                               Integer cpuCores,
                               Integer memoryMb,
                               String agentVersion,
                               LocalDateTime collectedAt) {
  }
}
