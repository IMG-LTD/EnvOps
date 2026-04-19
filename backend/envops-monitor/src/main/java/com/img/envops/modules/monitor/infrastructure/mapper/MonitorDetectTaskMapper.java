package com.img.envops.modules.monitor.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonitorDetectTaskMapper {

  @Insert("""
      INSERT INTO monitor_detect_task (task_name, host_id, target, schedule, last_run_at, last_result, created_at)
      VALUES (#{taskName}, #{hostId}, #{target}, #{schedule}, #{lastRunAt}, #{lastResult}, #{createdAt})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertDetectTask(DetectTaskEntity detectTask);

  @Select("""
      SELECT id,
             task_name AS taskName,
             host_id AS hostId,
             target,
             schedule,
             last_run_at AS lastRunAt,
             last_result AS lastResult,
             created_at AS createdAt
      FROM monitor_detect_task
      WHERE id = #{id}
      """)
  DetectTaskRow findById(@Param("id") Long id);

  @Select("""
      SELECT id,
             task_name AS taskName,
             host_id AS hostId,
             target,
             schedule,
             last_run_at AS lastRunAt,
             last_result AS lastResult,
             created_at AS createdAt
      FROM monitor_detect_task
      ORDER BY created_at DESC, id DESC
      """)
  List<DetectTaskRow> findAll();

  @Update("""
      UPDATE monitor_detect_task
      SET last_run_at = #{lastRunAt},
          last_result = #{lastResult}
      WHERE id = #{id}
      """)
  int updateLastExecution(@Param("id") Long id,
                          @Param("lastRunAt") LocalDateTime lastRunAt,
                          @Param("lastResult") String lastResult);

  class DetectTaskRow {
    private Long id;
    private String taskName;
    private Long hostId;
    private String target;
    private String schedule;
    private LocalDateTime lastRunAt;
    private String lastResult;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getTaskName() {
      return taskName;
    }

    public void setTaskName(String taskName) {
      this.taskName = taskName;
    }

    public Long getHostId() {
      return hostId;
    }

    public void setHostId(Long hostId) {
      this.hostId = hostId;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }

    public String getSchedule() {
      return schedule;
    }

    public void setSchedule(String schedule) {
      this.schedule = schedule;
    }

    public LocalDateTime getLastRunAt() {
      return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
      this.lastRunAt = lastRunAt;
    }

    public String getLastResult() {
      return lastResult;
    }

    public void setLastResult(String lastResult) {
      this.lastResult = lastResult;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  class DetectTaskEntity {
    private Long id;
    private String taskName;
    private Long hostId;
    private String target;
    private String schedule;
    private LocalDateTime lastRunAt;
    private String lastResult;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getTaskName() {
      return taskName;
    }

    public void setTaskName(String taskName) {
      this.taskName = taskName;
    }

    public Long getHostId() {
      return hostId;
    }

    public void setHostId(Long hostId) {
      this.hostId = hostId;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }

    public String getSchedule() {
      return schedule;
    }

    public void setSchedule(String schedule) {
      this.schedule = schedule;
    }

    public LocalDateTime getLastRunAt() {
      return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
      this.lastRunAt = lastRunAt;
    }

    public String getLastResult() {
      return lastResult;
    }

    public void setLastResult(String lastResult) {
      this.lastResult = lastResult;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
