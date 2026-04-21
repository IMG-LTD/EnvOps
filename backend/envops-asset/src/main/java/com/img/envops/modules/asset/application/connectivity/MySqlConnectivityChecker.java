package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;

@Component
public class MySqlConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "mysql";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    return "jdbc:mysql://%s:%d/%s?connectTimeout=5000&socketTimeout=5000&sslMode=DISABLED"
        .formatted(target.hostIpAddress(), target.port(), target.databaseName());
  }
}
