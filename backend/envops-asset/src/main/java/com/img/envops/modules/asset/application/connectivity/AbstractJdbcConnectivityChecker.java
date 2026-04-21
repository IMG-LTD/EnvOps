package com.img.envops.modules.asset.application.connectivity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public abstract class AbstractJdbcConnectivityChecker implements DatabaseConnectivityChecker {
  protected abstract String jdbcUrl(DatabaseConnectivityTarget target);

  protected String validationSql() {
    return "SELECT 1";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    try (Connection connection = DriverManager.getConnection(
        jdbcUrl(target),
        target.connectionUsername(),
        target.connectionPassword());
         Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(5);
      statement.execute(validationSql());
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (SQLException exception) {
      return DatabaseConnectivityProbeResult.failure(resolveMessage(exception));
    }
  }

  protected String resolveMessage(SQLException exception) {
    String message = exception.getMessage();
    if (message == null) {
      return "数据库拒绝连接";
    }

    String normalizedMessage = message.toLowerCase(Locale.ROOT);
    if (normalizedMessage.contains("timeout") || normalizedMessage.contains("timed out")) {
      return "连接超时";
    }
    if (normalizedMessage.contains("login")
        || normalizedMessage.contains("auth")
        || normalizedMessage.contains("password")
        || normalizedMessage.contains("access denied")
        || normalizedMessage.contains("28000")
        || normalizedMessage.contains("ora-01017")) {
      return "认证失败";
    }
    return "数据库拒绝连接";
  }
}
