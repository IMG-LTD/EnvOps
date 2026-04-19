package com.img.envops.modules.traffic.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TrafficPolicyMapper {

  @Select("""
      SELECT id,
             app,
             strategy,
             scope,
             traffic_ratio AS trafficRatio,
             owner,
             status,
             plugin_type AS pluginType,
             rollback_token AS rollbackToken,
             updated_at AS updatedAt
      FROM traffic_policy
      ORDER BY id ASC
      """)
  List<TrafficPolicyRow> findAll();

  @Select("""
      SELECT id,
             app,
             strategy,
             scope,
             traffic_ratio AS trafficRatio,
             owner,
             status,
             plugin_type AS pluginType,
             rollback_token AS rollbackToken,
             updated_at AS updatedAt
      FROM traffic_policy
      WHERE id = #{id}
      """)
  TrafficPolicyRow findById(@Param("id") Long id);

  @Update("""
      UPDATE traffic_policy
      SET status = #{status},
          rollback_token = #{rollbackToken},
          updated_at = #{updatedAt}
      WHERE id = #{id}
      """)
  int updatePolicyState(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("rollbackToken") String rollbackToken,
                        @Param("updatedAt") LocalDateTime updatedAt);

  class TrafficPolicyRow {
    private Long id;
    private String app;
    private String strategy;
    private String scope;
    private String trafficRatio;
    private String owner;
    private String status;
    private String pluginType;
    private String rollbackToken;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getApp() {
      return app;
    }

    public void setApp(String app) {
      this.app = app;
    }

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }

    public String getScope() {
      return scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }

    public String getTrafficRatio() {
      return trafficRatio;
    }

    public void setTrafficRatio(String trafficRatio) {
      this.trafficRatio = trafficRatio;
    }

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getPluginType() {
      return pluginType;
    }

    public void setPluginType(String pluginType) {
      this.pluginType = pluginType;
    }

    public String getRollbackToken() {
      return rollbackToken;
    }

    public void setRollbackToken(String rollbackToken) {
      this.rollbackToken = rollbackToken;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

}
