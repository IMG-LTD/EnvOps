package com.img.envops.modules.monitor.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface MonitorHostFactMapper {

  @Select("""
      SELECT id,
             host_id AS hostId,
             host_name AS hostName,
             os_name AS osName,
             kernel_version AS kernelVersion,
             cpu_cores AS cpuCores,
             memory_mb AS memoryMb,
             agent_version AS agentVersion,
             collected_at AS collectedAt
      FROM monitor_host_fact
      WHERE host_id = #{hostId}
      ORDER BY collected_at DESC, id DESC
      LIMIT 1
      """)
  HostFactRow findLatestByHostId(@Param("hostId") Long hostId);

  class HostFactRow {
    private Long id;
    private Long hostId;
    private String hostName;
    private String osName;
    private String kernelVersion;
    private Integer cpuCores;
    private Integer memoryMb;
    private String agentVersion;
    private LocalDateTime collectedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
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

    public String getOsName() {
      return osName;
    }

    public void setOsName(String osName) {
      this.osName = osName;
    }

    public String getKernelVersion() {
      return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
      this.kernelVersion = kernelVersion;
    }

    public Integer getCpuCores() {
      return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
      this.cpuCores = cpuCores;
    }

    public Integer getMemoryMb() {
      return memoryMb;
    }

    public void setMemoryMb(Integer memoryMb) {
      this.memoryMb = memoryMb;
    }

    public String getAgentVersion() {
      return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
      this.agentVersion = agentVersion;
    }

    public LocalDateTime getCollectedAt() {
      return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
      this.collectedAt = collectedAt;
    }
  }
}
