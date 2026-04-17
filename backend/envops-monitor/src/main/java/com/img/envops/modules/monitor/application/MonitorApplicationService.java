package com.img.envops.modules.monitor.application;

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

  public MonitorApplicationService(MonitorDetectTaskMapper monitorDetectTaskMapper,
                                   MonitorHostFactMapper monitorHostFactMapper,
                                   MonitorHostMapper monitorHostMapper) {
    this.monitorDetectTaskMapper = monitorDetectTaskMapper;
    this.monitorHostFactMapper = monitorHostFactMapper;
    this.monitorHostMapper = monitorHostMapper;
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
