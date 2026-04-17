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
public interface AppConfigTemplateMapper {

  @Select("""
      SELECT id,
             template_code AS templateCode,
             template_name AS templateName,
             template_content AS templateContent,
             render_engine AS renderEngine,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_config_template
      WHERE deleted = 0
      ORDER BY id DESC
      """)
  List<AppConfigTemplateRow> findAllActive();

  @Select("""
      SELECT id,
             template_code AS templateCode,
             template_name AS templateName,
             template_content AS templateContent,
             render_engine AS renderEngine,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_config_template
      WHERE id = #{id}
        AND deleted = 0
      """)
  AppConfigTemplateRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO app_config_template (
        template_code,
        template_name,
        template_content,
        render_engine,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{templateCode},
        #{templateName},
        #{templateContent},
        #{renderEngine},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertTemplate(AppConfigTemplateEntity entity);

  @Update("""
      UPDATE app_config_template
      SET template_code = #{templateCode},
          template_name = #{templateName},
          template_content = #{templateContent},
          render_engine = #{renderEngine},
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int updateTemplate(AppConfigTemplateEntity entity);

  @Update("""
      UPDATE app_config_template
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

  class AppConfigTemplateRow {
    private Long id;
    private String templateCode;
    private String templateName;
    private String templateContent;
    private String renderEngine;
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

    public String getTemplateContent() {
      return templateContent;
    }

    public void setTemplateContent(String templateContent) {
      this.templateContent = templateContent;
    }

    public String getRenderEngine() {
      return renderEngine;
    }

    public void setRenderEngine(String renderEngine) {
      this.renderEngine = renderEngine;
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

  class AppConfigTemplateEntity {
    private Long id;
    private String templateCode;
    private String templateName;
    private String templateContent;
    private String renderEngine;
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

    public String getTemplateContent() {
      return templateContent;
    }

    public void setTemplateContent(String templateContent) {
      this.templateContent = templateContent;
    }

    public String getRenderEngine() {
      return renderEngine;
    }

    public void setRenderEngine(String renderEngine) {
      this.renderEngine = renderEngine;
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
