package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Component
public class RedisConnectivityChecker implements DatabaseConnectivityChecker {
  @Override
  public String databaseType() {
    return "redis";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
        .user(target.connectionUsername())
        .password(target.connectionPassword())
        .timeoutMillis(5000)
        .build();

    try (JedisPooled jedis = new JedisPooled(new HostAndPort(target.hostIpAddress(), target.port()), config)) {
      jedis.ping();
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (RuntimeException exception) {
      return DatabaseConnectivityProbeResult.failure("认证失败");
    }
  }
}
