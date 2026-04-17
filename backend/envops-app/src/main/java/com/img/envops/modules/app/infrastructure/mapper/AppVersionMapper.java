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
public interface AppVersionMapper {

  @Select("""
      SELECT id,
             app_id AS appId,
             version_no AS versionNo,
             package_id AS packageId,
             config_template_id AS configTemplateId,
             script_template_id AS scriptTemplateId,
             changelog,
             status,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_version
      WHERE app_id = #{appId}
        AND deleted = 0
      ORDER BY id DESC
      """)
  List<AppVersionRow> findActiveByAppId(@Param("appId") Long appId);

  @Select("""
      SELECT id,
             app_id AS appId,
             version_no AS versionNo,
             package_id AS packageId,
             config_template_id AS configTemplateId,
             script_template_id AS scriptTemplateId,
             changelog,
             status,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_version
      WHERE id = #{id}
        AND deleted = 0
      """)
  AppVersionRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO app_version (
        app_id,
        version_no,
        package_id,
        config_template_id,
        script_template_id,
        changelog,
        status,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{appId},
        #{versionNo},
        #{packageId},
        #{configTemplateId},
        #{scriptTemplateId},
        #{changelog},
        #{status},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertVersion(AppVersionEntity entity);

  @Update("""
      UPDATE app_version
      SET version_no = #{versionNo},
          package_id = #{packageId},
          config_template_id = #{configTemplateId},
          script_template_id = #{scriptTemplateId},
          changelog = #{changelog},
          status = #{status},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int updateVersion(AppVersionEntity entity);

  @Update("""
      UPDATE app_version
      SET version_no = CONCAT(SUBSTRING(version_no, 1, 43), '#d', id),
          deleted = 1,
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int markDeleted(@Param("id") Long id,
                  @Param("updatedBy") String updatedBy,
                  @Param("updatedAt") LocalDateTime updatedAt);

  @Select("""
      SELECT COUNT(1)
      FROM app_version
      WHERE app_id = #{appId}
        AND deleted = 0
      """)
  int countActiveByAppId(@Param("appId") Long appId);

  @Select("""
      SELECT COUNT(1)
      FROM app_version
      WHERE package_id = #{packageId}
        AND deleted = 0
      """)
  int countActiveByPackageId(@Param("packageId") Long packageId);

  @Select("""
      SELECT COUNT(1)
      FROM app_version
      WHERE config_template_id = #{configTemplateId}
        AND deleted = 0
      """)
  int countActiveByConfigTemplateId(@Param("configTemplateId") Long configTemplateId);

  @Select("""
      SELECT COUNT(1)
      FROM app_version
      WHERE script_template_id = #{scriptTemplateId}
        AND deleted = 0
      """)
  int countActiveByScriptTemplateId(@Param("scriptTemplateId") Long scriptTemplateId);

  class AppVersionRow {
    private Long id;
    private Long appId;
    private String versionNo;
    private Long packageId;
    private Long configTemplateId;
    private Long scriptTemplateId;
    private String changelog;
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

    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long appId) {
      this.appId = appId;
    }

    public String getVersionNo() {
      return versionNo;
    }

    public void setVersionNo(String versionNo) {
      this.versionNo = versionNo;
    }

    public Long getPackageId() {
      return packageId;
    }

    public void setPackageId(Long packageId) {
      this.packageId = packageId;
    }

    public Long getConfigTemplateId() {
      return configTemplateId;
    }

    public void setConfigTemplateId(Long configTemplateId) {
      this.configTemplateId = configTemplateId;
    }

    public Long getScriptTemplateId() {
      return scriptTemplateId;
    }

    public void setScriptTemplateId(Long scriptTemplateId) {
      this.scriptTemplateId = scriptTemplateId;
    }

    public String getChangelog() {
      return changelog;
    }

    public void setChangelog(String changelog) {
      this.changelog = changelog;
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

  class AppVersionEntity {
    private Long id;
    private Long appId;
    private String versionNo;
    private Long packageId;
    private Long configTemplateId;
    private Long scriptTemplateId;
    private String changelog;
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

    public Long getAppId() {
      return appId;
    }

    public void setAppId(Long appId) {
      this.appId = appId;
    }

    public String getVersionNo() {
      return versionNo;
    }

    public void setVersionNo(String versionNo) {
      this.versionNo = versionNo;
    }

    public Long getPackageId() {
      return packageId;
    }

    public void setPackageId(Long packageId) {
      this.packageId = packageId;
    }

    public Long getConfigTemplateId() {
      return configTemplateId;
    }

    public void setConfigTemplateId(Long configTemplateId) {
      this.configTemplateId = configTemplateId;
    }

    public Long getScriptTemplateId() {
      return scriptTemplateId;
    }

    public void setScriptTemplateId(Long scriptTemplateId) {
      this.scriptTemplateId = scriptTemplateId;
    }

    public String getChangelog() {
      return changelog;
    }

    public void setChangelog(String changelog) {
      this.changelog = changelog;
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
