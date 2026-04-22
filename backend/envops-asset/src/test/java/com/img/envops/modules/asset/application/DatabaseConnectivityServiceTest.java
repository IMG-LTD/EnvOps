package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.application.connectivity.DatabaseConnectionFactory;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityChecker;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseConnectivityServiceTest {
  @Test
  void checkOneDatabaseCreatesAndUpdatesOneUnifiedTaskWithDatabaseSourceId() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql");

    when(mapper.findDatabasesByIds(List.of(11L))).thenReturn(List.of(mysql));
    when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9001L);

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkOneDatabase(11L);

    assertThat(report.summary().total()).isEqualTo(1);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(0);
    assertThat(report.summary().skipped()).isEqualTo(0);

    ArgumentCaptor<UnifiedTaskRecorder.CreateCommand> startCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.CreateCommand.class);
    ArgumentCaptor<UnifiedTaskRecorder.UpdateCommand> updateCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.UpdateCommand.class);
    verify(unifiedTaskRecorder, times(1)).start(startCaptor.capture());
    verify(unifiedTaskRecorder, times(1)).update(updateCaptor.capture());

    UnifiedTaskRecorder.CreateCommand started = startCaptor.getValue();
    assertThat(started.taskType()).isEqualTo("database_connectivity");
    assertThat(started.taskName()).isEqualTo("检测数据库连通性");
    assertThat(started.status()).isEqualTo("running");
    assertThat(started.sourceId()).isEqualTo(11L);
    assertThat(started.sourceRoute()).isEqualTo("/asset/database");
    assertThat(started.moduleName()).isEqualTo("asset");
    assertThat(started.errorSummary()).isNull();
    assertThat(started.detailPreview())
        .contains("\"mode\":\"single\"")
        .contains("\"sourceRoute\":\"/asset/database\"");

    UnifiedTaskRecorder.UpdateCommand updated = updateCaptor.getValue();
    assertThat(updated.id()).isEqualTo(9001L);
    assertThat(updated.status()).isEqualTo("success");
    assertThat(updated.summary()).isEqualTo("检测 1 条，成功 1，失败 0，跳过 0");
    assertThat(updated.errorSummary()).isNull();
    assertThat(updated.detailPreview())
        .contains("\"mode\":\"single\"")
        .contains("\"summary\":\"检测 1 条，成功 1，失败 0，跳过 0\"")
        .contains("\"errorSummary\":null");

    verify(mapper).findDatabasesByIds(List.of(11L));
    verify(mapper).updateConnectivitySnapshot(eq(11L), eq("online"), any(LocalDateTime.class));
  }

  @Test
  void checkSelectedDatabasesCreatesAndUpdatesOnlyOneFailedBatchUnifiedTask() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectivityChecker mysqlChecker = new StubChecker("mysql", true, "connected");
    DatabaseConnectivityChecker postgresqlChecker = new StubChecker("postgresql", false, "认证失败");
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(mysqlChecker, postgresqlChecker));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql");
    AssetDatabaseMapper.DatabaseRow postgres = databaseRow(12L, "traffic_gate", "postgresql", "10.20.1.12", 5432, "traffic_app", "sealed:pg");
    AssetDatabaseMapper.DatabaseRow missing = databaseRow(13L, "session_hub", "redis", "10.20.1.13", 6379, null, null);

    when(mapper.findDatabasesByIds(List.of(11L, 12L, 13L))).thenReturn(List.of(mysql, postgres, missing));
    when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");
    when(secretProtector.reveal("sealed:pg")).thenReturn("Traffic@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9002L);

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkSelectedDatabases(List.of(11L, 12L, 13L));

    assertThat(report.summary().total()).isEqualTo(3);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(1);
    assertThat(report.summary().skipped()).isEqualTo(1);
    assertThat(report.results()).extracting(DatabaseConnectivityService.DatabaseConnectivityItem::message)
        .contains("connected", "认证失败", "缺少连接用户名或密码");

    ArgumentCaptor<UnifiedTaskRecorder.CreateCommand> startCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.CreateCommand.class);
    ArgumentCaptor<UnifiedTaskRecorder.UpdateCommand> updateCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.UpdateCommand.class);
    verify(unifiedTaskRecorder, times(1)).start(startCaptor.capture());
    verify(unifiedTaskRecorder, times(1)).update(updateCaptor.capture());

    UnifiedTaskRecorder.CreateCommand started = startCaptor.getValue();
    assertThat(started.taskType()).isEqualTo("database_connectivity");
    assertThat(started.taskName()).isEqualTo("批量数据库连通性检测");
    assertThat(started.status()).isEqualTo("running");
    assertThat(started.sourceId()).isNull();
    assertThat(started.sourceRoute()).isEqualTo("/asset/database");
    assertThat(started.moduleName()).isEqualTo("asset");
    assertThat(started.errorSummary()).isNull();
    assertThat(started.detailPreview())
        .contains("\"mode\":\"batch\"")
        .contains("\"sourceRoute\":\"/asset/database\"");

    UnifiedTaskRecorder.UpdateCommand updated = updateCaptor.getValue();
    assertThat(updated.id()).isEqualTo(9002L);
    assertThat(updated.status()).isEqualTo("failed");
    assertThat(updated.summary()).isEqualTo("批量检测 3 条，成功 1，失败 1，跳过 1");
    assertThat(updated.errorSummary()).isEqualTo("数据库连通性检测存在失败项");
    assertThat(updated.detailPreview())
        .contains("\"mode\":\"batch\"")
        .contains("\"summary\":\"批量检测 3 条，成功 1，失败 1，跳过 1\"")
        .contains("\"errorSummary\":\"数据库连通性检测存在失败项\"");

    verify(mapper).updateConnectivitySnapshot(eq(11L), eq("online"), any(LocalDateTime.class));
    verify(mapper).updateConnectivitySnapshot(eq(12L), eq("offline"), any(LocalDateTime.class));
  }

  @Test
  void checkDatabasesByQueryCreatesAndUpdatesOneBatchUnifiedTask() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(21L, "billing_prod", "mysql", "10.20.1.21", 3306, "billing_app", "sealed:billing");

    when(mapper.findAllDatabasesByQuery("billing", "production", "mysql", "managed", "warning"))
        .thenReturn(List.of(mysql, mysql));
    when(secretProtector.reveal("sealed:billing")).thenReturn("Billing@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9003L);

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkDatabasesByQuery(
        "billing",
        "production",
        "mysql",
        "managed",
        "warning");

    assertThat(report.summary().total()).isEqualTo(1);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(0);
    assertThat(report.summary().skipped()).isEqualTo(0);

    ArgumentCaptor<UnifiedTaskRecorder.CreateCommand> startCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.CreateCommand.class);
    ArgumentCaptor<UnifiedTaskRecorder.UpdateCommand> updateCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.UpdateCommand.class);
    verify(unifiedTaskRecorder, times(1)).start(startCaptor.capture());
    verify(unifiedTaskRecorder, times(1)).update(updateCaptor.capture());

    UnifiedTaskRecorder.CreateCommand started = startCaptor.getValue();
    assertThat(started.taskType()).isEqualTo("database_connectivity");
    assertThat(started.taskName()).isEqualTo("批量数据库连通性检测");
    assertThat(started.status()).isEqualTo("running");
    assertThat(started.sourceId()).isNull();
    assertThat(started.sourceRoute()).isEqualTo("/asset/database");

    UnifiedTaskRecorder.UpdateCommand updated = updateCaptor.getValue();
    assertThat(updated.id()).isEqualTo(9003L);
    assertThat(updated.status()).isEqualTo("success");
    assertThat(updated.summary()).isEqualTo("批量检测 1 条，成功 1，失败 0，跳过 0");
    assertThat(updated.errorSummary()).isNull();
    assertThat(updated.detailPreview())
        .contains("\"mode\":\"batch\"")
        .contains("\"summary\":\"批量检测 1 条，成功 1，失败 0，跳过 0\"")
        .contains("\"errorSummary\":null");

    verify(mapper).findAllDatabasesByQuery("billing", "production", "mysql", "managed", "warning");
    verify(mapper).updateConnectivitySnapshot(eq(21L), eq("online"), any(LocalDateTime.class));
  }

  @Test
  void checkDatabasesByQuerySkipsUnifiedTaskWhenNoDatabaseMatches() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    when(mapper.findAllDatabasesByQuery("none", "production", "mysql", "managed", "warning"))
        .thenReturn(List.of());

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkDatabasesByQuery(
        "none",
        "production",
        "mysql",
        "managed",
        "warning");

    assertThat(report.summary().total()).isEqualTo(0);
    assertThat(report.results()).isEmpty();
    verify(unifiedTaskRecorder, times(0)).start(any());
    verify(unifiedTaskRecorder, times(0)).update(any());
  }

  @Test
  void checkOneDatabaseMarksStartedUnifiedTaskFailedWhenLaterRuntimeWorkThrows() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new ThrowingChecker("mysql")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(31L, "audit_prod", "mysql", "10.20.1.31", 3306, "audit_app", "sealed:audit");

    when(mapper.findDatabasesByIds(List.of(31L))).thenReturn(List.of(mysql));
    when(secretProtector.reveal("sealed:audit")).thenReturn("Audit@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9004L);

    assertThatThrownBy(() -> service.checkOneDatabase(31L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("simulated runtime failure");

    ArgumentCaptor<UnifiedTaskRecorder.CreateCommand> startCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.CreateCommand.class);
    ArgumentCaptor<UnifiedTaskRecorder.UpdateCommand> updateCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.UpdateCommand.class);
    verify(unifiedTaskRecorder, times(1)).start(startCaptor.capture());
    verify(unifiedTaskRecorder, times(1)).update(updateCaptor.capture());

    UnifiedTaskRecorder.CreateCommand started = startCaptor.getValue();
    assertThat(started.taskType()).isEqualTo("database_connectivity");
    assertThat(started.taskName()).isEqualTo("检测数据库连通性");
    assertThat(started.status()).isEqualTo("running");
    assertThat(started.sourceId()).isEqualTo(31L);
    assertThat(started.errorSummary()).isNull();

    UnifiedTaskRecorder.UpdateCommand updated = updateCaptor.getValue();
    assertThat(updated.id()).isEqualTo(9004L);
    assertThat(updated.status()).isEqualTo("failed");
    assertThat(updated.summary()).isEqualTo("检测 1 条，成功 0，失败 1，跳过 0");
    assertThat(updated.errorSummary()).isEqualTo("数据库连通性检测存在失败项");
    assertThat(updated.detailPreview())
        .contains("\"mode\":\"single\"")
        .contains("\"summary\":\"检测 1 条，成功 0，失败 1，跳过 0\"")
        .contains("\"total\":1")
        .contains("\"success\":0")
        .contains("\"failed\":1")
        .contains("\"skipped\":0")
        .contains("\"errorSummary\":\"数据库连通性检测存在失败项\"")
        .doesNotContain("simulated runtime failure");

    verify(mapper, times(0)).updateConnectivitySnapshot(any(), any(), any());
  }

  @Test
  void checkSelectedDatabasesPreservesPartialProgressWhenLaterRowThrows() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(
        new StubChecker("mysql", true, "connected"),
        new ThrowingChecker("postgresql")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(41L, "inventory_prod", "mysql", "10.20.1.41", 3306, "inventory_app", "sealed:inventory");
    AssetDatabaseMapper.DatabaseRow postgres = databaseRow(42L, "ledger_prod", "postgresql", "10.20.1.42", 5432, "ledger_app", "sealed:ledger");

    when(mapper.findDatabasesByIds(List.of(41L, 42L))).thenReturn(List.of(mysql, postgres));
    when(secretProtector.reveal("sealed:inventory")).thenReturn("Inventory@123456");
    when(secretProtector.reveal("sealed:ledger")).thenReturn("Ledger@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9005L);

    assertThatThrownBy(() -> service.checkSelectedDatabases(List.of(41L, 42L)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("simulated runtime failure");

    ArgumentCaptor<UnifiedTaskRecorder.UpdateCommand> updateCaptor = ArgumentCaptor.forClass(UnifiedTaskRecorder.UpdateCommand.class);
    verify(unifiedTaskRecorder, times(1)).start(any(UnifiedTaskRecorder.CreateCommand.class));
    verify(unifiedTaskRecorder, times(1)).update(updateCaptor.capture());

    UnifiedTaskRecorder.UpdateCommand updated = updateCaptor.getValue();
    assertThat(updated.id()).isEqualTo(9005L);
    assertThat(updated.status()).isEqualTo("failed");
    assertThat(updated.summary()).isEqualTo("批量检测 2 条，成功 1，失败 1，跳过 0");
    assertThat(updated.errorSummary()).isEqualTo("数据库连通性检测存在失败项");
    assertThat(updated.detailPreview())
        .contains("\"mode\":\"batch\"")
        .contains("\"summary\":\"批量检测 2 条，成功 1，失败 1，跳过 0\"")
        .contains("\"total\":2")
        .contains("\"success\":1")
        .contains("\"failed\":1")
        .contains("\"skipped\":0")
        .contains("\"errorSummary\":\"数据库连通性检测存在失败项\"")
        .doesNotContain("simulated runtime failure");

    verify(mapper).updateConnectivitySnapshot(eq(41L), eq("online"), any(LocalDateTime.class));
    verify(mapper, times(1)).updateConnectivitySnapshot(any(), any(), any());
  }

  @Test
  void checkOneDatabaseDoesNotWriteSecondFailedUpdateWhenFinishUpdateThrows() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(
        mapper,
        factory,
        secretProtector,
        unifiedTaskRecorder,
        new UnifiedTaskDetailPreviewFactory());

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(51L, "report_prod", "mysql", "10.20.1.51", 3306, "report_app", "sealed:report");

    when(mapper.findDatabasesByIds(List.of(51L))).thenReturn(List.of(mysql));
    when(secretProtector.reveal("sealed:report")).thenReturn("Report@123456");
    when(unifiedTaskRecorder.start(any(UnifiedTaskRecorder.CreateCommand.class))).thenReturn(9006L);
    doThrow(new RuntimeException("update failed"))
        .when(unifiedTaskRecorder)
        .update(any(UnifiedTaskRecorder.UpdateCommand.class));

    assertThatThrownBy(() -> service.checkOneDatabase(51L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("update failed");

    verify(unifiedTaskRecorder, times(1)).start(any(UnifiedTaskRecorder.CreateCommand.class));
    verify(unifiedTaskRecorder, times(1)).update(any(UnifiedTaskRecorder.UpdateCommand.class));
    verify(mapper).updateConnectivitySnapshot(eq(51L), eq("online"), any(LocalDateTime.class));
  }

  private static AssetDatabaseMapper.DatabaseRow databaseRow(Long id,
                                                             String databaseName,
                                                             String databaseType,
                                                             String hostIpAddress,
                                                             Integer port,
                                                             String connectionUsername,
                                                             String connectionPassword) {
    AssetDatabaseMapper.DatabaseRow row = new AssetDatabaseMapper.DatabaseRow();
    row.setId(id);
    row.setDatabaseName(databaseName);
    row.setDatabaseType(databaseType);
    row.setHostIpAddress(hostIpAddress);
    row.setPort(port);
    row.setConnectionUsername(connectionUsername);
    row.setConnectionPassword(connectionPassword);
    row.setEnvironment("production");
    row.setInstanceName(databaseName);
    return row;
  }

  private record StubChecker(String databaseType, boolean success, String message) implements DatabaseConnectivityChecker {
    @Override
    public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
      return success ? DatabaseConnectivityProbeResult.success(message) : DatabaseConnectivityProbeResult.failure(message);
    }
  }

  private record ThrowingChecker(String databaseType) implements DatabaseConnectivityChecker {
    @Override
    public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
      throw new RuntimeException("simulated runtime failure");
    }
  }

}
