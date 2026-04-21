package com.img.envops.modules.asset.application.connectivity;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DatabaseConnectionFactory {
  private final Map<String, DatabaseConnectivityChecker> checkers;

  public DatabaseConnectionFactory(List<DatabaseConnectivityChecker> checkers) {
    this.checkers = checkers.stream().collect(Collectors.toUnmodifiableMap(
        checker -> checker.databaseType().toLowerCase(Locale.ROOT),
        Function.identity()));
  }

  public DatabaseConnectivityChecker getChecker(String databaseType) {
    String normalizedDatabaseType = databaseType == null ? null : databaseType.toLowerCase(Locale.ROOT);
    DatabaseConnectivityChecker checker = checkers.get(normalizedDatabaseType);
    if (checker == null) {
      throw new IllegalArgumentException("Unsupported databaseType for connectivity check: " + databaseType);
    }
    return checker;
  }
}
