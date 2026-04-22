package com.img.envops.modules.deploy.application;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.modules.app.infrastructure.mapper.AppDefinitionMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppVersionMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetHostMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskHostMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskLogMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskMapper;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskParamMapper;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DeployTaskApplicationService {
  private static final List<String> SUPPORTED_TASK_TYPES = List.of("INSTALL", "UPGRADE", "ROLLBACK");
  private static final List<String> SUPPORTED_BATCH_STRATEGIES = List.of("ALL", "ROLLING");
  private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_RUNNING = "RUNNING";
  private static final String STATUS_FAILED = "FAILED";
  private static final String STATUS_REJECTED = "REJECTED";
  private static final String STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
  private static final List<String> SUPPORTED_STATUSES = List.of("INIT", STATUS_PENDING_APPROVAL, STATUS_PENDING, STATUS_RUNNING, "SUCCESS", STATUS_FAILED, STATUS_REJECTED, STATUS_CANCEL_REQUESTED, "CANCELLED");
  private static final List<String> SUPPORTED_SORT_FIELDS = List.of("createdAt", "updatedAt", "taskNo", "status");
  private static final List<String> SUPPORTED_SORT_ORDERS = List.of("asc", "desc");
  private static final List<String> SUPPORTED_SOURCE_TYPES = List.of("DEPLOY");
  private static final List<String> SUPPORTED_PRIORITIES = List.of("P1", "P2", "P3");
  private static final Set<String> SUPPORTED_CREATE_PARAM_KEYS = Set.of(
      "environment",
      "sshUser",
      "sshPort",
      "privateKeyPath",
      "remoteBaseDir",
      "rollbackCommand");
  private static final List<String> SECRET_PARAM_KEYS = List.of("token", "secret", "password", "credential", "privatekey", "accesskey", "secretkey", "apikey", "sshkey", "rollbackcommand");
  private static final DateTimeFormatter TASK_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final DeployTaskMapper deployTaskMapper;
  private final DeployTaskHostMapper deployTaskHostMapper;
  private final DeployTaskLogMapper deployTaskLogMapper;
  private final DeployTaskParamMapper deployTaskParamMapper;
  private final AppDefinitionMapper appDefinitionMapper;
  private final AppVersionMapper appVersionMapper;
  private final AssetHostMapper assetHostMapper;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory;

  public DeployTaskApplicationService(DeployTaskMapper deployTaskMapper,
                                      DeployTaskHostMapper deployTaskHostMapper,
                                      DeployTaskLogMapper deployTaskLogMapper,
                                      DeployTaskParamMapper deployTaskParamMapper,
                                      AppDefinitionMapper appDefinitionMapper,
                                      AppVersionMapper appVersionMapper,
                                      AssetHostMapper assetHostMapper,
                                      UnifiedTaskRecorder unifiedTaskRecorder,
                                      UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory) {
    this.deployTaskMapper = deployTaskMapper;
    this.deployTaskHostMapper = deployTaskHostMapper;
    this.deployTaskLogMapper = deployTaskLogMapper;
    this.deployTaskParamMapper = deployTaskParamMapper;
    this.appDefinitionMapper = appDefinitionMapper;
    this.appVersionMapper = appVersionMapper;
    this.assetHostMapper = assetHostMapper;
    this.unifiedTaskRecorder = unifiedTaskRecorder;
    this.unifiedTaskDetailPreviewFactory = unifiedTaskDetailPreviewFactory;
  }

  public DeployTaskPage getDeployTasks(DeployTaskQuery query) {
    NormalizedDeployTaskQuery normalized = normalizeDeployTaskQuery(query);
    long total = deployTaskMapper.countByQuery(
        normalized.keyword(),
        normalized.status(),
        normalized.taskType(),
        normalized.appId(),
        normalized.environment(),
        normalized.createdFrom(),
        normalized.createdTo());
    List<DeployTaskRecord> records = deployTaskMapper.findByQuery(
            normalized.keyword(),
            normalized.status(),
            normalized.taskType(),
            normalized.appId(),
            normalized.environment(),
            normalized.createdFrom(),
            normalized.createdTo(),
            normalized.sortBy(),
            normalized.sortOrder(),
            normalized.pageSize(),
            normalized.offset())
        .stream()
        .map(row -> toDeployTaskRecord(row, loadParams(row.getId())))
        .toList();
    return new DeployTaskPage(normalized.page(), normalized.pageSize(), total, records);
  }

  public DeployTaskDetailRecord getDeployTask(Long id) {
    DeployTaskMapper.DeployTaskRow row = requireTask(id);
    return toDeployTaskDetailRecord(row, loadParams(id), loadHostSummary(id));
  }

  public DeployTaskRecord getDeployTaskRecord(Long id) {
    DeployTaskMapper.DeployTaskRow row = requireTask(id);
    return toDeployTaskRecord(row, loadParams(id));
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

    syncUnifiedProjectionOnCreate(entity, validated, resolvedOperatorName, now);
    return toDeployTaskRecord(requireTask(entity.getId()), loadParams(entity.getId()));
  }

  @Transactional
  public DeployTaskRecord approveDeployTask(Long taskId, String comment, String operatorName) {
    return decideApproval(taskId, STATUS_PENDING, comment, operatorName, "Task approved");
  }

  @Transactional
  public DeployTaskRecord rejectDeployTask(Long taskId, String comment, String operatorName) {
    return decideApproval(taskId, STATUS_REJECTED, comment, operatorName, "Task rejected");
  }

  public DeployTaskHostPage getDeployTaskHosts(Long taskId, DeployTaskHostQuery query) {
    requireTask(taskId);
    NormalizedDeployTaskHostQuery normalized = normalizeDeployTaskHostQuery(query);
    long total = deployTaskHostMapper.countByTaskIdAndQuery(taskId, normalized.status(), normalized.keyword());
    List<DeployTaskHostRecord> records = deployTaskHostMapper.findByTaskIdAndQuery(
            taskId,
            normalized.status(),
            normalized.keyword(),
            normalized.pageSize(),
            normalized.offset())
        .stream()
        .map(this::toDeployTaskHostRecord)
        .toList();
    return new DeployTaskHostPage(normalized.page(), normalized.pageSize(), total, records);
  }

  public DeployTaskLogPage getDeployTaskLogs(Long taskId, DeployTaskLogQuery query) {
    requireTask(taskId);
    NormalizedDeployTaskLogQuery normalized = normalizeDeployTaskLogQuery(query);
    long total = deployTaskLogMapper.countByTaskIdAndQuery(taskId, normalized.hostId(), normalized.keyword());
    List<DeployTaskLogRecord> records = deployTaskLogMapper.findByTaskIdAndQuery(
            taskId,
            normalized.hostId(),
            normalized.keyword(),
            normalized.pageSize(),
            normalized.offset())
        .stream()
        .map(this::toDeployTaskLogRecord)
        .toList();
    return new DeployTaskLogPage(normalized.page(), normalized.pageSize(), total, records);
  }

  public TaskCenterPage getTaskCenterTasks(TaskCenterQuery query) {
    NormalizedTaskCenterQuery normalized = normalizeTaskCenterQuery(query);
    long total = deployTaskMapper.countTaskCenterByQuery(
        normalized.keyword(),
        normalized.status(),
        normalized.sourceType(),
        normalized.taskType(),
        normalized.priority());
    List<TaskCenterRecord> records = deployTaskMapper.findTaskCenterByQuery(
            normalized.keyword(),
            normalized.status(),
            normalized.sourceType(),
            normalized.taskType(),
            normalized.priority(),
            normalized.sortBy(),
            normalized.sortOrder(),
            normalized.pageSize(),
            normalized.offset())
        .stream()
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
            resolvePriority(row),
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
    return new TaskCenterPage(normalized.page(), normalized.pageSize(), total, records);
  }

  private NormalizedDeployTaskQuery normalizeDeployTaskQuery(DeployTaskQuery query) {
    int page = query == null ? 1 : normalizePage(query.page());
    int pageSize = query == null ? 10 : normalizePageSize(query.pageSize());
    return new NormalizedDeployTaskQuery(
        query == null ? null : trimToNull(query.keyword()),
        query == null ? null : normalizeOptionalUppercase(query.status()),
        query == null ? null : normalizeOptionalUppercase(query.taskType()),
        query == null ? null : query.appId(),
        normalizeDeployEnvironment(query == null ? null : query.environment()),
        query == null ? null : query.createdFrom(),
        query == null ? null : query.createdTo(),
        page,
        pageSize,
        normalizeSortBy(query == null ? null : query.sortBy()),
        normalizeSortOrder(query == null ? null : query.sortOrder()),
        (page - 1) * pageSize);
  }

  private NormalizedTaskCenterQuery normalizeTaskCenterQuery(TaskCenterQuery query) {
    int page = query == null ? 1 : normalizePage(query.page());
    int pageSize = query == null ? 10 : normalizePageSize(query.pageSize());
    return new NormalizedTaskCenterQuery(
        query == null ? null : trimToNull(query.keyword()),
        query == null ? null : normalizeOptionalUppercase(query.status()),
        normalizeSourceType(query == null ? null : query.sourceType()),
        query == null ? null : normalizeOptionalUppercase(query.taskType()),
        normalizePriority(query == null ? null : query.priority()),
        page,
        pageSize,
        normalizeSortBy(query == null ? null : query.sortBy()),
        normalizeSortOrder(query == null ? null : query.sortOrder()),
        (page - 1) * pageSize);
  }

  private NormalizedDeployTaskHostQuery normalizeDeployTaskHostQuery(DeployTaskHostQuery query) {
    int page = query == null ? 1 : normalizePage(query.page());
    int pageSize = query == null ? 10 : normalizePageSize(query.pageSize());
    return new NormalizedDeployTaskHostQuery(
        query == null ? null : normalizeOptionalUppercase(query.status()),
        query == null ? null : trimToNull(query.keyword()),
        page,
        pageSize,
        (page - 1) * pageSize);
  }

  private NormalizedDeployTaskLogQuery normalizeDeployTaskLogQuery(DeployTaskLogQuery query) {
    int page = query == null ? 1 : normalizePage(query.page());
    int pageSize = query == null ? 10 : normalizePageSize(query.pageSize());
    return new NormalizedDeployTaskLogQuery(
        query == null ? null : query.hostId(),
        query == null ? null : trimToNull(query.keyword()),
        page,
        pageSize,
        (page - 1) * pageSize);
  }

  private int normalizePage(Integer page) {
    if (page == null) {
      return 1;
    }
    if (page < 1) {
      throw new IllegalArgumentException("page must be greater than 0");
    }
    return page;
  }

  private int normalizePageSize(Integer pageSize) {
    if (pageSize == null) {
      return 10;
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize must be greater than 0");
    }
    return pageSize;
  }

  private String normalizeSortBy(String sortBy) {
    String normalized = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
    if (!SUPPORTED_SORT_FIELDS.contains(normalized)) {
      throw new IllegalArgumentException("sortBy must be one of createdAt, updatedAt, taskNo, status");
    }
    return normalized;
  }

  private String normalizeSortOrder(String sortOrder) {
    String normalized = StringUtils.hasText(sortOrder) ? sortOrder.trim().toLowerCase(Locale.ROOT) : "desc";
    if (!SUPPORTED_SORT_ORDERS.contains(normalized)) {
      throw new IllegalArgumentException("sortOrder must be asc or desc");
    }
    return normalized;
  }

  private String normalizeSourceType(String sourceType) {
    String normalized = normalizeOptionalUppercase(sourceType);
    if (normalized == null) {
      return null;
    }
    if (!SUPPORTED_SOURCE_TYPES.contains(normalized)) {
      throw new IllegalArgumentException("sourceType must be one of DEPLOY");
    }
    return normalized;
  }

  private String normalizePriority(String priority) {
    String normalized = normalizeOptionalUppercase(priority);
    if (normalized == null) {
      return null;
    }
    if (!SUPPORTED_PRIORITIES.contains(normalized)) {
      throw new IllegalArgumentException("priority must be one of P1, P2, P3");
    }
    return normalized;
  }

  private String normalizeDeployEnvironment(String environment) {
    String normalized = trimToNull(environment);
    if (normalized == null) {
      return null;
    }

    return switch (normalized.toLowerCase(Locale.ROOT)) {
      case "prod", "production" -> "production";
      case "stage", "staging", "pre", "preprod", "uat" -> "staging";
      case "sandbox", "test", "testing", "dev", "development" -> "sandbox";
      default -> null;
    };
  }

  private String normalizeOptionalUppercase(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String resolvePriority(DeployTaskMapper.DeployTaskRow row) {
    int failCount = row.getFailCount() == null ? 0 : row.getFailCount();
    int targetCount = row.getTargetCount() == null ? 0 : row.getTargetCount();
    if (STATUS_FAILED.equals(row.getStatus()) || failCount > 0) {
      return "P1";
    }
    if (STATUS_RUNNING.equals(row.getStatus()) || STATUS_CANCEL_REQUESTED.equals(row.getStatus()) || targetCount >= 10) {
      return "P2";
    }
    return "P3";
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
        String key = entry.getKey().trim();
        if (!SUPPORTED_CREATE_PARAM_KEYS.contains(key)) {
          continue;
        }
        String value = String.valueOf(entry.getValue());
        if (!StringUtils.hasText(value)) {
          continue;
        }
        normalizedParams.put(key, value.trim());
      }
    }
    normalizedParams.put("sshUser", requireNonBlankParam(normalizedParams, "sshUser"));
    normalizedParams.put("privateKeyPath", requireNonBlankParam(normalizedParams, "privateKeyPath"));
    normalizedParams.put("remoteBaseDir", requireNonBlankParam(normalizedParams, "remoteBaseDir"));

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

  private String requireNonBlankParam(Map<String, String> params, String key) {
    String value = params.get(key);
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(key + " is required");
    }
    return value.trim();
  }

  private void syncUnifiedProjectionOnCreate(
      DeployTaskMapper.DeployTaskEntity entity,
      ValidatedCreateCommand validated,
      String operatorName,
      LocalDateTime now) {
    String environment = resolveEnvironment(validated.params());
    String sourceRoute = buildSourceRoute(entity.getId());
    unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
        "deploy",
        entity.getId(),
        entity.getTaskName(),
        "pending",
        operatorName,
        now,
        null,
        buildDeploySummary(validated.app().getAppName(), environment, entity.getTargetCount(), entity.getSuccessCount(), entity.getFailCount()),
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
            validated.app().getAppName(),
            environment,
            defaultZero(entity.getTargetCount()),
            defaultZero(entity.getSuccessCount()),
            defaultZero(entity.getFailCount()),
            entity.getStatus(),
            sourceRoute)),
        sourceRoute,
        "deploy",
        null));
  }

  private void syncUnifiedProjectionOnApproval(
      DeployTaskMapper.DeployTaskRow task,
      String targetStatus,
      String comment,
      LocalDateTime now) {
    String environment = resolveEnvironment(loadParams(task.getId()));
    String sourceRoute = buildSourceRoute(task.getId());
    boolean rejected = STATUS_REJECTED.equals(targetStatus);
    unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
        "deploy",
        task.getId(),
        rejected ? "failed" : "pending",
        rejected ? now : null,
        rejected ? "发布任务已拒绝" : "发布任务已审批，等待执行",
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
            task.getAppName(),
            environment,
            defaultZero(task.getTargetCount()),
            defaultZero(task.getSuccessCount()),
            defaultZero(task.getFailCount()),
            targetStatus,
            sourceRoute)),
        rejected ? comment : null));
  }

  String resolveEnvironment(Map<String, String> params) {
    return firstNonBlank(
        params.get("environment"),
        params.get("env"),
        params.get("profile"),
        params.get("namespace"));
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  String buildDeploySummary(String appName, String environment, Integer targetCount, Integer successCount, Integer failCount) {
    return String.format(
        "发布 %s 到 %s，目标 %d，成功 %d，失败 %d",
        appName,
        environment == null ? "default" : environment,
        defaultZero(targetCount),
        defaultZero(successCount),
        defaultZero(failCount));
  }

  String buildSourceRoute(Long taskId) {
    return "/deploy/task?taskId=" + taskId;
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

  private HostSummary loadHostSummary(Long taskId) {
    DeployTaskHostMapper.DeployTaskHostSummaryRow row = deployTaskHostMapper.summarizeByTaskId(taskId);
    if (row == null) {
      return new HostSummary(0, 0, 0, 0, 0, 0);
    }
    return new HostSummary(
        defaultZero(row.getTotalHosts()),
        defaultZero(row.getPendingHosts()),
        defaultZero(row.getRunningHosts()),
        defaultZero(row.getSuccessHosts()),
        defaultZero(row.getFailedHosts()),
        defaultZero(row.getCancelledHosts()));
  }

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
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

    syncUnifiedProjectionOnApproval(task, targetStatus, entity.getApprovalComment(), now);
    return toDeployTaskRecord(requireTask(taskId), loadParams(taskId));
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
    ensureSupportedStatus(row.getStatus());
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

  private DeployTaskDetailRecord toDeployTaskDetailRecord(DeployTaskMapper.DeployTaskRow row,
                                                          Map<String, String> params,
                                                          HostSummary summary) {
    ensureSupportedStatus(row.getStatus());
    return new DeployTaskDetailRecord(
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
        params,
        summary.totalHosts(),
        summary.pendingHosts(),
        summary.runningHosts(),
        summary.successHosts(),
        summary.failedHosts(),
        summary.cancelledHosts());
  }

  private DeployTaskHostRecord toDeployTaskHostRecord(DeployTaskHostMapper.DeployTaskHostRow row) {
    return new DeployTaskHostRecord(
        row.getId(),
        row.getTaskId(),
        row.getHostId(),
        row.getHostName(),
        row.getIpAddress(),
        row.getStatus(),
        row.getCurrentStep(),
        row.getStartedAt(),
        row.getFinishedAt(),
        row.getErrorMsg());
  }

  private DeployTaskLogRecord toDeployTaskLogRecord(DeployTaskLogMapper.DeployTaskLogRow row) {
    return new DeployTaskLogRecord(
        row.getId(),
        row.getTaskId(),
        row.getTaskHostId(),
        row.getLogLevel(),
        row.getLogContent(),
        row.getCreatedAt());
  }

  private void ensureSupportedStatus(String status) {
    if (!SUPPORTED_STATUSES.contains(status)) {
      throw new IllegalStateException("unsupported deploy task status: " + status);
    }
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

  public record DeployTaskQuery(String keyword,
                                String status,
                                String taskType,
                                Long appId,
                                String environment,
                                LocalDateTime createdFrom,
                                LocalDateTime createdTo,
                                Integer page,
                                Integer pageSize,
                                String sortBy,
                                String sortOrder) {
  }

  public record DeployTaskPage(int page, int pageSize, long total, List<DeployTaskRecord> records) {
  }

  public static class DeployTaskRecord {
    private final Long id;
    private final String taskNo;
    private final String taskName;
    private final String taskType;
    private final Long appId;
    private final String appName;
    private final Long versionId;
    private final String versionNo;
    private final Long originTaskId;
    private final String status;
    private final String batchStrategy;
    private final Integer batchSize;
    private final Integer targetCount;
    private final Integer successCount;
    private final Integer failCount;
    private final String operatorName;
    private final String approvalOperatorName;
    private final String approvalComment;
    private final LocalDateTime approvalAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Map<String, String> params;

    public DeployTaskRecord(Long id,
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
      this.id = id;
      this.taskNo = taskNo;
      this.taskName = taskName;
      this.taskType = taskType;
      this.appId = appId;
      this.appName = appName;
      this.versionId = versionId;
      this.versionNo = versionNo;
      this.originTaskId = originTaskId;
      this.status = status;
      this.batchStrategy = batchStrategy;
      this.batchSize = batchSize;
      this.targetCount = targetCount;
      this.successCount = successCount;
      this.failCount = failCount;
      this.operatorName = operatorName;
      this.approvalOperatorName = approvalOperatorName;
      this.approvalComment = approvalComment;
      this.approvalAt = approvalAt;
      this.startedAt = startedAt;
      this.finishedAt = finishedAt;
      this.createdAt = createdAt;
      this.updatedAt = updatedAt;
      this.params = params;
    }

    public Long getId() { return id; }
    public String getTaskNo() { return taskNo; }
    public String getTaskName() { return taskName; }
    public String getTaskType() { return taskType; }
    public Long getAppId() { return appId; }
    public String getAppName() { return appName; }
    public Long getVersionId() { return versionId; }
    public String getVersionNo() { return versionNo; }
    public Long getOriginTaskId() { return originTaskId; }
    public String getStatus() { return status; }
    public String getBatchStrategy() { return batchStrategy; }
    public Integer getBatchSize() { return batchSize; }
    public Integer getTargetCount() { return targetCount; }
    public Integer getSuccessCount() { return successCount; }
    public Integer getFailCount() { return failCount; }
    public String getOperatorName() { return operatorName; }
    public String getApprovalOperatorName() { return approvalOperatorName; }
    public String getApprovalComment() { return approvalComment; }
    public LocalDateTime getApprovalAt() { return approvalAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Map<String, String> getParams() { return params; }
  }

  public static class DeployTaskDetailRecord extends DeployTaskRecord {
    private final int totalHosts;
    private final int pendingHosts;
    private final int runningHosts;
    private final int successHosts;
    private final int failedHosts;
    private final int cancelledHosts;

    public DeployTaskDetailRecord(Long id,
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
                                  Map<String, String> params,
                                  int totalHosts,
                                  int pendingHosts,
                                  int runningHosts,
                                  int successHosts,
                                  int failedHosts,
                                  int cancelledHosts) {
      super(id,
          taskNo,
          taskName,
          taskType,
          appId,
          appName,
          versionId,
          versionNo,
          originTaskId,
          status,
          batchStrategy,
          batchSize,
          targetCount,
          successCount,
          failCount,
          operatorName,
          approvalOperatorName,
          approvalComment,
          approvalAt,
          startedAt,
          finishedAt,
          createdAt,
          updatedAt,
          params);
      this.totalHosts = totalHosts;
      this.pendingHosts = pendingHosts;
      this.runningHosts = runningHosts;
      this.successHosts = successHosts;
      this.failedHosts = failedHosts;
      this.cancelledHosts = cancelledHosts;
    }

    public int getTotalHosts() { return totalHosts; }
    public int getPendingHosts() { return pendingHosts; }
    public int getRunningHosts() { return runningHosts; }
    public int getSuccessHosts() { return successHosts; }
    public int getFailedHosts() { return failedHosts; }
    public int getCancelledHosts() { return cancelledHosts; }
  }

  public record DeployTaskHostQuery(String status,
                                    String keyword,
                                    Integer page,
                                    Integer pageSize) {
  }

  public record DeployTaskHostPage(int page, int pageSize, long total, List<DeployTaskHostRecord> records) {
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

  public record DeployTaskLogQuery(Long hostId,
                                   String keyword,
                                   Integer page,
                                   Integer pageSize) {
  }

  public record DeployTaskLogPage(int page, int pageSize, long total, List<DeployTaskLogRecord> records) {
  }

  public record DeployTaskLogRecord(Long id,
                                    Long taskId,
                                    Long taskHostId,
                                    String logLevel,
                                    String logContent,
                                    LocalDateTime createdAt) {
  }

  public record TaskCenterQuery(String keyword,
                                String status,
                                String sourceType,
                                String taskType,
                                String priority,
                                Integer page,
                                Integer pageSize,
                                String sortBy,
                                String sortOrder) {
  }

  public record TaskCenterPage(int page, int pageSize, long total, List<TaskCenterRecord> records) {
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
                                 String priority,
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

  private record NormalizedDeployTaskQuery(String keyword,
                                           String status,
                                           String taskType,
                                           Long appId,
                                           String environment,
                                           LocalDateTime createdFrom,
                                           LocalDateTime createdTo,
                                           int page,
                                           int pageSize,
                                           String sortBy,
                                           String sortOrder,
                                           int offset) {
  }

  private record NormalizedTaskCenterQuery(String keyword,
                                           String status,
                                           String sourceType,
                                           String taskType,
                                           String priority,
                                           int page,
                                           int pageSize,
                                           String sortBy,
                                           String sortOrder,
                                           int offset) {
  }

  private record NormalizedDeployTaskHostQuery(String status,
                                               String keyword,
                                               int page,
                                               int pageSize,
                                               int offset) {
  }

  private record NormalizedDeployTaskLogQuery(Long hostId,
                                              String keyword,
                                              int page,
                                              int pageSize,
                                              int offset) {
  }

  private record HostSummary(int totalHosts,
                             int pendingHosts,
                             int runningHosts,
                             int successHosts,
                             int failedHosts,
                             int cancelledHosts) {
  }
}
