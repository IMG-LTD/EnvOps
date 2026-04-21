package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;

@Component
public class OracleConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "oracle";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    String serviceName = target.instanceName() == null || target.instanceName().isBlank()
        ? target.databaseName()
        : target.instanceName();
    return "jdbc:oracle:thin:@//%s:%d/%s".formatted(target.hostIpAddress(), target.port(), serviceName);
  }

  @Override
  protected String validationSql() {
    return "SELECT 1 FROM DUAL";
  }
}
