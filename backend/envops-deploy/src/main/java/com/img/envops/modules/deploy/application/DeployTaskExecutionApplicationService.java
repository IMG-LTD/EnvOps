package com.img.envops.modules.deploy.application;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.modules.app.infrastructure.LocalPackageStorage;
import com.img.envops.modules.app.infrastructure.mapper.AppDefinitionMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppPackageMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppScriptTemplateMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppVersionMapper;
import com.img.envops.modules.deploy.executor.ExecRequest;
import com.img.envops.modules.deploy.executor.FileUploadRequest;
import com.img.envops.modules.deploy.executor.RemoteExecutor;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskHostMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskLogMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskParamMapper;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class DeployTaskExecutionApplicationService {
  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_RUNNING = "RUNNING";
  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_FAILED = "FAILED";
  private static final String STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
  private static final String STATUS_CANCELLED = "CANCELLED";
  private static final String TASK_TYPE_ROLLBACK = "ROLLBACK";
  private static final DateTimeFormatter TASK_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final DeployTaskApplicationService deployTaskApplicationService;
  private final DeployTaskMapper deployTaskMapper;
  private final DeployTaskHostMapper deployTaskHostMapper;
  private final DeployTaskLogMapper deployTaskLogMapper;
  private final DeployTaskParamMapper deployTaskParamMapper;
  private final AppVersionMapper appVersionMapper;
  private final AppPackageMapper appPackageMapper;
  private final AppScriptTemplateMapper appScriptTemplateMapper;
  private final AppDefinitionMapper appDefinitionMapper;
  private final LocalPackageStorage localPackageStorage;
  private final RemoteExecutor remoteExecutor;
  private final TaskExecutor deployTaskExecutor;
  private final TransactionTemplate transactionTemplate;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory;

  public DeployTaskExecutionApplicationService(DeployTaskApplicationService deployTaskApplicationService,
                                               DeployTaskMapper deployTaskMapper,
                                               DeployTaskHostMapper deployTaskHostMapper,
                                               DeployTaskLogMapper deployTaskLogMapper,
                                               DeployTaskParamMapper deployTaskParamMapper,
                                               AppVersionMapper appVersionMapper,
                                               AppPackageMapper appPackageMapper,
                                               AppScriptTemplateMapper appScriptTemplateMapper,
                                               AppDefinitionMapper appDefinitionMapper,
                                               LocalPackageStorage localPackageStorage,
                                               RemoteExecutor remoteExecutor,
                                               @Qualifier("deployTaskExecutor") TaskExecutor deployTaskExecutor,
                                               PlatformTransactionManager transactionManager,
                                               UnifiedTaskRecorder unifiedTaskRecorder,
                                               UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory) {
    this.deployTaskApplicationService = deployTaskApplicationService;
    this.deployTaskMapper = deployTaskMapper;
    this.deployTaskHostMapper = deployTaskHostMapper;
    this.deployTaskLogMapper = deployTaskLogMapper;
    this.deployTaskParamMapper = deployTaskParamMapper;
    this.appVersionMapper = appVersionMapper;
    this.appPackageMapper = appPackageMapper;
    this.appScriptTemplateMapper = appScriptTemplateMapper;
    this.appDefinitionMapper = appDefinitionMapper;
    this.localPackageStorage = localPackageStorage;
    this.remoteExecutor = remoteExecutor;
    this.deployTaskExecutor = deployTaskExecutor;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.unifiedTaskRecorder = unifiedTaskRecorder;
    this.unifiedTaskDetailPreviewFactory = unifiedTaskDetailPreviewFactory;
  }

  public DeployTaskApplicationService.DeployTaskRecord executeDeployTask(Long taskId, String operatorName) {
    String resolvedOperatorName = requireOperatorName(operatorName);
    runInTransaction(() -> {
      markTaskRunning(taskId, resolvedOperatorName);
      syncUnifiedProjection(taskId, null, null, null, null);
    });
    deployTaskExecutor.execute(() -> runTask(taskId, resolvedOperatorName));
    return deployTaskApplicationService.getDeployTaskRecord(taskId);
  }

  public DeployTaskApplicationService.DeployTaskRecord cancelDeployTask(Long taskId, String operatorName) {
    String resolvedOperatorName = requireOperatorName(operatorName);
    boolean cancelledImmediately = runInTransaction(() -> cancelPendingTask(taskId, resolvedOperatorName));
    if (!cancelledImmediately) {
      runInTransaction(() -> requestTaskCancellation(taskId, resolvedOperatorName));
    }
    return deployTaskApplicationService.getDeployTaskRecord(taskId);
  }

  public DeployTaskApplicationService.DeployTaskRecord retryDeployTask(Long taskId, String operatorName) {
    String resolvedOperatorName = requireOperatorName(operatorName);
    runInTransaction(() -> resetTaskForRetry(taskId, resolvedOperatorName));
    return executeDeployTask(taskId, resolvedOperatorName);
  }

  public DeployTaskApplicationService.DeployTaskRecord rollbackDeployTask(Long taskId, String operatorName) {
    String resolvedOperatorName = requireOperatorName(operatorName);
    Long rollbackTaskId = runInTransaction(() -> createRollbackTask(taskId, resolvedOperatorName));
    return executeDeployTask(rollbackTaskId, resolvedOperatorName);
  }

  protected void runTask(Long taskId, String operatorName) {
    int successCount = 0;
    int failCount = 0;
    boolean cancelRequested = false;

    try {
      DeployTaskMapper.DeployTaskRow task = requireTask(taskId);
      TaskExecutionContext context = buildContext(taskId, task);
      List<DeployTaskHostMapper.DeployTaskHostRow> hosts = deployTaskHostMapper.findByTaskId(taskId);

      for (DeployTaskHostMapper.DeployTaskHostRow host : hosts) {
        if (STATUS_SUCCESS.equals(host.getStatus())) {
          successCount++;
          continue;
        }
        if (isCancelRequested(taskId)) {
          markHostCancelled(host, "Task cancelled before host execution");
          cancelRequested = true;
          continue;
        }

        try {
          runHost(taskId, host, context, operatorName);
          successCount++;
        } catch (RuntimeException exception) {
          failCount++;
          markHostFailed(host, exception.getMessage());
        }
      }

      if (cancelRequested || isCancelRequested(taskId)) {
        markQueuedHostsCancelled(taskId, "Task cancelled");
        finalizeTask(taskId, STATUS_CANCELLED, successCount, failCount, operatorName, "Task cancelled");
        return;
      }

      String taskStatus = failCount > 0 ? STATUS_FAILED : STATUS_SUCCESS;
      finalizeTask(taskId, taskStatus, successCount, failCount, operatorName, "Task finished with status " + taskStatus);
    } catch (RuntimeException exception) {
      finalizeTask(taskId, STATUS_FAILED, successCount, failCount, operatorName, exception.getMessage());
      throw exception;
    }
  }

  private TaskExecutionContext buildContext(Long taskId, DeployTaskMapper.DeployTaskRow task) {
    Map<String, String> params = loadParams(taskId);
    String sshUser = requireParam(params, "sshUser");
    Integer sshPort = parseSshPort(params.get("sshPort"));
    String privateKeyPath = requireParam(params, "privateKeyPath");
    String remoteBaseDir = normalizeRemoteBaseDir(requireParam(params, "remoteBaseDir"));
    String rollbackCommand = normalizeOptionalParam(params.get("rollbackCommand"));

    if (TASK_TYPE_ROLLBACK.equals(task.getTaskType()) && StringUtils.hasText(rollbackCommand)) {
      return new TaskExecutionContext(task.getTaskType(), sshUser, sshPort, privateKeyPath, null, null, null, null, rollbackCommand);
    }

    AppVersionMapper.AppVersionRow version = appVersionMapper.findActiveById(task.getVersionId());
    if (version == null) {
      throw new IllegalArgumentException("version not found: " + task.getVersionId());
    }
    if (version.getPackageId() == null) {
      throw new IllegalArgumentException("version package is required: " + task.getVersionId());
    }
    if (version.getScriptTemplateId() == null) {
      throw new IllegalArgumentException("version script template is required: " + task.getVersionId());
    }

    AppPackageMapper.AppPackageRow appPackage = appPackageMapper.findActiveById(version.getPackageId());
    if (appPackage == null) {
      throw new IllegalArgumentException("package not found: " + version.getPackageId());
    }
    AppScriptTemplateMapper.AppScriptTemplateRow scriptTemplate = appScriptTemplateMapper.findActiveById(version.getScriptTemplateId());
    if (scriptTemplate == null) {
      throw new IllegalArgumentException("script template not found: " + version.getScriptTemplateId());
    }
    AppDefinitionMapper.AppDefinitionRow app = appDefinitionMapper.findActiveById(task.getAppId());
    if (app == null) {
      throw new IllegalArgumentException("app not found: " + task.getAppId());
    }

    Path packagePath = localPackageStorage.resolve(appPackage.getFilePath());
    String packageFileName = packagePath.getFileName().toString();
    String remoteReleaseDir = remoteBaseDir + "/" + app.getAppCode() + "/" + version.getVersionNo();
    String remotePackagePath = remoteReleaseDir + "/" + packageFileName;
    String renderedScript = renderScript(scriptTemplate.getScriptContent(), remotePackagePath);

    return new TaskExecutionContext(task.getTaskType(), sshUser, sshPort, privateKeyPath, packagePath, remoteReleaseDir, remotePackagePath, renderedScript, rollbackCommand);
  }

  private void runHost(Long taskId,
                       DeployTaskHostMapper.DeployTaskHostRow host,
                       TaskExecutionContext context,
                       String operatorName) {
    LocalDateTime startedAt = LocalDateTime.now();
    String command;
    if (TASK_TYPE_ROLLBACK.equals(context.taskType()) && StringUtils.hasText(context.rollbackCommand())) {
      updateHostState(host.getId(), STATUS_RUNNING, "ROLLBACK_COMMAND", startedAt, null, null);
      command = context.rollbackCommand();
      logHost(taskId, host.getId(), "INFO", "Executing rollback command", LocalDateTime.now());
    } else {
      updateHostState(host.getId(), STATUS_RUNNING, "PREPARE_REMOTE_DIR", startedAt, null, null);
      logHost(taskId, host.getId(), "INFO", "Preparing remote release directory " + context.remoteReleaseDir(), startedAt);
      remoteExecutor.exec(new ExecRequest(
          host.getHostId(),
          host.getHostName(),
          host.getIpAddress(),
          context.sshUser(),
          context.sshPort(),
          context.privateKeyPath(),
          "mkdir -p " + shellQuote(context.remoteReleaseDir())));

      updateHostState(host.getId(), STATUS_RUNNING, "UPLOAD_PACKAGE", startedAt, null, null);
      logHost(taskId, host.getId(), "INFO", "Uploading package to " + context.remotePackagePath(), LocalDateTime.now());
      remoteExecutor.upload(new FileUploadRequest(
          host.getHostId(),
          host.getHostName(),
          host.getIpAddress(),
          context.sshUser(),
          context.sshPort(),
          context.privateKeyPath(),
          context.packagePath().toString(),
          context.remotePackagePath()));

      Path renderedScript = writeRenderedScript(taskId, host.getId(), context.renderedScript());
      String remoteScriptPath = context.remoteReleaseDir() + "/deploy-task-" + taskId + "-host-" + host.getHostId() + ".sh";
      updateHostState(host.getId(), STATUS_RUNNING, "UPLOAD_SCRIPT", startedAt, null, null);
      logHost(taskId, host.getId(), "INFO", "Uploading script to " + remoteScriptPath, LocalDateTime.now());
      remoteExecutor.upload(new FileUploadRequest(
          host.getHostId(),
          host.getHostName(),
          host.getIpAddress(),
          context.sshUser(),
          context.sshPort(),
          context.privateKeyPath(),
          renderedScript.toString(),
          remoteScriptPath));

      updateHostState(host.getId(), STATUS_RUNNING, "EXEC_SCRIPT", startedAt, null, null);
      command = "ENVOPS_TASK_ACTION=" + context.taskType() + " ENVOPS_PACKAGE_PATH=" + shellQuote(context.remotePackagePath()) + " bash " + shellQuote(remoteScriptPath);
      logHost(taskId, host.getId(), "INFO", "Executing deploy script with action " + context.taskType(), LocalDateTime.now());
    }

    remoteExecutor.exec(new ExecRequest(
        host.getHostId(),
        host.getHostName(),
        host.getIpAddress(),
        context.sshUser(),
        context.sshPort(),
        context.privateKeyPath(),
        command));

    updateHostState(host.getId(), STATUS_SUCCESS, "COMPLETED", startedAt, LocalDateTime.now(), null);
    logHost(taskId, host.getId(), "INFO", "Host execution completed", LocalDateTime.now());
  }

  private void markHostFailed(DeployTaskHostMapper.DeployTaskHostRow host, String errorMessage) {
    DeployTaskHostMapper.DeployTaskHostRow current = requireHost(host.getId());
    LocalDateTime finishedAt = LocalDateTime.now();
    updateHostState(current.getId(), STATUS_FAILED, "FAILED", current.getStartedAt(), finishedAt, errorMessage);
    logHost(current.getTaskId(), current.getId(), "ERROR", errorMessage, finishedAt);
  }

  private void markHostCancelled(DeployTaskHostMapper.DeployTaskHostRow host, String message) {
    DeployTaskHostMapper.DeployTaskHostRow current = requireHost(host.getId());
    LocalDateTime finishedAt = LocalDateTime.now();
    updateHostState(current.getId(), STATUS_CANCELLED, "CANCELLED", current.getStartedAt(), finishedAt, message);
    logHost(current.getTaskId(), current.getId(), "WARN", message, finishedAt);
  }

  private void markQueuedHostsCancelled(Long taskId, String message) {
    for (DeployTaskHostMapper.DeployTaskHostRow host : deployTaskHostMapper.findByTaskId(taskId)) {
      if (STATUS_PENDING.equals(host.getStatus()) || STATUS_RUNNING.equals(host.getStatus()) || STATUS_CANCEL_REQUESTED.equals(host.getStatus())) {
        markHostCancelled(host, message);
      }
    }
  }

  @Transactional
  protected void markTaskRunning(Long taskId, String operatorName) {
    DeployTaskMapper.DeployTaskRow task = requireTask(taskId);
    if (!STATUS_PENDING.equals(task.getStatus())) {
      throw new ConflictException("deploy task cannot be executed: " + taskId);
    }

    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setStartedAt(now);
    entity.setUpdatedBy(operatorName);
    entity.setUpdatedAt(now);
    int updated = deployTaskMapper.markRunning(entity);
    if (updated == 0) {
      throw new ConflictException("deploy task cannot be executed: " + taskId);
    }
    deployTaskLogMapper.insertLog(log(taskId, null, "INFO", "Task execution started", now));
  }

  @Transactional
  protected void requestTaskCancellation(Long taskId, String operatorName) {
    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setUpdatedBy(operatorName);
    entity.setUpdatedAt(now);
    int updated = deployTaskMapper.markCancelRequested(entity);
    if (updated == 0) {
      throw new ConflictException("deploy task cannot be cancelled: " + taskId);
    }
    deployTaskLogMapper.insertLog(log(taskId, null, "WARN", "Task cancel requested", now));
    syncUnifiedProjection(taskId, "running", null, "发布任务等待取消", null);
  }

  @Transactional
  protected boolean cancelPendingTask(Long taskId, String operatorName) {
    requireTask(taskId);
    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setSuccessCount(0);
    entity.setFailCount(0);
    entity.setFinishedAt(now);
    entity.setUpdatedBy(operatorName);
    entity.setUpdatedAt(now);
    int updated = deployTaskMapper.markCancelledFromPending(entity);
    if (updated == 0) {
      return false;
    }
    markQueuedHostsCancelled(taskId, "Task cancelled before execution started");
    deployTaskLogMapper.insertLog(log(taskId, null, "WARN", "Task cancelled", now));
    syncUnifiedProjection(taskId, "failed", now, "发布任务已取消", "Task cancelled");
    return true;
  }

  @Transactional
  protected void resetTaskForRetry(Long taskId, String operatorName) {
    DeployTaskMapper.DeployTaskRow task = requireTask(taskId);
    if (!STATUS_FAILED.equals(task.getStatus()) && !STATUS_CANCELLED.equals(task.getStatus())) {
      throw new ConflictException("deploy task cannot be retried: " + taskId);
    }

    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setUpdatedBy(operatorName);
    entity.setUpdatedAt(now);
    int updated = deployTaskMapper.resetForRetry(entity);
    if (updated == 0) {
      throw new ConflictException("deploy task cannot be retried: " + taskId);
    }
    deployTaskHostMapper.resetForRetry(taskId);
    deployTaskLogMapper.insertLog(log(taskId, null, "INFO", "Task retried", now));
  }

  @Transactional
  protected Long createRollbackTask(Long taskId, String operatorName) {
    DeployTaskMapper.DeployTaskRow original = requireTask(taskId);
    if (TASK_TYPE_ROLLBACK.equals(original.getTaskType())) {
      throw new ConflictException("rollback task cannot be rolled back: " + taskId);
    }
    if (!STATUS_SUCCESS.equals(original.getStatus())) {
      throw new ConflictException("deploy task cannot be rolled back: " + taskId);
    }

    for (int attempt = 0; attempt < 5; attempt++) {
      try {
        return createRollbackTaskOnce(original, operatorName);
      } catch (org.springframework.dao.DuplicateKeyException exception) {
        if (attempt == 4) {
          throw exception;
        }
      }
    }
    throw new IllegalStateException("failed to create rollback task");
  }

  private Long createRollbackTaskOnce(DeployTaskMapper.DeployTaskRow original, String operatorName) {
    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setTaskNo(generateTaskNo(now));
    entity.setTaskName(original.getTaskName() + "-rollback");
    entity.setTaskType(TASK_TYPE_ROLLBACK);
    entity.setAppId(original.getAppId());
    entity.setVersionId(original.getVersionId());
    entity.setOriginTaskId(original.getId());
    entity.setStatus(STATUS_PENDING);
    entity.setBatchStrategy(original.getBatchStrategy());
    entity.setBatchSize(original.getBatchSize());
    entity.setTargetCount(original.getTargetCount());
    entity.setSuccessCount(0);
    entity.setFailCount(0);
    entity.setOperatorName(operatorName);
    entity.setApprovalOperatorName(operatorName);
    entity.setApprovalComment("Rollback task created from " + original.getId());
    entity.setApprovalAt(now);
    entity.setDeleted(0);
    entity.setCreatedBy(operatorName);
    entity.setUpdatedBy(operatorName);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    deployTaskMapper.insertTask(entity);

    for (DeployTaskHostMapper.DeployTaskHostRow host : deployTaskHostMapper.findByTaskId(original.getId())) {
      DeployTaskHostMapper.DeployTaskHostEntity hostEntity = new DeployTaskHostMapper.DeployTaskHostEntity();
      hostEntity.setTaskId(entity.getId());
      hostEntity.setHostId(host.getHostId());
      hostEntity.setStatus(STATUS_PENDING);
      deployTaskHostMapper.insertTaskHost(hostEntity);
    }

    for (DeployTaskParamMapper.DeployTaskParamRow param : deployTaskParamMapper.findByTaskId(original.getId())) {
      DeployTaskParamMapper.DeployTaskParamEntity paramEntity = new DeployTaskParamMapper.DeployTaskParamEntity();
      paramEntity.setTaskId(entity.getId());
      paramEntity.setParamKey(param.getParamKey());
      paramEntity.setParamValue(param.getParamValue());
      paramEntity.setSecretFlag(param.getSecretFlag());
      deployTaskParamMapper.insertParam(paramEntity);
    }

    deployTaskLogMapper.insertLog(log(entity.getId(), null, "INFO", "Rollback task created from " + original.getId(), now));
    syncRollbackUnifiedProjection(entity, original, now, operatorName);
    return entity.getId();
  }

  @Transactional
  protected LocalDateTime finishTask(Long taskId,
                                     String status,
                                     int successCount,
                                     int failCount,
                                     String operatorName,
                                     String logMessage) {
    LocalDateTime now = LocalDateTime.now();
    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setStatus(status);
    entity.setSuccessCount(successCount);
    entity.setFailCount(failCount);
    entity.setFinishedAt(now);
    entity.setUpdatedBy(operatorName);
    entity.setUpdatedAt(now);

    int updated = STATUS_CANCELLED.equals(status)
        ? deployTaskMapper.markCancelledFromCancelRequested(entity)
        : deployTaskMapper.updateExecutionSummary(entity);

    if (updated == 0 && !STATUS_CANCELLED.equals(status) && isCancelRequested(taskId)) {
      updated = deployTaskMapper.markCancelledFromCancelRequested(entity);
      if (updated > 0) {
        deployTaskLogMapper.insertLog(log(taskId, null, "WARN", "Task cancelled", now));
        return now;
      }
    }

    if (updated > 0) {
      deployTaskLogMapper.insertLog(log(taskId, null, STATUS_CANCELLED.equals(status) ? "WARN" : "INFO", logMessage, now));
      return now;
    }
    return now;
  }

  private void updateHostState(Long hostRowId,
                               String status,
                               String step,
                               LocalDateTime startedAt,
                               LocalDateTime finishedAt,
                               String errorMessage) {
    DeployTaskHostMapper.DeployTaskHostEntity entity = new DeployTaskHostMapper.DeployTaskHostEntity();
    entity.setId(hostRowId);
    entity.setStatus(status);
    entity.setCurrentStep(step);
    entity.setStartedAt(startedAt);
    entity.setFinishedAt(finishedAt);
    entity.setErrorMsg(errorMessage);
    deployTaskHostMapper.updateHostExecutionState(entity);
  }

  private void logHost(Long taskId, Long hostRowId, String level, String message, LocalDateTime createdAt) {
    deployTaskLogMapper.insertLog(log(taskId, hostRowId, level, message, createdAt));
  }

  private DeployTaskLogMapper.DeployTaskLogEntity log(Long taskId,
                                                      Long taskHostId,
                                                      String level,
                                                      String message,
                                                      LocalDateTime createdAt) {
    DeployTaskLogMapper.DeployTaskLogEntity entity = new DeployTaskLogMapper.DeployTaskLogEntity();
    entity.setTaskId(taskId);
    entity.setTaskHostId(taskHostId);
    entity.setLogLevel(level);
    entity.setLogContent(message);
    entity.setCreatedAt(createdAt);
    return entity;
  }

  private void finalizeTask(Long taskId,
                            String status,
                            int successCount,
                            int failCount,
                            String operatorName,
                            String logMessage) {
    runInTransaction(() -> {
      LocalDateTime finishedAt = finishTask(taskId, status, successCount, failCount, operatorName, logMessage);
      String finalStatus = requireTask(taskId).getStatus();
      syncUnifiedProjection(
          taskId,
          null,
          finishedAt,
          null,
          STATUS_FAILED.equals(finalStatus) ? "Deploy host execution failed" : null);
    });
  }

  private void syncUnifiedProjection(Long taskId,
                                     String unifiedStatus,
                                     LocalDateTime finishedAt,
                                     String summaryOverride,
                                     String errorSummary) {
    DeployTaskApplicationService.DeployTaskRecord record = deployTaskApplicationService.getDeployTaskRecord(taskId);
    String environment = deployTaskApplicationService.resolveEnvironment(record.getParams());
    String sourceRoute = deployTaskApplicationService.buildSourceRoute(taskId);
    String resolvedUnifiedStatus = unifiedStatus != null
        ? unifiedStatus
        : UnifiedTaskCenterApplicationService.normalizeStatus(record.getStatus());
    String summary = summaryOverride != null
        ? summaryOverride
        : deployTaskApplicationService.buildDeploySummary(
            record.getAppName(),
            environment,
            record.getTargetCount(),
            record.getSuccessCount(),
            record.getFailCount());
    unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
        "deploy",
        taskId,
        resolvedUnifiedStatus,
        finishedAt,
        summary,
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
            record.getAppName(),
            environment,
            record.getTargetCount(),
            record.getSuccessCount(),
            record.getFailCount(),
            record.getStatus(),
            sourceRoute)),
        errorSummary));
  }

  private void syncRollbackUnifiedProjection(DeployTaskMapper.DeployTaskEntity rollbackTask,
                                             DeployTaskMapper.DeployTaskRow originalTask,
                                             LocalDateTime now,
                                             String operatorName) {
    String environment = deployTaskApplicationService.resolveEnvironment(loadParams(originalTask.getId()));
    String sourceRoute = deployTaskApplicationService.buildSourceRoute(rollbackTask.getId());
    unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
        "deploy",
        rollbackTask.getId(),
        rollbackTask.getTaskName(),
        "pending",
        operatorName,
        now,
        null,
        "回滚任务已创建，等待执行",
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
            originalTask.getAppName(),
            environment,
            rollbackTask.getTargetCount(),
            rollbackTask.getSuccessCount(),
            rollbackTask.getFailCount(),
            rollbackTask.getStatus(),
            sourceRoute)),
        sourceRoute,
        "deploy",
        null));
  }

  private DeployTaskHostMapper.DeployTaskHostRow requireHost(Long hostRowId) {
    DeployTaskHostMapper.DeployTaskHostRow host = deployTaskHostMapper.findById(hostRowId);
    if (host == null) {
      throw new NotFoundException("deploy task host not found: " + hostRowId);
    }
    return host;
  }

  private boolean isCancelRequested(Long taskId) {
    DeployTaskMapper.DeployTaskRow current = requireTask(taskId);
    return STATUS_CANCEL_REQUESTED.equals(current.getStatus());
  }

  private <T> T runInTransaction(Supplier<T> supplier) {
    return transactionTemplate.execute(status -> supplier.get());
  }

  private void runInTransaction(Runnable action) {
    transactionTemplate.executeWithoutResult(status -> action.run());
  }

  private DeployTaskMapper.DeployTaskRow requireTask(Long taskId) {
    DeployTaskMapper.DeployTaskRow task = deployTaskMapper.findActiveById(taskId);
    if (task == null) {
      throw new NotFoundException("deploy task not found: " + taskId);
    }
    return task;
  }

  private Map<String, String> loadParams(Long taskId) {
    Map<String, String> params = new LinkedHashMap<>();
    for (DeployTaskParamMapper.DeployTaskParamRow row : deployTaskParamMapper.findByTaskId(taskId)) {
      params.put(row.getParamKey(), row.getParamValue());
    }
    return params;
  }

  private String renderScript(String template, String remotePackagePath) {
    String quotedRemotePackagePath = shellQuote(remotePackagePath);
    return template
        .replace("{{package}}", quotedRemotePackagePath)
        .replace("{{packagePath}}", remotePackagePath);
  }

  private Path writeRenderedScript(Long taskId, Long hostRowId, String content) {
    try {
      Path tempDir = Files.createTempDirectory("envops-deploy-task-");
      Path scriptPath = tempDir.resolve("deploy-task-" + taskId + "-host-" + hostRowId + ".sh");
      Files.writeString(scriptPath, content);
      return scriptPath;
    } catch (Exception exception) {
      throw new IllegalStateException("failed to render script", exception);
    }
  }

  private String requireOperatorName(String operatorName) {
    if (!StringUtils.hasText(operatorName)) {
      throw new IllegalArgumentException("operatorName is required");
    }
    return operatorName.trim();
  }

  private String requireParam(Map<String, String> params, String key) {
    String value = params.get(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(key + " is required");
    }
    return value.trim();
  }

  private Integer parseSshPort(String sshPort) {
    if (!StringUtils.hasText(sshPort)) {
      return 22;
    }
    return Integer.parseInt(sshPort.trim());
  }

  private String normalizeRemoteBaseDir(String remoteBaseDir) {
    String normalized = remoteBaseDir.trim();
    if (normalized.endsWith("/")) {
      return normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String normalizeOptionalParam(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String shellQuote(String value) {
    if (value == null) {
      throw new IllegalArgumentException("shell value is required");
    }
    return "'" + value.replace("'", "'\"'\"'") + "'";
  }

  private String generateTaskNo(LocalDateTime now) {
    return "DT" + TASK_NO_TIME_FORMATTER.format(now) + String.format("%04d", now.getNano() / 100000);
  }

  private record TaskExecutionContext(String taskType,
                                      String sshUser,
                                      Integer sshPort,
                                      String privateKeyPath,
                                      Path packagePath,
                                      String remoteReleaseDir,
                                      String remotePackagePath,
                                      String renderedScript,
                                      String rollbackCommand) {
  }
}
