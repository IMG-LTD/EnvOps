package com.img.envops.modules.deploy.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeployTaskLogMapper {

  @Insert("""
      INSERT INTO deploy_task_log (
        task_id,
        task_host_id,
        log_level,
        log_content,
        created_at
      )
      VALUES (
        #{taskId},
        #{taskHostId},
        #{logLevel},
        #{logContent},
        #{createdAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertLog(DeployTaskLogEntity entity);

  @Select({
      "<script>",
      "SELECT COUNT(*)",
      "FROM deploy_task_log dtl",
      "LEFT JOIN deploy_task_host dth ON dth.id = dtl.task_host_id",
      "<where>",
      "  dtl.task_id = #{taskId}",
      "  <if test='hostId != null'>AND dth.host_id = #{hostId}</if>",
      "  <if test='keyword != null'>AND dtl.log_content LIKE CONCAT('%', #{keyword}, '%')</if>",
      "</where>",
      "</script>"
  })
  long countByTaskIdAndQuery(@Param("taskId") Long taskId,
                             @Param("hostId") Long hostId,
                             @Param("keyword") String keyword);

  @Select({
      "<script>",
      "SELECT dtl.id,",
      "       dtl.task_id AS taskId,",
      "       dtl.task_host_id AS taskHostId,",
      "       dtl.log_level AS logLevel,",
      "       dtl.log_content AS logContent,",
      "       dtl.created_at AS createdAt",
      "FROM deploy_task_log dtl",
      "LEFT JOIN deploy_task_host dth ON dth.id = dtl.task_host_id",
      "<where>",
      "  dtl.task_id = #{taskId}",
      "  <if test='hostId != null'>AND dth.host_id = #{hostId}</if>",
      "  <if test='keyword != null'>AND dtl.log_content LIKE CONCAT('%', #{keyword}, '%')</if>",
      "</where>",
      "ORDER BY dtl.id",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<DeployTaskLogRow> findByTaskIdAndQuery(@Param("taskId") Long taskId,
                                              @Param("hostId") Long hostId,
                                              @Param("keyword") String keyword,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

  @Select("""
      SELECT id,
             task_id AS taskId,
             task_host_id AS taskHostId,
             log_level AS logLevel,
             log_content AS logContent,
             created_at AS createdAt
      FROM deploy_task_log
      WHERE task_id = #{taskId}
      ORDER BY id
      """)
  List<DeployTaskLogRow> findByTaskId(@Param("taskId") Long taskId);

  class DeployTaskLogRow {
    private Long id;
    private Long taskId;
    private Long taskHostId;
    private String logLevel;
    private String logContent;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getTaskId() {
      return taskId;
    }

    public void setTaskId(Long taskId) {
      this.taskId = taskId;
    }

    public Long getTaskHostId() {
      return taskHostId;
    }

    public void setTaskHostId(Long taskHostId) {
      this.taskHostId = taskHostId;
    }

    public String getLogLevel() {
      return logLevel;
    }

    public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }

    public String getLogContent() {
      return logContent;
    }

    public void setLogContent(String logContent) {
      this.logContent = logContent;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  class DeployTaskLogEntity {
    private Long id;
    private Long taskId;
    private Long taskHostId;
    private String logLevel;
    private String logContent;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getTaskId() {
      return taskId;
    }

    public void setTaskId(Long taskId) {
      this.taskId = taskId;
    }

    public Long getTaskHostId() {
      return taskHostId;
    }

    public void setTaskHostId(Long taskHostId) {
      this.taskHostId = taskHostId;
    }

    public String getLogLevel() {
      return logLevel;
    }

    public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }

    public String getLogContent() {
      return logContent;
    }

    public void setLogContent(String logContent) {
      this.logContent = logContent;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
