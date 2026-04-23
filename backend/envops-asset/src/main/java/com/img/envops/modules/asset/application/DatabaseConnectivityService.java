package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.application.connectivity.DatabaseConnectionFactory;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseConnectivityService {
  private static final String TASK_TYPE = "database_connectivity";
  private static final String MODULE_NAME = "asset";
  private static final String SOURCE_ROUTE = "/asset/database";
  private static final String SINGLE_TASK_NAME = "检测数据库连通性";
  private static final String BATCH_TASK_NAME = "批量数据库连通性检测";
  private static final String FAILURE_SUMMARY = "数据库连通性检测存在失败项";

  private final AssetDatabaseMapper assetDatabaseMapper;
  private final DatabaseConnectionFactory databaseConnectionFactory;
  private final DatabaseConnectionSecretProtector databaseConnectionSecretProtector;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory;

  public DatabaseConnectivityService(AssetDatabaseMapper assetDatabaseMapper,
                                     DatabaseConnectionFactory databaseConnectionFactory,
                                     DatabaseConnectionSecretProtector databaseConnectionSecretProtector,
                                     UnifiedTaskRecorder unifiedTaskRecorder,
                                     UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory) {
    this.assetDatabaseMapper = assetDatabaseMapper;
    this.databaseConnectionFactory = databaseConnectionFactory;
    this.databaseConnectionSecretProtector = databaseConnectionSecretProtector;
    this.unifiedTaskRecorder = unifiedTaskRecorder;
    this.unifiedTaskDetailPreviewFactory = unifiedTaskDetailPreviewFactory;
  }

  public DatabaseConnectivityReport checkOneDatabase(Long id) {
    if (id == null || id < 1) {
      throw new IllegalArgumentException("id is required");
    }

    List<AssetDatabaseMapper.DatabaseRow> rows = uniqueRows(assetDatabaseMapper.findDatabasesByIds(List.of(id)));
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("database not found: " + id);
    }

    AssetDatabaseMapper.DatabaseRow row = rows.get(0);
    return runWithConnectivityTask(rows, false, row.getId());
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

    List<AssetDatabaseMapper.DatabaseRow> rows = uniqueRows(assetDatabaseMapper.findDatabasesByIds(normalizedIds));
    if (rows.isEmpty()) {
      return new DatabaseConnectivityReport(new DatabaseConnectivitySummary(0, 0, 0, 0), List.of());
    }
    return runWithConnectivityTask(rows, true, null);
  }

  public DatabaseConnectivityReport checkDatabasesByQuery(String keyword,
                                                          String environment,
                                                          String databaseType,
                                                          String lifecycleStatus,
                                                          String connectivityStatus) {
    List<AssetDatabaseMapper.DatabaseRow> rows = uniqueRows(assetDatabaseMapper.findAllDatabasesByQuery(
        keyword,
        environment,
        databaseType,
        lifecycleStatus,
        connectivityStatus));
    if (rows.isEmpty()) {
      return new DatabaseConnectivityReport(new DatabaseConnectivitySummary(0, 0, 0, 0), List.of());
    }
    return runWithConnectivityTask(rows, true, null);
  }

  private DatabaseConnectivityReport run(List<AssetDatabaseMapper.DatabaseRow> rows) {
    List<DatabaseConnectivityItem> results = new ArrayList<>();
    for (AssetDatabaseMapper.DatabaseRow row : rows) {
      try {
        results.add(checkRow(row));
      } catch (RuntimeException exception) {
        results.add(DatabaseConnectivityItem.failed(
            row.getId(),
            row.getDatabaseName(),
            row.getDatabaseType(),
            row.getEnvironment(),
            FAILURE_SUMMARY));
        throw new DatabaseConnectivityRunException(summarizeResults(results), exception);
      }
    }
    return summarizeResults(results);
  }

  private DatabaseConnectivityReport summarizeResults(List<DatabaseConnectivityItem> results) {
    long success = results.stream().filter(item -> item.status().equals("success")).count();
    long failed = results.stream().filter(item -> item.status().equals("failed")).count();
    long skipped = results.stream().filter(item -> item.status().equals("skipped")).count();

    return new DatabaseConnectivityReport(
        new DatabaseConnectivitySummary(results.size(), success, failed, skipped),
        List.copyOf(results));
  }

  private DatabaseConnectivityReport runWithConnectivityTask(List<AssetDatabaseMapper.DatabaseRow> rows,
                                                             boolean batch,
                                                             Long sourceId) {
    Long unifiedTaskId = startConnectivityTask(batch, sourceId, rows.size());
    DatabaseConnectivityReport report;
    try {
      report = run(rows);
    } catch (DatabaseConnectivityRunException exception) {
      RuntimeException cause = exception.cause();
      try {
        failConnectivityTask(unifiedTaskId, batch, exception.report());
      } catch (RuntimeException updateException) {
        cause.addSuppressed(updateException);
      }
      throw cause;
    } catch (RuntimeException exception) {
      try {
        failConnectivityTask(unifiedTaskId, batch, failedReport(rows.size()));
      } catch (RuntimeException updateException) {
        exception.addSuppressed(updateException);
      }
      throw exception;
    }

    finishConnectivityTask(unifiedTaskId, batch, report);
    return report;
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

  private Long startConnectivityTask(boolean batch, Long sourceId, long total) {
    String taskName = batch ? BATCH_TASK_NAME : SINGLE_TASK_NAME;
    String summary = batch ? "批量数据库连通性检测执行中" : "数据库连通性检测执行中";
    return unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
        TASK_TYPE,
        taskName,
        "running",
        null,
        LocalDateTime.now(),
        summary,
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDatabaseConnectivityPreview(
            batch,
            summary,
            total,
            0,
            0,
            0,
            SOURCE_ROUTE,
            null)),
        sourceId,
        SOURCE_ROUTE,
        MODULE_NAME,
        null));
  }

  private void finishConnectivityTask(Long unifiedTaskId,
                                      boolean batch,
                                      DatabaseConnectivityReport report) {
    String summary = String.format(
        "%s %d 条，成功 %d，失败 %d，跳过 %d",
        batch ? "批量检测" : "检测",
        report.summary().total(),
        report.summary().success(),
        report.summary().failed(),
        report.summary().skipped());
    String status = report.summary().failed() > 0 ? "failed" : "success";
    String errorSummary = report.summary().failed() > 0 ? FAILURE_SUMMARY : null;
    updateConnectivityTask(unifiedTaskId, batch, summary, report.summary().total(), report.summary().success(), report.summary().failed(), report.summary().skipped(), errorSummary, status);
  }

  private void failConnectivityTask(Long unifiedTaskId,
                                    boolean batch,
                                    DatabaseConnectivityReport report) {
    String summary = String.format(
        "%s %d 条，成功 %d，失败 %d，跳过 %d",
        batch ? "批量检测" : "检测",
        report.summary().total(),
        report.summary().success(),
        report.summary().failed(),
        report.summary().skipped());
    updateConnectivityTask(
        unifiedTaskId,
        batch,
        summary,
        report.summary().total(),
        report.summary().success(),
        report.summary().failed(),
        report.summary().skipped(),
        FAILURE_SUMMARY,
        "failed");
  }

  private DatabaseConnectivityReport failedReport(long total) {
    return new DatabaseConnectivityReport(
        new DatabaseConnectivitySummary(total, 0, total, 0),
        List.of());
  }

  private void updateConnectivityTask(Long unifiedTaskId,
                                      boolean batch,
                                      String summary,
                                      long total,
                                      long success,
                                      long failed,
                                      long skipped,
                                      String errorSummary,
                                      String status) {
    DatabaseConnectivityReport report = new DatabaseConnectivityReport(
        new DatabaseConnectivitySummary(total, success, failed, skipped),
        List.of());
    unifiedTaskRecorder.update(new UnifiedTaskRecorder.UpdateCommand(
        unifiedTaskId,
        status,
        LocalDateTime.now(),
        summary,
        unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDatabaseConnectivityPreview(
            batch,
            summary,
            total,
            success,
            failed,
            skipped,
            SOURCE_ROUTE,
            errorSummary)),
        errorSummary));
    unifiedTaskRecorder.updateTrackingSnapshot(new UnifiedTaskRecorder.TrackingSnapshotCommand(
        unifiedTaskId,
        buildConnectivityTimeline(status, report),
        buildConnectivityLogSummary(report, errorSummary),
        SOURCE_ROUTE));
  }

  private String buildConnectivityTimeline(String status, DatabaseConnectivityReport report) {
    return unifiedTaskDetailPreviewFactory.toJsonArray(List.of(
        Map.of("label", "检测开始", "status", "success", "description", "数据库连通性检测已开始"),
        Map.of("label", "检测完成", "status", status, "description", buildConnectivitySummary(report))));
  }

  private String buildConnectivityLogSummary(DatabaseConnectivityReport report, String errorSummary) {
    String summary = buildConnectivitySummary(report);
    return errorSummary == null || errorSummary.isBlank() ? summary : summary + "；失败摘要：" + errorSummary;
  }

  private String buildConnectivitySummary(DatabaseConnectivityReport report) {
    return String.format(
        "检测 %d 条，成功 %d，失败 %d，跳过 %d",
        report.summary().total(),
        report.summary().success(),
        report.summary().failed(),
        report.summary().skipped());
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

    static DatabaseConnectivityItem failed(Long databaseId,
                                           String databaseName,
                                           String databaseType,
                                           String environment,
                                           String message) {
      return new DatabaseConnectivityItem(databaseId, databaseName, databaseType, environment, "failed", message, "unknown", null);
    }
  }

  private static final class DatabaseConnectivityRunException extends RuntimeException {
    private final DatabaseConnectivityReport report;

    private DatabaseConnectivityRunException(DatabaseConnectivityReport report, RuntimeException cause) {
      super(cause);
      this.report = report;
    }

    private DatabaseConnectivityReport report() {
      return report;
    }

    private RuntimeException cause() {
      return (RuntimeException) getCause();
    }
  }
}
