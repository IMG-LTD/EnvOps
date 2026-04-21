package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;

@Component
public class SqlServerConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "sqlserver";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    return "jdbc:sqlserver://%s:%d;databaseName=%s;loginTimeout=5;encrypt=false;trustServerCertificate=true"
        .formatted(target.hostIpAddress(), target.port(), target.databaseName());
  }
}
