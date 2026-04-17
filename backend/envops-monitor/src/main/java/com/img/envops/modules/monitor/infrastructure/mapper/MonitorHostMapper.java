package com.img.envops.modules.monitor.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorHostMapper {

  @Select("""
      SELECT id,
             host_name AS hostName,
             ip_address AS ipAddress
      FROM asset_host
      WHERE id = #{id}
      """)
  HostRow findById(@Param("id") Long id);

  class HostRow {
    private Long id;
    private String hostName;
    private String ipAddress;

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
  }
}
