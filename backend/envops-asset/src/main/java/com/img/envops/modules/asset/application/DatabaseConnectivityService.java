package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.application.connectivity.DatabaseConnectionFactory;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseConnectivityService {
  private final AssetDatabaseMapper assetDatabaseMapper;
  private final DatabaseConnectionFactory databaseConnectionFactory;
  private final DatabaseConnectionSecretProtector databaseConnectionSecretProtector;

  public DatabaseConnectivityService(AssetDatabaseMapper assetDatabaseMapper,
                                     DatabaseConnectionFactory databaseConnectionFactory,
                                     DatabaseConnectionSecretProtector databaseConnectionSecretProtector) {
    this.assetDatabaseMapper = assetDatabaseMapper;
    this.databaseConnectionFactory = databaseConnectionFactory;
    this.databaseConnectionSecretProtector = databaseConnectionSecretProtector;
  }

  public DatabaseConnectivityReport checkOneDatabase(Long id) {
    if (id == null || id < 1) {
      throw new IllegalArgumentException("id is required");
    }
    return run(assetDatabaseMapper.findDatabasesByIds(List.of(id)));
  }

  public DatabaseConnectivityReport checkSelectedDatabases(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("ids are required");
    }

    List<Long> normalizedIds = ids.stream()
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (normalizedIds.isEmpty()) {
      throw new IllegalArgumentException("ids are required");
    }

    return run(assetDatabaseMapper.findDatabasesByIds(normalizedIds));
  }

  public DatabaseConnectivityReport checkDatabasesByQuery(String keyword,
                                                          String environment,
                                                          String databaseType,
                                                          String lifecycleStatus,
                                                          String connectivityStatus) {
    return run(assetDatabaseMapper.findAllDatabasesByQuery(
        keyword,
        environment,
        databaseType,
        lifecycleStatus,
        connectivityStatus));
  }

  private DatabaseConnectivityReport run(List<AssetDatabaseMapper.DatabaseRow> rows) {
    List<DatabaseConnectivityItem> results = new ArrayList<>();
    for (AssetDatabaseMapper.DatabaseRow row : uniqueRows(rows)) {
      results.add(checkRow(row));
    }

    long success = results.stream().filter(item -> item.status().equals("success")).count();
    long failed = results.stream().filter(item -> item.status().equals("failed")).count();
    long skipped = results.stream().filter(item -> item.status().equals("skipped")).count();

    return new DatabaseConnectivityReport(
        new DatabaseConnectivitySummary(results.size(), success, failed, skipped),
        results);
  }

  private List<AssetDatabaseMapper.DatabaseRow> uniqueRows(List<AssetDatabaseMapper.DatabaseRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }

    Map<Long, AssetDatabaseMapper.DatabaseRow> orderedRows = new LinkedHashMap<>();
    for (AssetDatabaseMapper.DatabaseRow row : rows) {
      if (row != null && row.getId() != null) {
        orderedRows.putIfAbsent(row.getId(), row);
      }
    }
    return List.copyOf(orderedRows.values());
  }

  private DatabaseConnectivityItem checkRow(AssetDatabaseMapper.DatabaseRow row) {
    if (row.getConnectionUsername() == null || row.getConnectionPassword() == null) {
      return DatabaseConnectivityItem.skipped(
          row.getId(),
          row.getDatabaseName(),
          row.getDatabaseType(),
          row.getEnvironment(),
          "缺少连接用户名或密码");
    }

    LocalDateTime checkedAt = LocalDateTime.now();
    String rawPassword = databaseConnectionSecretProtector.reveal(row.getConnectionPassword());
    DatabaseConnectivityTarget target = new DatabaseConnectivityTarget(
        row.getId(),
        row.getDatabaseName(),
        row.getDatabaseType(),
        row.getEnvironment(),
        row.getHostIpAddress(),
        row.getPort(),
        row.getInstanceName(),
        row.getConnectionUsername(),
        rawPassword);
    DatabaseConnectivityProbeResult probeResult = databaseConnectionFactory.getChecker(row.getDatabaseType()).check(target);
    String snapshot = probeResult.success() ? "online" : "offline";
    assetDatabaseMapper.updateConnectivitySnapshot(row.getId(), snapshot, checkedAt);
    return new DatabaseConnectivityItem(
        row.getId(),
        row.getDatabaseName(),
        row.getDatabaseType(),
        row.getEnvironment(),
        probeResult.success() ? "success" : "failed",
        probeResult.message(),
        snapshot,
        checkedAt);
  }

  public record DatabaseConnectivityReport(DatabaseConnectivitySummary summary, List<DatabaseConnectivityItem> results) {
  }

  public record DatabaseConnectivitySummary(long total, long success, long failed, long skipped) {
  }

  public record DatabaseConnectivityItem(Long databaseId,
                                         String databaseName,
                                         String databaseType,
                                         String environment,
                                         String status,
                                         String message,
                                         String connectivityStatus,
                                         LocalDateTime checkedAt) {
    static DatabaseConnectivityItem skipped(Long databaseId,
                                            String databaseName,
                                            String databaseType,
                                            String environment,
                                            String message) {
      return new DatabaseConnectivityItem(databaseId, databaseName, databaseType, environment, "skipped", message, "unknown", null);
    }
  }
}
