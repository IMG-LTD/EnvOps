package com.img.envops.modules.asset.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssetHostMapper {

  @Select("""
      SELECT COUNT(*)
      FROM asset_host
      """)
  long countHosts();

  @Select("""
      SELECT COUNT(*) AS managedHosts,
             COALESCE(SUM(CASE WHEN status = 'online' THEN 1 ELSE 0 END), 0) AS onlineHosts,
             COALESCE(SUM(CASE WHEN status = 'warning' THEN 1 ELSE 0 END), 0) AS warningHosts
      FROM asset_host
      """)
  HostSummaryRow summarizeHosts();

  @Select("""
      SELECT id,
             host_name AS hostName,
             ip_address AS ipAddress,
             environment,
             cluster_name AS clusterName,
             owner_name AS ownerName,
             status,
             last_heartbeat AS lastHeartbeat
      FROM asset_host
      ORDER BY id
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<HostRow> findHosts(@Param("limit") int limit, @Param("offset") int offset);

  @Insert("""
      INSERT INTO asset_host (host_name, ip_address, environment, cluster_name, owner_name, status, last_heartbeat)
      VALUES (#{hostName}, #{ipAddress}, #{environment}, #{clusterName}, #{ownerName}, #{status}, #{lastHeartbeat})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertHost(HostEntity host);

  @Select("""
      SELECT id,
             host_name AS hostName,
             ip_address AS ipAddress,
             environment,
             cluster_name AS clusterName,
             owner_name AS ownerName,
             status,
             last_heartbeat AS lastHeartbeat
      FROM asset_host
      WHERE id = #{id}
      """)
  HostRow findById(@Param("id") Long id);

  class HostSummaryRow {
    private long managedHosts;
    private long onlineHosts;
    private long warningHosts;

    public long getManagedHosts() {
      return managedHosts;
    }

    public void setManagedHosts(long managedHosts) {
      this.managedHosts = managedHosts;
    }

    public long getOnlineHosts() {
      return onlineHosts;
    }

    public void setOnlineHosts(long onlineHosts) {
      this.onlineHosts = onlineHosts;
    }

    public long getWarningHosts() {
      return warningHosts;
    }

    public void setWarningHosts(long warningHosts) {
      this.warningHosts = warningHosts;
    }
  }

  class HostRow {
    private Long id;
    private String hostName;
    private String ipAddress;
    private String environment;
    private String clusterName;
    private String ownerName;
    private String status;
    private LocalDateTime lastHeartbeat;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getHostName() {
      return hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    public String getIpAddress() {
      return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public String getClusterName() {
      return clusterName;
    }

    public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
      return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
      this.lastHeartbeat = lastHeartbeat;
    }
  }

  class HostEntity {
    private Long id;
    private String hostName;
    private String ipAddress;
    private String environment;
    private String clusterName;
    private String ownerName;
    private String status;
    private LocalDateTime lastHeartbeat;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getHostName() {
      return hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    public String getIpAddress() {
      return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public String getClusterName() {
      return clusterName;
    }

    public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public void setOwnerName(String ownerName) {
      this.ownerName = ownerName;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
      return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
      this.lastHeartbeat = lastHeartbeat;
    }
  }
}
