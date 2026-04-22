package com.img.envops.modules.task.infrastructure.mapper;

import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UnifiedTaskCenterMapper {

  @Insert("""
      INSERT INTO unified_task_center (
        task_type, task_name, status, triggered_by, started_at, finished_at,
        summary, detail_preview, source_id, source_route, module_name, error_summary,
        created_at, updated_at
      ) VALUES (
        #{taskType}, #{taskName}, #{status}, #{triggeredBy}, #{startedAt}, #{finishedAt},
        #{summary}, #{detailPreview}, #{sourceId}, #{sourceRoute}, #{moduleName}, #{errorSummary},
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UnifiedTaskCenterEntity entity);

  @Select("""
      SELECT id,
             task_type AS taskType,
             task_name AS taskName,
             status,
             triggered_by AS triggeredBy,
             started_at AS startedAt,
             finished_at AS finishedAt,
             summary,
             detail_preview AS detailPreview,
             source_id AS sourceId,
             source_route AS sourceRoute,
             module_name AS moduleName,
             error_summary AS errorSummary,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM unified_task_center
      WHERE task_type = #{taskType}
        AND source_id = #{sourceId}
      ORDER BY id DESC
      LIMIT 1
      """)
  UnifiedTaskCenterEntity findEntityBySource(@Param("taskType") String taskType, @Param("sourceId") Long sourceId);

  @Select({
      "<script>",
      "SELECT COUNT(*)",
      "FROM unified_task_center",
      "<where>",
      "  <if test='keyword != null'>AND (task_name LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%') OR triggered_by LIKE CONCAT('%', #{keyword}, '%'))</if>",
      "  <if test='taskType != null'>AND task_type = #{taskType}</if>",
      "  <if test='status != null'>AND status = #{status}</if>",
      "  <if test='startedFrom != null'>AND started_at <![CDATA[>=]]> #{startedFrom}</if>",
      "  <if test='startedTo != null'>AND started_at <![CDATA[<=]]> #{startedTo}</if>",
      "</where>",
      "</script>"
  })
  long countByQuery(
      @Param("keyword") String keyword,
      @Param("taskType") String taskType,
      @Param("status") String status,
      @Param("startedFrom") LocalDateTime startedFrom,
      @Param("startedTo") LocalDateTime startedTo);

  @Select({
      "<script>",
      "SELECT id,",
      "       task_type AS taskType,",
      "       task_name AS taskName,",
      "       status,",
      "       triggered_by AS triggeredBy,",
      "       started_at AS startedAt,",
      "       finished_at AS finishedAt,",
      "       summary,",
      "       detail_preview AS detailPreview,",
      "       source_id AS sourceId,",
      "       source_route AS sourceRoute,",
      "       module_name AS moduleName,",
      "       error_summary AS errorSummary,",
      "       created_at AS createdAt,",
      "       updated_at AS updatedAt",
      "FROM unified_task_center",
      "<where>",
      "  <if test='keyword != null'>AND (task_name LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%') OR triggered_by LIKE CONCAT('%', #{keyword}, '%'))</if>",
      "  <if test='taskType != null'>AND task_type = #{taskType}</if>",
      "  <if test='status != null'>AND status = #{status}</if>",
      "  <if test='startedFrom != null'>AND started_at <![CDATA[>=]]> #{startedFrom}</if>",
      "  <if test='startedTo != null'>AND started_at <![CDATA[<=]]> #{startedTo}</if>",
      "</where>",
      "ORDER BY started_at DESC, id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<UnifiedTaskCenterRow> findByQuery(
      @Param("keyword") String keyword,
      @Param("taskType") String taskType,
      @Param("status") String status,
      @Param("startedFrom") LocalDateTime startedFrom,
      @Param("startedTo") LocalDateTime startedTo,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Select("""
      SELECT id,
             task_type AS taskType,
             task_name AS taskName,
             status,
             triggered_by AS triggeredBy,
             started_at AS startedAt,
             finished_at AS finishedAt,
             summary,
             detail_preview AS detailPreview,
             source_id AS sourceId,
             source_route AS sourceRoute,
             module_name AS moduleName,
             error_summary AS errorSummary,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM unified_task_center
      WHERE id = #{id}
      """)
  UnifiedTaskCenterRow findById(@Param("id") Long id);

  @Update("""
      UPDATE unified_task_center
      SET status = #{status},
          finished_at = #{finishedAt},
          summary = #{summary},
          detail_preview = COALESCE(#{detailPreview}, detail_preview),
          error_summary = #{errorSummary},
          updated_at = CURRENT_TIMESTAMP
      WHERE id = #{id}
      """)
  int updateById(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("finishedAt") LocalDateTime finishedAt,
      @Param("summary") String summary,
      @Param("detailPreview") String detailPreview,
      @Param("errorSummary") String errorSummary);

  @Select("""
      SELECT dt.id AS sourceId,
             dt.task_name AS taskName,
             dt.status AS status,
             dt.operator_name AS operatorName,
             ad.app_name AS appName,
             COALESCE(
               NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'environment' THEN dtp.param_value END)), ''),
               NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'env' THEN dtp.param_value END)), ''),
               NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'profile' THEN dtp.param_value END)), ''),
               NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'namespace' THEN dtp.param_value END)), '')
             ) AS environment,
             dt.target_count AS targetCount,
             dt.success_count AS successCount,
             dt.fail_count AS failCount,
             dt.created_at AS createdAt,
             dt.started_at AS startedAt,
             dt.finished_at AS finishedAt
      FROM deploy_task dt
      JOIN app_definition ad ON ad.id = dt.app_id
      LEFT JOIN deploy_task_param dtp ON dtp.task_id = dt.id
      WHERE dt.deleted = 0
        AND dt.id > #{afterSourceId}
        AND NOT EXISTS (
          SELECT 1
          FROM unified_task_center utc
          WHERE utc.task_type = 'deploy'
            AND utc.source_id = dt.id
        )
      GROUP BY dt.id,
               dt.task_name,
               dt.status,
               dt.operator_name,
               ad.app_name,
               dt.target_count,
               dt.success_count,
               dt.fail_count,
               dt.created_at,
               dt.started_at,
               dt.finished_at
      ORDER BY dt.id ASC
      LIMIT #{limit}
      """)
  List<DeployBackfillRow> findDeployRowsMissingProjection(@Param("afterSourceId") long afterSourceId,
                                                          @Param("limit") int limit);

  class DeployBackfillRow {
    private Long sourceId;
    private String taskName;
    private String status;
    private String operatorName;
    private String appName;
    private String environment;
    private Integer targetCount;
    private Integer successCount;
    private Integer failCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public Long getSourceId() {
      return sourceId;
    }

    public void setSourceId(Long sourceId) {
      this.sourceId = sourceId;
    }

    public String getTaskName() {
      return taskName;
    }

    public void setTaskName(String taskName) {
      this.taskName = taskName;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getOperatorName() {
      return operatorName;
    }

    public void setOperatorName(String operatorName) {
      this.operatorName = operatorName;
    }

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public Integer getTargetCount() {
      return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
      this.targetCount = targetCount;
    }

    public Integer getSuccessCount() {
      return successCount;
    }

    public void setSuccessCount(Integer successCount) {
      this.successCount = successCount;
    }

    public Integer getFailCount() {
      return failCount;
    }

    public void setFailCount(Integer failCount) {
      this.failCount = failCount;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
      return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
      this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
      return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
      this.finishedAt = finishedAt;
    }
  }
}
