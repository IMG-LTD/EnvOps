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
public interface DeployTaskHostMapper {

  @Insert("""
      INSERT INTO deploy_task_host (
        task_id,
        host_id,
        status,
        current_step,
        started_at,
        finished_at,
        error_msg
      )
      VALUES (
        #{taskId},
        #{hostId},
        #{status},
        #{currentStep},
        #{startedAt},
        #{finishedAt},
        #{errorMsg}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertTaskHost(DeployTaskHostEntity entity);

  @Select("""
      SELECT dth.id,
             dth.task_id AS taskId,
             dth.host_id AS hostId,
             ah.host_name AS hostName,
             ah.ip_address AS ipAddress,
             dth.status,
             dth.current_step AS currentStep,
             dth.started_at AS startedAt,
             dth.finished_at AS finishedAt,
             dth.error_msg AS errorMsg
      FROM deploy_task_host dth
      JOIN asset_host ah ON ah.id = dth.host_id
      WHERE dth.task_id = #{taskId}
      ORDER BY dth.id
      """)
  List<DeployTaskHostRow> findByTaskId(@Param("taskId") Long taskId);

  @Select("""
      SELECT dth.id,
             dth.task_id AS taskId,
             dth.host_id AS hostId,
             ah.host_name AS hostName,
             ah.ip_address AS ipAddress,
             dth.status,
             dth.current_step AS currentStep,
             dth.started_at AS startedAt,
             dth.finished_at AS finishedAt,
             dth.error_msg AS errorMsg
      FROM deploy_task_host dth
      JOIN asset_host ah ON ah.id = dth.host_id
      WHERE dth.id = #{id}
      """)
  DeployTaskHostRow findById(@Param("id") Long id);

  @Update("""
      UPDATE deploy_task_host
      SET status = #{status},
          current_step = #{currentStep},
          started_at = #{startedAt},
          finished_at = #{finishedAt},
          error_msg = #{errorMsg}
      WHERE id = #{id}
      """)
  int updateHostExecutionState(DeployTaskHostEntity entity);

  @Update("""
      UPDATE deploy_task_host
      SET status = 'PENDING',
          current_step = NULL,
          started_at = NULL,
          finished_at = NULL,
          error_msg = NULL
      WHERE task_id = #{taskId}
        AND status <> 'SUCCESS'
      """)
  int resetForRetry(@Param("taskId") Long taskId);

  class DeployTaskHostRow {
    private Long id;
    private Long taskId;
    private Long hostId;
    private String hostName;
    private String ipAddress;
    private String status;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMsg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
  }

  class DeployTaskHostEntity {
    private Long id;
    private Long taskId;
    private Long hostId;
    private String status;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMsg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
  }
}
