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
public interface AppPackageMapper {

  @Select("""
      SELECT id,
             package_name AS packageName,
             package_type AS packageType,
             file_path AS filePath,
             file_size AS fileSize,
             file_hash AS fileHash,
             storage_type AS storageType,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_package
      WHERE deleted = 0
      ORDER BY id DESC
      """)
  List<AppPackageRow> findAllActive();

  @Select("""
      SELECT id,
             package_name AS packageName,
             package_type AS packageType,
             file_path AS filePath,
             file_size AS fileSize,
             file_hash AS fileHash,
             storage_type AS storageType,
             created_by AS createdBy,
             updated_by AS updatedBy,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM app_package
      WHERE id = #{id}
        AND deleted = 0
      """)
  AppPackageRow findActiveById(@Param("id") Long id);

  @Insert("""
      INSERT INTO app_package (
        package_name,
        package_type,
        file_path,
        file_size,
        file_hash,
        storage_type,
        deleted,
        created_by,
        updated_by,
        created_at,
        updated_at
      )
      VALUES (
        #{packageName},
        #{packageType},
        #{filePath},
        #{fileSize},
        #{fileHash},
        #{storageType},
        #{deleted},
        #{createdBy},
        #{updatedBy},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertPackage(AppPackageEntity entity);

  @Update("""
      UPDATE app_package
      SET deleted = 1,
          updated_by = #{updatedBy},
          updated_at = #{updatedAt}
      WHERE id = #{id}
        AND deleted = 0
      """)
  int markDeleted(@Param("id") Long id,
                  @Param("updatedBy") String updatedBy,
                  @Param("updatedAt") LocalDateTime updatedAt);

  class AppPackageRow {
    private Long id;
    private String packageName;
    private String packageType;
    private String filePath;
    private Long fileSize;
    private String fileHash;
    private String storageType;
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

    public String getPackageName() {
      return packageName;
    }

    public void setPackageName(String packageName) {
      this.packageName = packageName;
    }

    public String getPackageType() {
      return packageType;
    }

    public void setPackageType(String packageType) {
      this.packageType = packageType;
    }

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public Long getFileSize() {
      return fileSize;
    }

    public void setFileSize(Long fileSize) {
      this.fileSize = fileSize;
    }

    public String getFileHash() {
      return fileHash;
    }

    public void setFileHash(String fileHash) {
      this.fileHash = fileHash;
    }

    public String getStorageType() {
      return storageType;
    }

    public void setStorageType(String storageType) {
      this.storageType = storageType;
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

  class AppPackageEntity {
    private Long id;
    private String packageName;
    private String packageType;
    private String filePath;
    private Long fileSize;
    private String fileHash;
    private String storageType;
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

    public String getPackageName() {
      return packageName;
    }

    public void setPackageName(String packageName) {
      this.packageName = packageName;
    }

    public String getPackageType() {
      return packageType;
    }

    public void setPackageType(String packageType) {
      this.packageType = packageType;
    }

    public String getFilePath() {
      return filePath;
    }

    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    public Long getFileSize() {
      return fileSize;
    }

    public void setFileSize(Long fileSize) {
      this.fileSize = fileSize;
    }

    public String getFileHash() {
      return fileHash;
    }

    public void setFileHash(String fileHash) {
      this.fileHash = fileHash;
    }

    public String getStorageType() {
      return storageType;
    }

    public void setStorageType(String storageType) {
      this.storageType = storageType;
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
