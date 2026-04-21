package com.img.envops.modules.asset.infrastructure.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssetDatabaseMapper {

  @Select({
      "<script>",
      "SELECT COUNT(*)",
      "FROM asset_database db",
      "LEFT JOIN asset_host host ON host.id = db.host_id",
      "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
      "<where>",
      "  <if test='keyword != null'>",
      "    AND (db.database_name LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.instance_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.owner_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(host.host_name, '') LIKE CONCAT('%', #{keyword}, '%'))",
      "  </if>",
      "  <if test='environment != null'>AND db.environment = #{environment}</if>",
      "  <if test='databaseType != null'>AND db.database_type = #{databaseType}</if>",
      "  <if test='lifecycleStatus != null'>AND db.lifecycle_status = #{lifecycleStatus}</if>",
      "  <if test='connectivityStatus != null'>AND db.connectivity_status = #{connectivityStatus}</if>",
      "</where>",
      "</script>"
  })
  long countDatabasesByQuery(@Param("keyword") String keyword,
                             @Param("environment") String environment,
                             @Param("databaseType") String databaseType,
                             @Param("lifecycleStatus") String lifecycleStatus,
                             @Param("connectivityStatus") String connectivityStatus);

  @Select({
      "<script>",
      "SELECT COALESCE(SUM(CASE WHEN db.lifecycle_status = 'managed' THEN 1 ELSE 0 END), 0) AS managedDatabases,",
      "       COALESCE(SUM(CASE WHEN db.connectivity_status = 'warning' THEN 1 ELSE 0 END), 0) AS warningDatabases,",
      "       COALESCE(SUM(CASE WHEN db.connectivity_status = 'online' THEN 1 ELSE 0 END), 0) AS onlineDatabases",
      "FROM asset_database db",
      "LEFT JOIN asset_host host ON host.id = db.host_id",
      "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
      "<where>",
      "  <if test='keyword != null'>",
      "    AND (db.database_name LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.instance_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.owner_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(host.host_name, '') LIKE CONCAT('%', #{keyword}, '%'))",
      "  </if>",
      "  <if test='environment != null'>AND db.environment = #{environment}</if>",
      "  <if test='databaseType != null'>AND db.database_type = #{databaseType}</if>",
      "  <if test='lifecycleStatus != null'>AND db.lifecycle_status = #{lifecycleStatus}</if>",
      "  <if test='connectivityStatus != null'>AND db.connectivity_status = #{connectivityStatus}</if>",
      "</where>",
      "</script>"
  })
  DatabaseSummaryRow summarizeDatabasesByQuery(@Param("keyword") String keyword,
                                               @Param("environment") String environment,
                                               @Param("databaseType") String databaseType,
                                               @Param("lifecycleStatus") String lifecycleStatus,
                                               @Param("connectivityStatus") String connectivityStatus);

  @Select({
      "<script>",
      "SELECT db.id,",
      "       db.database_name AS databaseName,",
      "       db.database_type AS databaseType,",
      "       db.environment,",
      "       db.host_id AS hostId,",
      "       host.host_name AS hostName,",
      "       host.ip_address AS hostIpAddress,",
      "       db.port,",
      "       db.instance_name AS instanceName,",
      "       db.credential_id AS credentialId,",
      "       credential.name AS credentialName,",
      "       db.owner_name AS ownerName,",
      "       db.lifecycle_status AS lifecycleStatus,",
      "       db.connectivity_status AS connectivityStatus,",
      "       db.connection_username AS connectionUsername,",
      "       db.connection_password AS connectionPassword,",
      "       db.description,",
      "       db.last_checked_at AS lastCheckedAt,",
      "       db.created_at AS createdAt,",
      "       db.updated_at AS updatedAt",
      "FROM asset_database db",
      "LEFT JOIN asset_host host ON host.id = db.host_id",
      "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
      "<where>",
      "  <if test='keyword != null'>",
      "    AND (db.database_name LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.instance_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.owner_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(host.host_name, '') LIKE CONCAT('%', #{keyword}, '%'))",
      "  </if>",
      "  <if test='environment != null'>AND db.environment = #{environment}</if>",
      "  <if test='databaseType != null'>AND db.database_type = #{databaseType}</if>",
      "  <if test='lifecycleStatus != null'>AND db.lifecycle_status = #{lifecycleStatus}</if>",
      "  <if test='connectivityStatus != null'>AND db.connectivity_status = #{connectivityStatus}</if>",
      "</where>",
      "ORDER BY db.id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<DatabaseRow> findDatabasesByQuery(@Param("keyword") String keyword,
                                         @Param("environment") String environment,
                                         @Param("databaseType") String databaseType,
                                         @Param("lifecycleStatus") String lifecycleStatus,
                                         @Param("connectivityStatus") String connectivityStatus,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

  @Select({
      "<script>",
      "SELECT db.id,",
      "       db.database_name AS databaseName,",
      "       db.database_type AS databaseType,",
      "       db.environment,",
      "       db.host_id AS hostId,",
      "       host.host_name AS hostName,",
      "       host.ip_address AS hostIpAddress,",
      "       db.port,",
      "       db.instance_name AS instanceName,",
      "       db.credential_id AS credentialId,",
      "       credential.name AS credentialName,",
      "       db.owner_name AS ownerName,",
      "       db.lifecycle_status AS lifecycleStatus,",
      "       db.connectivity_status AS connectivityStatus,",
      "       db.connection_username AS connectionUsername,",
      "       db.connection_password AS connectionPassword,",
      "       db.description,",
      "       db.last_checked_at AS lastCheckedAt,",
      "       db.created_at AS createdAt,",
      "       db.updated_at AS updatedAt",
      "FROM asset_database db",
      "LEFT JOIN asset_host host ON host.id = db.host_id",
      "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
      "WHERE db.id IN",
      "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
      "ORDER BY db.id DESC",
      "</script>"
  })
  List<DatabaseRow> findDatabasesByIds(@Param("ids") List<Long> ids);

  @Select({
      "<script>",
      "SELECT db.id,",
      "       db.database_name AS databaseName,",
      "       db.database_type AS databaseType,",
      "       db.environment,",
      "       db.host_id AS hostId,",
      "       host.host_name AS hostName,",
      "       host.ip_address AS hostIpAddress,",
      "       db.port,",
      "       db.instance_name AS instanceName,",
      "       db.credential_id AS credentialId,",
      "       credential.name AS credentialName,",
      "       db.owner_name AS ownerName,",
      "       db.lifecycle_status AS lifecycleStatus,",
      "       db.connectivity_status AS connectivityStatus,",
      "       db.connection_username AS connectionUsername,",
      "       db.connection_password AS connectionPassword,",
      "       db.description,",
      "       db.last_checked_at AS lastCheckedAt,",
      "       db.created_at AS createdAt,",
      "       db.updated_at AS updatedAt",
      "FROM asset_database db",
      "LEFT JOIN asset_host host ON host.id = db.host_id",
      "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
      "<where>",
      "  <if test='keyword != null'>",
      "    AND (db.database_name LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.instance_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(db.owner_name, '') LIKE CONCAT('%', #{keyword}, '%')",
      "      OR COALESCE(host.host_name, '') LIKE CONCAT('%', #{keyword}, '%'))",
      "  </if>",
      "  <if test='environment != null'>AND db.environment = #{environment}</if>",
      "  <if test='databaseType != null'>AND db.database_type = #{databaseType}</if>",
      "  <if test='lifecycleStatus != null'>AND db.lifecycle_status = #{lifecycleStatus}</if>",
      "  <if test='connectivityStatus != null'>AND db.connectivity_status = #{connectivityStatus}</if>",
      "</where>",
      "ORDER BY db.id DESC",
      "</script>"
  })
  List<DatabaseRow> findAllDatabasesByQuery(@Param("keyword") String keyword,
                                            @Param("environment") String environment,
                                            @Param("databaseType") String databaseType,
                                            @Param("lifecycleStatus") String lifecycleStatus,
                                            @Param("connectivityStatus") String connectivityStatus);

  @Insert("""
      INSERT INTO asset_database (
        database_name,
        database_type,
        environment,
        host_id,
        port,
        instance_name,
        credential_id,
        owner_name,
        lifecycle_status,
        connectivity_status,
        connection_username,
        connection_password,
        description,
        last_checked_at,
        created_at,
        updated_at
      )
      VALUES (
        #{databaseName},
        #{databaseType},
        #{environment},
        #{hostId},
        #{port},
        #{instanceName},
        #{credentialId},
        #{ownerName},
        #{lifecycleStatus},
        #{connectivityStatus},
        #{connectionUsername},
        #{connectionPassword},
        #{description},
        #{lastCheckedAt},
        #{createdAt},
        #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertDatabase(DatabaseEntity entity);

  @Update("""
      UPDATE asset_database
      SET database_name = #{databaseName},
          database_type = #{databaseType},
          environment = #{environment},
          host_id = #{hostId},
          port = #{port},
          instance_name = #{instanceName},
          credential_id = #{credentialId},
          owner_name = #{ownerName},
          lifecycle_status = #{lifecycleStatus},
          connectivity_status = #{connectivityStatus},
          connection_username = #{connectionUsername},
          connection_password = #{connectionPassword},
          description = #{description},
          last_checked_at = #{lastCheckedAt},
          updated_at = #{updatedAt}
      WHERE id = #{id}
      """)
  int updateDatabase(DatabaseEntity entity);

  @Update("""
      UPDATE asset_database
      SET connectivity_status = #{connectivityStatus},
          last_checked_at = #{lastCheckedAt},
          updated_at = #{lastCheckedAt}
      WHERE id = #{id}
      """)
  int updateConnectivitySnapshot(@Param("id") Long id,
                                 @Param("connectivityStatus") String connectivityStatus,
                                 @Param("lastCheckedAt") LocalDateTime lastCheckedAt);

  @Delete("""
      DELETE FROM asset_database
      WHERE id = #{id}
      """)
  int deleteById(@Param("id") Long id);

  @Select("""
      SELECT db.id,
             db.database_name AS databaseName,
             db.database_type AS databaseType,
             db.environment,
             db.host_id AS hostId,
             host.host_name AS hostName,
             host.ip_address AS hostIpAddress,
             db.port,
             db.instance_name AS instanceName,
             db.credential_id AS credentialId,
             credential.name AS credentialName,
             db.owner_name AS ownerName,
             db.lifecycle_status AS lifecycleStatus,
             db.connectivity_status AS connectivityStatus,
             db.connection_username AS connectionUsername,
             db.connection_password AS connectionPassword,
             db.description,
             db.last_checked_at AS lastCheckedAt,
             db.created_at AS createdAt,
             db.updated_at AS updatedAt
      FROM asset_database db
      LEFT JOIN asset_host host ON host.id = db.host_id
      LEFT JOIN asset_credential credential ON credential.id = db.credential_id
      WHERE db.id = #{id}
      """)
  DatabaseRow findById(@Param("id") Long id);

  @Select("""
      SELECT db.id,
             db.database_name AS databaseName,
             db.database_type AS databaseType,
             db.environment,
             db.host_id AS hostId,
             host.host_name AS hostName,
             host.ip_address AS hostIpAddress,
             db.port,
             db.instance_name AS instanceName,
             db.credential_id AS credentialId,
             credential.name AS credentialName,
             db.owner_name AS ownerName,
             db.lifecycle_status AS lifecycleStatus,
             db.connectivity_status AS connectivityStatus,
             db.connection_username AS connectionUsername,
             db.connection_password AS connectionPassword,
             db.description,
             db.last_checked_at AS lastCheckedAt,
             db.created_at AS createdAt,
             db.updated_at AS updatedAt
      FROM asset_database db
      LEFT JOIN asset_host host ON host.id = db.host_id
      LEFT JOIN asset_credential credential ON credential.id = db.credential_id
      WHERE db.environment = #{environment}
        AND db.host_id = #{hostId}
        AND db.port = #{port}
        AND db.database_name = #{databaseName}
      """)
  DatabaseRow findByUniqueKey(@Param("environment") String environment,
                              @Param("hostId") Long hostId,
                              @Param("port") Integer port,
                              @Param("databaseName") String databaseName);

  class DatabaseSummaryRow {
    private long managedDatabases;
    private long warningDatabases;
    private long onlineDatabases;

    public long getManagedDatabases() {
      return managedDatabases;
    }

    public void setManagedDatabases(long managedDatabases) {
      this.managedDatabases = managedDatabases;
    }

    public long getWarningDatabases() {
      return warningDatabases;
    }

    public void setWarningDatabases(long warningDatabases) {
      this.warningDatabases = warningDatabases;
    }

    public long getOnlineDatabases() {
      return onlineDatabases;
    }

    public void setOnlineDatabases(long onlineDatabases) {
      this.onlineDatabases = onlineDatabases;
    }
  }

  class DatabaseRow {
    private Long id;
    private String databaseName;
    private String databaseType;
    private String environment;
    private Long hostId;
    private String hostName;
    private String hostIpAddress;
    private Integer port;
    private String instanceName;
    private Long credentialId;
    private String credentialName;
    private String ownerName;
    private String lifecycleStatus;
    private String connectivityStatus;
    private String connectionUsername;
    private String connectionPassword;
    private String description;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
    }

    public String getDatabaseType() {
      return databaseType;
    }

    public void setDatabaseType(String databaseType) {
      this.databaseType = databaseType;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public Long getHostId() {
      return hostId;
    }

    public void setHostId(Long hostId) {
      this.hostId = hostId;
    }

    public String getHostName() {
      return hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    public String getHostIpAddress() {
      return hostIpAddress;
    }

    public void setHostIpAddress(String hostIpAddress) {
      this.hostIpAddress = hostIpAddress;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public String getInstanceName() {
      return instanceName;
    }

    public void setInstanceName(String instanceName) {
      this.instanceName = instanceName;
    }

    public Long getCredentialId() {
      return credentialId;
    }

    public void setCredentialId(Long credentialId) {
      this.credentialId = credentialId;
    }

    public String getCredentialName() {
      return credentialName;
    }

    public void setCredentialName(String credentialName) {
      this.credentialName = credentialName;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }

    public String getLifecycleStatus() {
      return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
      this.lifecycleStatus = lifecycleStatus;
    }

    public String getConnectivityStatus() {
      return connectivityStatus;
    }

    public void setConnectivityStatus(String connectivityStatus) {
      this.connectivityStatus = connectivityStatus;
    }

    public String getConnectionUsername() {
      return connectionUsername;
    }

    public void setConnectionUsername(String connectionUsername) {
      this.connectionUsername = connectionUsername;
    }

    public String getConnectionPassword() {
      return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
      this.connectionPassword = connectionPassword;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public LocalDateTime getLastCheckedAt() {
      return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
      this.lastCheckedAt = lastCheckedAt;
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

  class DatabaseEntity {
    private Long id;
    private String databaseName;
    private String databaseType;
    private String environment;
    private Long hostId;
    private Integer port;
    private String instanceName;
    private Long credentialId;
    private String ownerName;
    private String lifecycleStatus;
    private String connectivityStatus;
    private String connectionUsername;
    private String connectionPassword;
    private String description;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
    }

    public String getDatabaseType() {
      return databaseType;
    }

    public void setDatabaseType(String databaseType) {
      this.databaseType = databaseType;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public Long getHostId() {
      return hostId;
    }

    public void setHostId(Long hostId) {
      this.hostId = hostId;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public String getInstanceName() {
      return instanceName;
    }

    public void setInstanceName(String instanceName) {
      this.instanceName = instanceName;
    }

    public Long getCredentialId() {
      return credentialId;
    }

    public void setCredentialId(Long credentialId) {
      this.credentialId = credentialId;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }

    public String getLifecycleStatus() {
      return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
      this.lifecycleStatus = lifecycleStatus;
    }

    public String getConnectivityStatus() {
      return connectivityStatus;
    }

    public void setConnectivityStatus(String connectivityStatus) {
      this.connectivityStatus = connectivityStatus;
    }

    public String getConnectionUsername() {
      return connectionUsername;
    }

    public void setConnectionUsername(String connectionUsername) {
      this.connectionUsername = connectionUsername;
    }

    public String getConnectionPassword() {
      return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
      this.connectionPassword = connectionPassword;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public LocalDateTime getLastCheckedAt() {
      return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
      this.lastCheckedAt = lastCheckedAt;
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
