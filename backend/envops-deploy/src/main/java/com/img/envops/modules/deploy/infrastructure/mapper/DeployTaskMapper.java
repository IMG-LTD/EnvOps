package com.img.envops.modules.deploy.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeployTaskMapper {

  @Select("""
      SELECT dt.id,
             dt.task_no AS taskNo,
             dt.task_name AS taskName,
             dt.task_type AS taskType,
             dt.app_id AS appId,
             ad.app_name AS appName,
             dt.version_id AS versionId,
             dt.origin_task_id AS originTaskId,
             av.version_no AS versionNo,
             dt.status,
             dt.batch_strategy AS batchStrategy,
             dt.batch_size AS batchSize,
             dt.target_count AS targetCount,
             dt.success_count AS successCount,
             dt.fail_count AS failCount,
             dt.operator_name AS operatorName,
             dt.approval_operator_name AS approvalOperatorName,
             dt.approval_comment AS approvalComment,
             dt.approval_at AS approvalAt,
             dt.started_at AS startedAt,
             dt.finished_at AS finishedAt,
             dt.created_at AS createdAt,
             dt.updated_at AS updatedAt
      FROM deploy_task dt
      JOIN app_definition ad ON ad.id = dt.app_id
      JOIN app_version av ON av.id = dt.version_id
      WHERE dt.deleted = 0
      ORDER BY dt.id DESC
      """)
  List<DeployTaskRow> findAllActive();

  @Select("""
      SELECT dt.id,
             dt.task_no AS taskNo,
             dt.task_name AS taskName,
             dt.task_type AS taskType,
             dt.app_id AS appId,
             ad.app_name AS appName,
             dt.version_id AS versionId,
             dt.origin_task_id AS originTaskId,
             av.version_no AS versionNo,
             dt.status,
             dt.batch_strategy AS batchStrategy,
             dt.batch_size AS batchSize,
             dt.target_count AS targetCount,
             dt.success_count AS successCount,
             dt.fail_count AS failCount,
             dt.operator_name AS operatorName,
             dt.approval_operator_name AS approvalOperatorName,
             dt.approval_comment AS approvalComment,
             dt.approval_at AS approvalAt,
             dt.started_at AS startedAt,
             dt.finished_at AS finishedAt,
             dt.created_at AS createdAt,
             dt.updated_at AS updatedAt
      FROM deploy_task dt
      JOIN app_definition ad ON ad.id = dt.app_id
      JOIN app_version av ON av.id = dt.version_id
      WHERE dt.id = #{id}
        AND dt.deleted = 0
      """)
  DeployTaskRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO deploy_task (
        task_no,
        task_name,
        task_type,
        app_id,
        version_id,
        origin_task_id,
        status,
        batch_strategy,
        batch_size,
        target_count,
        success_count,
        fail_count,
        operator_name,
        approval_operator_name,
        approval_comment,
        approval_at,
        started_at,
        finished_at,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{taskNo},
        #{taskName},
        #{taskType},
        #{appId},
        #{versionId},
        #{originTaskId},
        #{status},
        #{batchStrategy},
        #{batchSize},
        #{targetCount},
        #{successCount},
        #{failCount},
        #{operatorName},
        #{approvalOperatorName},
        #{approvalComment},
        #{approvalAt},
        #{startedAt},
        #{finishedAt},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertTask(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = #{status},
          approval_operator_name = #{approvalOperatorName},
          approval_comment = #{approvalComment},
          approval_at = #{approvalAt},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'PENDING_APPROVAL'
        AND deleted = 0
      """)
  int updateApprovalDecision(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = 'RUNNING',
          started_at = COALESCE(started_at, #{startedAt}),
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'PENDING'
        AND deleted = 0
      """)
  int markRunning(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = 'CANCEL_REQUESTED',
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status IN ('PENDING', 'RUNNING')
        AND deleted = 0
      """)
  int markCancelRequested(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = #{status},
          success_count = #{successCount},
          fail_count = #{failCount},
          finished_at = #{finishedAt},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'RUNNING'
        AND deleted = 0
      """)
  int updateExecutionSummary(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = 'CANCELLED',
          success_count = #{successCount},
          fail_count = #{failCount},
          finished_at = #{finishedAt},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'PENDING'
        AND deleted = 0
      """)
  int markCancelledFromPending(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = 'CANCELLED',
          success_count = #{successCount},
          fail_count = #{failCount},
          finished_at = #{finishedAt},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status = 'CANCEL_REQUESTED'
        AND deleted = 0
      """)
  int markCancelledFromCancelRequested(DeployTaskEntity entity);

  @Update("""
      UPDATE deploy_task
      SET status = 'PENDING',
          success_count = 0,
          fail_count = 0,
          started_at = NULL,
          finished_at = NULL,
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND status IN ('FAILED', 'CANCELLED')
        AND deleted = 0
      """)
  int resetForRetry(DeployTaskEntity entity);

  class DeployTaskRow {
    private Long id;
    private String taskNo;
    private String taskName;
    private String taskType;
    private Long appId;
    private String appName;
    private Long versionId;
    private Long originTaskId;
    private String versionNo;
    private String status;
    private String batchStrategy;
    private Integer batchSize;
    private Integer targetCount;
    private Integer successCount;
    private Integer failCount;
    private String operatorName;
    private String approvalOperatorName;
    private String approvalComment;
    private LocalDateTime approvalAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getOriginTaskId() { return originTaskId; }
    public void setOriginTaskId(Long originTaskId) { this.originTaskId = originTaskId; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBatchStrategy() { return batchStrategy; }
    public void setBatchStrategy(String batchStrategy) { this.batchStrategy = batchStrategy; }
    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
    public Integer getTargetCount() { return targetCount; }
    public void setTargetCount(Integer targetCount) { this.targetCount = targetCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getApprovalOperatorName() { return approvalOperatorName; }
    public void setApprovalOperatorName(String approvalOperatorName) { this.approvalOperatorName = approvalOperatorName; }
    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }
    public LocalDateTime getApprovalAt() { return approvalAt; }
    public void setApprovalAt(LocalDateTime approvalAt) { this.approvalAt = approvalAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  }

  class DeployTaskEntity {
    private Long id;
    private String taskNo;
    private String taskName;
    private String taskType;
    private Long appId;
    private Long versionId;
    private Long originTaskId;
    private String status;
    private String batchStrategy;
    private Integer batchSize;
    private Integer targetCount;
    private Integer successCount;
    private Integer failCount;
    private String operatorName;
    private String approvalOperatorName;
    private String approvalComment;
    private LocalDateTime approvalAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer deleted;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getOriginTaskId() { return originTaskId; }
    public void setOriginTaskId(Long originTaskId) { this.originTaskId = originTaskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBatchStrategy() { return batchStrategy; }
    public void setBatchStrategy(String batchStrategy) { this.batchStrategy = batchStrategy; }
    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
    public Integer getTargetCount() { return targetCount; }
    public void setTargetCount(Integer targetCount) { this.targetCount = targetCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getApprovalOperatorName() { return approvalOperatorName; }
    public void setApprovalOperatorName(String approvalOperatorName) { this.approvalOperatorName = approvalOperatorName; }
    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }
    public LocalDateTime getApprovalAt() { return approvalAt; }
    public void setApprovalAt(LocalDateTime approvalAt) { this.approvalAt = approvalAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  }
}
