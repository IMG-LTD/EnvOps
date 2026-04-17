package com.img.envops.modules.deploy.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeployTaskParamMapper {

  @Insert("""
      INSERT INTO deploy_task_param (
        task_id,
        param_key,
        param_value,
        secret_flag
      )
      VALUES (
        #{taskId},
        #{paramKey},
        #{paramValue},
        #{secretFlag}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertParam(DeployTaskParamEntity entity);

  @Select("""
      SELECT id,
             task_id AS taskId,
             param_key AS paramKey,
             param_value AS paramValue,
             secret_flag AS secretFlag
      FROM deploy_task_param
      WHERE task_id = #{taskId}
      ORDER BY id
      """)
  List<DeployTaskParamRow> findByTaskId(@Param("taskId") Long taskId);

  class DeployTaskParamRow {
    private Long id;
    private Long taskId;
    private String paramKey;
    private String paramValue;
    private Integer secretFlag;

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

    public String getParamKey() {
      return paramKey;
    }

    public void setParamKey(String paramKey) {
      this.paramKey = paramKey;
    }

    public String getParamValue() {
      return paramValue;
    }

    public void setParamValue(String paramValue) {
      this.paramValue = paramValue;
    }

    public Integer getSecretFlag() {
      return secretFlag;
    }

    public void setSecretFlag(Integer secretFlag) {
      this.secretFlag = secretFlag;
    }
  }

  class DeployTaskParamEntity {
    private Long id;
    private Long taskId;
    private String paramKey;
    private String paramValue;
    private Integer secretFlag;

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

    public String getParamKey() {
      return paramKey;
    }

    public void setParamKey(String paramKey) {
      this.paramKey = paramKey;
    }

    public String getParamValue() {
      return paramValue;
    }

    public void setParamValue(String paramValue) {
      this.paramValue = paramValue;
    }

    public Integer getSecretFlag() {
      return secretFlag;
    }

    public void setSecretFlag(Integer secretFlag) {
      this.secretFlag = secretFlag;
    }
  }
}
