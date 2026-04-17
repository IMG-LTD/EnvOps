package com.img.envops.modules.deploy.application;

import com.img.envops.modules.app.infrastructure.mapper.AppDefinitionMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppVersionMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetHostMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskHostMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskLogMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskParamMapper;
import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DeployTaskApplicationService {
  private static final List<String> SUPPORTED_TASK_TYPES = List.of("INSTALL", "UPGRADE", "ROLLBACK");
  private static final List<String> SUPPORTED_BATCH_STRATEGIES = List.of("ALL", "ROLLING");
  private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_REJECTED = "REJECTED";
  private static final String STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
  private static final List<String> SUPPORTED_STATUSES = List.of("INIT", STATUS_PENDING_APPROVAL, STATUS_PENDING, "RUNNING", "SUCCESS", "FAILED", STATUS_REJECTED, STATUS_CANCEL_REQUESTED, "CANCELLED");
  private static final List<String> SECRET_PARAM_KEYS = List.of("token", "secret", "password", "credential", "privatekey", "accesskey", "secretkey", "apikey", "sshkey", "rollbackcommand");
  private static final DateTimeFormatter TASK_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final DeployTaskMapper deployTaskMapper;
  private final DeployTaskHostMapper deployTaskHostMapper;
  private final DeployTaskLogMapper deployTaskLogMapper;
  private final DeployTaskParamMapper deployTaskParamMapper;
  private final AppDefinitionMapper appDefinitionMapper;
  private final AppVersionMapper appVersionMapper;
  private final AssetHostMapper assetHostMapper;

  public DeployTaskApplicationService(DeployTaskMapper deployTaskMapper,
                                      DeployTaskHostMapper deployTaskHostMapper,
                                      DeployTaskLogMapper deployTaskLogMapper,
                                      DeployTaskParamMapper deployTaskParamMapper,
                                      AppDefinitionMapper appDefinitionMapper,
                                      AppVersionMapper appVersionMapper,
                                      AssetHostMapper assetHostMapper) {
    this.deployTaskMapper = deployTaskMapper;
    this.deployTaskHostMapper = deployTaskHostMapper;
    this.deployTaskLogMapper = deployTaskLogMapper;
    this.deployTaskParamMapper = deployTaskParamMapper;
    this.appDefinitionMapper = appDefinitionMapper;
    this.appVersionMapper = appVersionMapper;
    this.assetHostMapper = assetHostMapper;
  }

  public List<DeployTaskRecord> getDeployTasks() {
    return deployTaskMapper.findAllActive().stream()
        .map(row -> toDeployTaskRecord(row, loadParams(row.getId())))
        .toList();
  }

  public DeployTaskRecord getDeployTask(Long id) {
    return toDeployTaskRecord(requireTask(id), loadParams(id));
  }

  @Transactional
  public DeployTaskRecord createDeployTask(CreateDeployTaskCommand command, String operatorName) {
    ValidatedCreateCommand validated = validateCreateCommand(command);
    LocalDateTime now = LocalDateTime.now();
    String resolvedOperatorName = resolveOperatorName(operatorName);

    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setTaskNo(generateTaskNo(now));
    entity.setTaskName(validated.taskName());
    entity.setTaskType(validated.taskType());
    entity.setAppId(validated.app().getId());
    entity.setVersionId(validated.version().getId());
    entity.setStatus(STATUS_PENDING_APPROVAL);
    entity.setOriginTaskId(null);
    entity.setBatchStrategy(validated.batchStrategy());
    entity.setBatchSize(validated.batchSize());
    entity.setTargetCount(validated.hostIds().size());
    entity.setSuccessCount(0);
    entity.setFailCount(0);
    entity.setOperatorName(resolvedOperatorName);
    entity.setDeleted(0);
    entity.setCreatedBy(resolvedOperatorName);
    entity.setUpdatedBy(resolvedOperatorName);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    deployTaskMapper.insertTask(entity);

    for (Long hostId : validated.hostIds()) {
      DeployTaskHostMapper.DeployTaskHostEntity hostEntity = new DeployTaskHostMapper.DeployTaskHostEntity();
      hostEntity.setTaskId(entity.getId());
      hostEntity.setHostId(hostId);
      hostEntity.setStatus(STATUS_PENDING);
      deployTaskHostMapper.insertTaskHost(hostEntity);
    }

    for (Map.Entry<String, String> entry : validated.params().entrySet()) {
      DeployTaskParamMapper.DeployTaskParamEntity paramEntity = new DeployTaskParamMapper.DeployTaskParamEntity();
      paramEntity.setTaskId(entity.getId());
      paramEntity.setParamKey(entry.getKey());
      paramEntity.setParamValue(entry.getValue());
      paramEntity.setSecretFlag(isSecretParamKey(entry.getKey()) ? 1 : 0);
      deployTaskParamMapper.insertParam(paramEntity);
    }

    DeployTaskLogMapper.DeployTaskLogEntity logEntity = new DeployTaskLogMapper.DeployTaskLogEntity();
    logEntity.setTaskId(entity.getId());
    logEntity.setLogLevel("INFO");
    logEntity.setLogContent("Task created");
    logEntity.setCreatedAt(now);
    deployTaskLogMapper.insertLog(logEntity);

    return getDeployTask(entity.getId());
  }

  @Transactional
  public DeployTaskRecord approveDeployTask(Long taskId, String comment, String operatorName) {
    return decideApproval(taskId, STATUS_PENDING, comment, operatorName, "Task approved");
  }

  @Transactional
  public DeployTaskRecord rejectDeployTask(Long taskId, String comment, String operatorName) {
    return decideApproval(taskId, STATUS_REJECTED, comment, operatorName, "Task rejected");
  }

  public List<DeployTaskHostRecord> getDeployTaskHosts(Long taskId) {
    requireTask(taskId);
    return deployTaskHostMapper.findByTaskId(taskId).stream()
        .map(row -> new DeployTaskHostRecord(
            row.getId(),
            row.getTaskId(),
            row.getHostId(),
            row.getHostName(),
            row.getIpAddress(),
            row.getStatus(),
            row.getCurrentStep(),
            row.getStartedAt(),
            row.getFinishedAt(),
            row.getErrorMsg()))
        .toList();
  }

  public List<DeployTaskLogRecord> getDeployTaskLogs(Long taskId) {
    requireTask(taskId);
    return deployTaskLogMapper.findByTaskId(taskId).stream()
        .map(row -> new DeployTaskLogRecord(
            row.getId(),
            row.getTaskId(),
            row.getTaskHostId(),
            row.getLogLevel(),
            row.getLogContent(),
            row.getCreatedAt()))
        .toList();
  }

  public List<TaskCenterRecord> getTaskCenterTasks() {
    return deployTaskMapper.findAllActive().stream()
        .map(row -> new TaskCenterRecord(
            row.getId(),
            "DEPLOY",
            row.getTaskNo(),
            row.getTaskName(),
            row.getTaskType(),
            row.getStatus(),
            row.getAppId(),
            row.getAppName(),
            row.getVersionId(),
            row.getVersionNo(),
            row.getTargetCount(),
            row.getSuccessCount(),
            row.getFailCount(),
            row.getOperatorName(),
            row.getApprovalOperatorName(),
            row.getApprovalComment(),
            row.getApprovalAt(),
            row.getCreatedAt(),
            row.getUpdatedAt()))
        .toList();
  }

  private ValidatedCreateCommand validateCreateCommand(CreateDeployTaskCommand command) {
    if (command == null
        || !StringUtils.hasText(command.taskName())
        || !StringUtils.hasText(command.taskType())
        || command.appId() == null
        || command.versionId() == null
        || command.hostIds() == null) {
      throw new IllegalArgumentException("taskName, taskType, appId, versionId and hostIds are required");
    }

    String taskType = command.taskType().trim();
    if (!SUPPORTED_TASK_TYPES.contains(taskType)) {
      throw new IllegalArgumentException("taskType must be one of INSTALL, UPGRADE, ROLLBACK");
    }

    Set<Long> deduplicatedHostIds = new LinkedHashSet<>();
    for (Long hostId : command.hostIds()) {
      if (hostId != null) {
        deduplicatedHostIds.add(hostId);
      }
    }
    if (deduplicatedHostIds.isEmpty()) {
      throw new IllegalArgumentException("hostIds must not be empty");
    }

    String batchStrategy = StringUtils.hasText(command.batchStrategy()) ? command.batchStrategy().trim() : "ALL";
    if (!SUPPORTED_BATCH_STRATEGIES.contains(batchStrategy)) {
      throw new IllegalArgumentException("batchStrategy must be one of ALL, ROLLING");
    }

    Integer batchSize = command.batchSize() == null ? 0 : command.batchSize();
    if ("ROLLING".equals(batchStrategy) && batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be greater than 0 when batchStrategy is ROLLING");
    }
    if ("ALL".equals(batchStrategy)) {
      batchSize = 0;
    }

    AppDefinitionMapper.AppDefinitionRow app = appDefinitionMapper.findActiveById(command.appId());
    if (app == null) {
      throw new IllegalArgumentException("app not found: " + command.appId());
    }

    AppVersionMapper.AppVersionRow version = appVersionMapper.findActiveById(command.versionId());
    if (version == null) {
      throw new IllegalArgumentException("version not found: " + command.versionId());
    }
    if (!app.getId().equals(version.getAppId())) {
      throw new IllegalArgumentException("versionId does not belong to appId");
    }

    for (Long hostId : deduplicatedHostIds) {
      if (assetHostMapper.findById(hostId) == null) {
        throw new IllegalArgumentException("host not found: " + hostId);
      }
    }

    Map<String, String> normalizedParams = new LinkedHashMap<>();
    if (command.params() != null) {
      for (Map.Entry<String, Object> entry : command.params().entrySet()) {
        if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
          continue;
        }
        normalizedParams.put(entry.getKey().trim(), String.valueOf(entry.getValue()));
      }
    }

    return new ValidatedCreateCommand(
        command.taskName().trim(),
        taskType,
        app,
        version,
        List.copyOf(deduplicatedHostIds),
        batchStrategy,
        batchSize,
        normalizedParams);
  }

  private DeployTaskMapper.DeployTaskRow requireTask(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("taskId is required");
    }

    DeployTaskMapper.DeployTaskRow row = deployTaskMapper.findActiveById(id);
    if (row == null) {
      throw new NotFoundException("deploy task not found: " + id);
    }

    return row;
  }

  private Map<String, String> loadParams(Long taskId) {
    Map<String, String> params = new LinkedHashMap<>();
    for (DeployTaskParamMapper.DeployTaskParamRow row : deployTaskParamMapper.findByTaskId(taskId)) {
      if ((row.getSecretFlag() != null && row.getSecretFlag() == 1) || isSecretParamKey(row.getParamKey())) {
        continue;
      }
      params.put(row.getParamKey(), row.getParamValue());
    }
    return params;
  }

  private DeployTaskRecord decideApproval(Long taskId,
                                          String targetStatus,
                                          String comment,
                                          String operatorName,
                                          String logContent) {
    DeployTaskMapper.DeployTaskRow task = requireTask(taskId);
    if (!STATUS_PENDING_APPROVAL.equals(task.getStatus())) {
      throw new ConflictException("deploy task is not pending approval: " + taskId);
    }

    LocalDateTime now = LocalDateTime.now();
    String resolvedOperatorName = resolveOperatorName(operatorName);

    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setStatus(targetStatus);
    entity.setApprovalOperatorName(resolvedOperatorName);
    entity.setApprovalComment(normalizeApprovalComment(comment));
    entity.setApprovalAt(now);
    entity.setUpdatedBy(resolvedOperatorName);
    entity.setUpdatedAt(now);
    int updatedRows = deployTaskMapper.updateApprovalDecision(entity);
    if (updatedRows == 0) {
      throw new ConflictException("deploy task is not pending approval: " + taskId);
    }

    DeployTaskLogMapper.DeployTaskLogEntity logEntity = new DeployTaskLogMapper.DeployTaskLogEntity();
    logEntity.setTaskId(taskId);
    logEntity.setLogLevel("INFO");
    logEntity.setLogContent(logContent);
    logEntity.setCreatedAt(now);
    deployTaskLogMapper.insertLog(logEntity);

    return getDeployTask(taskId);
  }

  private String generateTaskNo(LocalDateTime now) {
    return "DT" + TASK_NO_TIME_FORMATTER.format(now) + String.format("%04d", now.getNano() / 100000);
  }

  private String resolveOperatorName(String operatorName) {
    if (!StringUtils.hasText(operatorName)) {
      throw new IllegalArgumentException("operatorName is required");
    }

    return operatorName.trim();
  }

  private String normalizeApprovalComment(String comment) {
    if (!StringUtils.hasText(comment)) {
      return null;
    }

    return comment.trim();
  }

  private boolean isSecretParamKey(String key) {
    if (!StringUtils.hasText(key)) {
      return false;
    }

    String normalizedKey = key.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    return SECRET_PARAM_KEYS.stream().anyMatch(normalizedKey::contains);
  }

  private DeployTaskRecord toDeployTaskRecord(DeployTaskMapper.DeployTaskRow row, Map<String, String> params) {
    if (!SUPPORTED_STATUSES.contains(row.getStatus())) {
      throw new IllegalStateException("unsupported deploy task status: " + row.getStatus());
    }

    return new DeployTaskRecord(
        row.getId(),
        row.getTaskNo(),
        row.getTaskName(),
        row.getTaskType(),
        row.getAppId(),
        row.getAppName(),
        row.getVersionId(),
        row.getVersionNo(),
        row.getOriginTaskId(),
        row.getStatus(),
        row.getBatchStrategy(),
        row.getBatchSize(),
        row.getTargetCount(),
        row.getSuccessCount(),
        row.getFailCount(),
        row.getOperatorName(),
        row.getApprovalOperatorName(),
        row.getApprovalComment(),
        row.getApprovalAt(),
        row.getStartedAt(),
        row.getFinishedAt(),
        row.getCreatedAt(),
        row.getUpdatedAt(),
        params);
  }

  public record CreateDeployTaskCommand(String taskName,
                                        String taskType,
                                        Long appId,
                                        Long versionId,
                                        List<Long> hostIds,
                                        String batchStrategy,
                                        Integer batchSize,
                                        Map<String, Object> params) {
  }

  public record DeployTaskRecord(Long id,
                                 String taskNo,
                                 String taskName,
                                 String taskType,
                                 Long appId,
                                 String appName,
                                 Long versionId,
                                 String versionNo,
                                 Long originTaskId,
                                 String status,
                                 String batchStrategy,
                                 Integer batchSize,
                                 Integer targetCount,
                                 Integer successCount,
                                 Integer failCount,
                                 String operatorName,
                                 String approvalOperatorName,
                                 String approvalComment,
                                 LocalDateTime approvalAt,
                                 LocalDateTime startedAt,
                                 LocalDateTime finishedAt,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt,
                                 Map<String, String> params) {
  }

  public record DeployTaskHostRecord(Long id,
                                     Long taskId,
                                     Long hostId,
                                     String hostName,
                                     String ipAddress,
                                     String status,
                                     String currentStep,
                                     LocalDateTime startedAt,
                                     LocalDateTime finishedAt,
                                     String errorMsg) {
  }

  public record DeployTaskLogRecord(Long id,
                                    Long taskId,
                                    Long taskHostId,
                                    String logLevel,
                                    String logContent,
                                    LocalDateTime createdAt) {
  }

  public record TaskCenterRecord(Long id,
                                 String sourceType,
                                 String taskNo,
                                 String taskName,
                                 String taskType,
                                 String status,
                                 Long appId,
                                 String appName,
                                 Long versionId,
                                 String versionNo,
                                 Integer targetCount,
                                 Integer successCount,
                                 Integer failCount,
                                 String operatorName,
                                 String approvalOperatorName,
                                 String approvalComment,
                                 LocalDateTime approvalAt,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
  }

  private record ValidatedCreateCommand(String taskName,
                                        String taskType,
                                        AppDefinitionMapper.AppDefinitionRow app,
                                        AppVersionMapper.AppVersionRow version,
                                        List<Long> hostIds,
                                        String batchStrategy,
                                        Integer batchSize,
                                        Map<String, String> params) {
  }
}
