package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;

@Component
public class PostgreSqlConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "postgresql";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    return "jdbc:postgresql://%s:%d/%s?loginTimeout=5&connectTimeout=5&socketTimeout=5&sslmode=disable"
        .formatted(target.hostIpAddress(), target.port(), target.databaseName());
  }
}
