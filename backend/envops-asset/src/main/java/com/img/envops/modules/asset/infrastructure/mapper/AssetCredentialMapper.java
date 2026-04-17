package com.img.envops.modules.asset.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssetCredentialMapper {

  @Select("""
      SELECT id,
             name,
             credential_type AS credentialType,
             username,
             description,
             created_at AS createdAt
      FROM asset_credential
      ORDER BY created_at DESC, id DESC
      """)
  List<CredentialRow> findAll();

  @Insert("""
      INSERT INTO asset_credential (name, credential_type, username, secret, description, created_at)
      VALUES (#{name}, #{credentialType}, #{username}, #{secret}, #{description}, #{createdAt})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertCredential(CredentialEntity credential);

  @Select("""
      SELECT id,
             name,
             credential_type AS credentialType,
             username,
             description,
             created_at AS createdAt
      FROM asset_credential
      WHERE id = #{id}
      """)
  CredentialRow findById(@Param("id") Long id);

  class CredentialRow {
    private Long id;
    private String name;
    private String credentialType;
    private String username;
    private String description;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCredentialType() {
      return credentialType;
    }

    public void setCredentialType(String credentialType) {
      this.credentialType = credentialType;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  class CredentialEntity {
    private Long id;
    private String name;
    private String credentialType;
    private String username;
    private String secret;
    private String description;
    private LocalDateTime createdAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCredentialType() {
      return credentialType;
    }

    public void setCredentialType(String credentialType) {
      this.credentialType = credentialType;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
