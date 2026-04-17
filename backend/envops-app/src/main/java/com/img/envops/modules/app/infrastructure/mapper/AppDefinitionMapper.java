package com.img.envops.modules.app.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppDefinitionMapper {

  @Select("""
      SELECT id,
             app_code AS appCode,
             app_name AS appName,
             app_type AS appType,
             runtime_type AS runtimeType,
             deploy_mode AS deployMode,
             default_port AS defaultPort,
             health_check_path AS healthCheckPath,
             description,
             status,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_definition
      WHERE deleted = 0
      ORDER BY id DESC
      """)
  List<AppDefinitionRow> findAllActive();

  @Select("""
      SELECT id,
             app_code AS appCode,
             app_name AS appName,
             app_type AS appType,
             runtime_type AS runtimeType,
             deploy_mode AS deployMode,
             default_port AS defaultPort,
             health_check_path AS healthCheckPath,
             description,
             status,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_definition
      WHERE id = #{id}
        AND deleted = 0
      """)
  AppDefinitionRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO app_definition (
        app_code,
        app_name,
        app_type,
        runtime_type,
        deploy_mode,
        default_port,
        health_check_path,
        description,
        status,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{appCode},
        #{appName},
        #{appType},
        #{runtimeType},
        #{deployMode},
        #{defaultPort},
        #{healthCheckPath},
        #{description},
        #{status},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertApp(AppDefinitionEntity entity);

  @Update("""
      UPDATE app_definition
      SET app_code = #{appCode},
          app_name = #{appName},
          app_type = #{appType},
          runtime_type = #{runtimeType},
          deploy_mode = #{deployMode},
          default_port = #{defaultPort},
          health_check_path = #{healthCheckPath},
          description = #{description},
          status = #{status},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int updateApp(AppDefinitionEntity entity);

  @Update("""
      UPDATE app_definition
      SET app_code = CONCAT(SUBSTRING(app_code, 1, 43), '#d', id),
          deleted = 1,
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int markDeleted(@Param("id") Long id,
                  @Param("updatedBy") String updatedBy,
                  @Param("updatedAt") LocalDateTime updatedAt);

  class AppDefinitionRow {
    private Long id;
    private String appCode;
    private String appName;
    private String appType;
    private String runtimeType;
    private String deployMode;
    private Integer defaultPort;
    private String healthCheckPath;
    private String description;
    private Integer status;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getAppCode() {
      return appCode;
    }

    public void setAppCode(String appCode) {
      this.appCode = appCode;
    }

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getAppType() {
      return appType;
    }

    public void setAppType(String appType) {
      this.appType = appType;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
      this.runtimeType = runtimeType;
    }

    public String getDeployMode() {
      return deployMode;
    }

    public void setDeployMode(String deployMode) {
      this.deployMode = deployMode;
    }

    public Integer getDefaultPort() {
      return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
      this.defaultPort = defaultPort;
    }

    public String getHealthCheckPath() {
      return healthCheckPath;
    }

    public void setHealthCheckPath(String healthCheckPath) {
      this.healthCheckPath = healthCheckPath;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getStatus() {
      return status;
    }

    public void setStatus(Integer status) {
      this.status = status;
    }

    public String getCreatedBy() {
      return createdBy;
    }

    public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
      return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
      this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  class AppDefinitionEntity {
    private Long id;
    private String appCode;
    private String appName;
    private String appType;
    private String runtimeType;
    private String deployMode;
    private Integer defaultPort;
    private String healthCheckPath;
    private String description;
    private Integer status;
    private Integer deleted;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getAppCode() {
      return appCode;
    }

    public void setAppCode(String appCode) {
      this.appCode = appCode;
    }

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getAppType() {
      return appType;
    }

    public void setAppType(String appType) {
      this.appType = appType;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
      this.runtimeType = runtimeType;
    }

    public String getDeployMode() {
      return deployMode;
    }

    public void setDeployMode(String deployMode) {
      this.deployMode = deployMode;
    }

    public Integer getDefaultPort() {
      return defaultPort;
    }

    public void setDefaultPort(Integer defaultPort) {
      this.defaultPort = defaultPort;
    }

    public String getHealthCheckPath() {
      return healthCheckPath;
    }

    public void setHealthCheckPath(String healthCheckPath) {
      this.healthCheckPath = healthCheckPath;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getStatus() {
      return status;
    }

    public void setStatus(Integer status) {
      this.status = status;
    }

    public Integer getDeleted() {
      return deleted;
    }

    public void setDeleted(Integer deleted) {
      this.deleted = deleted;
    }

    public String getCreatedBy() {
      return createdBy;
    }

    public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
      return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
      this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }
}
