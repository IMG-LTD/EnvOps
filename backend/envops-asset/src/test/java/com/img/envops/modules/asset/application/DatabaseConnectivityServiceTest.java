package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.application.connectivity.DatabaseConnectionFactory;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityChecker;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseConnectivityServiceTest {
  @Test
  void checkOneDatabaseRunsSingleRecordAndWritesOnlineSnapshot() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(mapper, factory, secretProtector);

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql");

    when(mapper.findDatabasesByIds(List.of(11L))).thenReturn(List.of(mysql));
    when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkOneDatabase(11L);

    assertThat(report.summary().total()).isEqualTo(1);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(0);
    assertThat(report.summary().skipped()).isEqualTo(0);
    assertThat(report.results()).singleElement().satisfies(item -> {
      assertThat(item.databaseId()).isEqualTo(11L);
      assertThat(item.status()).isEqualTo("success");
      assertThat(item.connectivityStatus()).isEqualTo("online");
      assertThat(item.checkedAt()).isNotNull();
    });

    verify(mapper).findDatabasesByIds(List.of(11L));
    verify(mapper).updateConnectivitySnapshot(eq(11L), eq("online"), any(LocalDateTime.class));
  }

  @Test
  void checkSelectedDatabasesAggregatesSuccessFailureAndSkipped() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    DatabaseConnectivityChecker mysqlChecker = new StubChecker("mysql", true, "connected");
    DatabaseConnectivityChecker postgresqlChecker = new StubChecker("postgresql", false, "认证失败");
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(mysqlChecker, postgresqlChecker));
    DatabaseConnectivityService service = new DatabaseConnectivityService(mapper, factory, secretProtector);

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql");
    AssetDatabaseMapper.DatabaseRow postgres = databaseRow(12L, "traffic_gate", "postgresql", "10.20.1.12", 5432, "traffic_app", "sealed:pg");
    AssetDatabaseMapper.DatabaseRow missing = databaseRow(13L, "session_hub", "redis", "10.20.1.13", 6379, null, null);

    when(mapper.findDatabasesByIds(List.of(11L, 12L, 13L))).thenReturn(List.of(mysql, postgres, missing));
    when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");
    when(secretProtector.reveal("sealed:pg")).thenReturn("Traffic@123456");

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkSelectedDatabases(List.of(11L, 12L, 13L));

    assertThat(report.summary().total()).isEqualTo(3);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(1);
    assertThat(report.summary().skipped()).isEqualTo(1);
    assertThat(report.results()).extracting(DatabaseConnectivityService.DatabaseConnectivityItem::message)
        .contains("connected", "认证失败", "缺少连接用户名或密码");

    verify(mapper).updateConnectivitySnapshot(eq(11L), eq("online"), any(LocalDateTime.class));
    verify(mapper).updateConnectivitySnapshot(eq(12L), eq("offline"), any(LocalDateTime.class));
  }

  @Test
  void checkDatabasesByQueryForwardsFiltersAndDeduplicatesRows() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(new StubChecker("mysql", true, "connected")));
    DatabaseConnectivityService service = new DatabaseConnectivityService(mapper, factory, secretProtector);

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(21L, "billing_prod", "mysql", "10.20.1.21", 3306, "billing_app", "sealed:billing");

    when(mapper.findAllDatabasesByQuery("billing", "production", "mysql", "managed", "warning"))
        .thenReturn(List.of(mysql, mysql));
    when(secretProtector.reveal("sealed:billing")).thenReturn("Billing@123456");

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
    assertThat(report.results()).singleElement().satisfies(item -> {
      assertThat(item.databaseId()).isEqualTo(21L);
      assertThat(item.status()).isEqualTo("success");
      assertThat(item.connectivityStatus()).isEqualTo("online");
    });

    verify(mapper).findAllDatabasesByQuery("billing", "production", "mysql", "managed", "warning");
    verify(mapper).updateConnectivitySnapshot(eq(21L), eq("online"), any(LocalDateTime.class));
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
}
