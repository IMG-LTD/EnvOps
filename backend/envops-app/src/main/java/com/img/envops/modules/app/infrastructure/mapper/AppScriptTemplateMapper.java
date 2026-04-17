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
public interface AppScriptTemplateMapper {

  @Select("""
      SELECT id,
             template_code AS templateCode,
             template_name AS templateName,
             script_type AS scriptType,
             script_content AS scriptContent,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_script_template
      WHERE deleted = 0
      ORDER BY id DESC
      """)
  List<AppScriptTemplateRow> findAllActive();

  @Select("""
      SELECT id,
             template_code AS templateCode,
             template_name AS templateName,
             script_type AS scriptType,
             script_content AS scriptContent,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_script_template
      WHERE id = #{id}
        AND deleted = 0
      """)
  AppScriptTemplateRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO app_script_template (
        template_code,
        template_name,
        script_type,
        script_content,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{templateCode},
        #{templateName},
        #{scriptType},
        #{scriptContent},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertTemplate(AppScriptTemplateEntity entity);

  @Update("""
      UPDATE app_script_template
      SET template_code = #{templateCode},
          template_name = #{templateName},
          script_type = #{scriptType},
          script_content = #{scriptContent},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int updateTemplate(AppScriptTemplateEntity entity);

  @Update("""
      UPDATE app_script_template
      SET template_code = CONCAT(SUBSTRING(template_code, 1, 43), '#d', id),
          deleted = 1,
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int markDeleted(@Param("id") Long id,
                  @Param("updatedBy") String updatedBy,
                  @Param("updatedAt") LocalDateTime updatedAt);

  class AppScriptTemplateRow {
    private Long id;
    private String templateCode;
    private String templateName;
    private String scriptType;
    private String scriptContent;
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

    public String getTemplateCode() {
      return templateCode;
    }

    public void setTemplateCode(String templateCode) {
      this.templateCode = templateCode;
    }

    public String getTemplateName() {
      return templateName;
    }

    public void setTemplateName(String templateName) {
      this.templateName = templateName;
    }

    public String getScriptType() {
      return scriptType;
    }

    public void setScriptType(String scriptType) {
      this.scriptType = scriptType;
    }

    public String getScriptContent() {
      return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
      this.scriptContent = scriptContent;
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

  class AppScriptTemplateEntity {
    private Long id;
    private String templateCode;
    private String templateName;
    private String scriptType;
    private String scriptContent;
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

    public String getTemplateCode() {
      return templateCode;
    }

    public void setTemplateCode(String templateCode) {
      this.templateCode = templateCode;
    }

    public String getTemplateName() {
      return templateName;
    }

    public void setTemplateName(String templateName) {
      this.templateName = templateName;
    }

    public String getScriptType() {
      return scriptType;
    }

    public void setScriptType(String scriptType) {
      this.scriptType = scriptType;
    }

    public String getScriptContent() {
      return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
      this.scriptContent = scriptContent;
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
