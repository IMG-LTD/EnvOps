package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.asset.application.AssetApplicationService;
import com.img.envops.modules.asset.application.DatabaseConnectivityService;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityChecker;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AssetControllerTest.TestBeans.class)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "spring.main.allow-bean-definition-overriding=true"
})
class AssetControllerTest {
  private static final String CREDENTIAL_PROTECTION_SECRET = "test-only-envops-credential-protection-secret-12345";
  private static final String EDGE_SSH_KEY_PLACEHOLDER = "FAKE_EDGE_OPS_SSH_KEY_DO_NOT_USE";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @SpyBean
  private AssetApplicationService assetApplicationService;

  @SpyBean
  private DatabaseConnectivityService databaseConnectivityService;

  @Test
  void getHostsReturnsPaginatedRecords() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/assets/hosts")
            .param("current", "1")
            .param("size", "10")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.current").value(1))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.total").value(4))
        .andExpect(jsonPath("$.data.summary.managedHosts").value(4))
        .andExpect(jsonPath("$.data.summary.onlineHosts").value(2))
        .andExpect(jsonPath("$.data.summary.warningHosts").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].hostName").value("host-sbx-01"))
        .andExpect(jsonPath("$.data.records[0].hasMonitorFacts").value(false))
        .andExpect(jsonPath("$.data.records[0].latestMonitorFactAt").isEmpty())
        .andExpect(jsonPath("$.data.records[1].hostName").value("host-stg-01"))
        .andExpect(jsonPath("$.data.records[1].hasMonitorFacts").value(false))
        .andExpect(jsonPath("$.data.records[1].latestMonitorFactAt").isEmpty())
        .andExpect(jsonPath("$.data.records[3].hostName").value("host-prd-01"))
        .andExpect(jsonPath("$.data.records[3].hasMonitorFacts").value(true))
        .andExpect(jsonPath("$.data.records[3].latestMonitorFactAt").value("2026-04-16T08:45:00"));
  }

  @Test
  void createHostReturnsCreatedHost() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/hosts")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "hostName": "host-new-01",
                  "ipAddress": "10.60.1.20",
                  "environment": "sandbox",
                  "clusterName": "cn-shenzhen-a",
                  "ownerName": "Asset Team",
                  "status": "online",
                  "lastHeartbeat": "2026-04-16T11:22:33"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.hostName").value("host-new-01"))
        .andExpect(jsonPath("$.data.ipAddress").value("10.60.1.20"))
        .andExpect(jsonPath("$.data.lastHeartbeat").value("2026-04-16T11:22:33"));
  }

  @Test
  void getDatabasesReturnsPaginatedRecords() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/assets/databases")
            .param("current", "1")
            .param("size", "10")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.current").value(1))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.total").value(6))
        .andExpect(jsonPath("$.data.summary.managedDatabases").value(5))
        .andExpect(jsonPath("$.data.summary.warningDatabases").value(2))
        .andExpect(jsonPath("$.data.summary.onlineDatabases").value(2))
        .andExpect(jsonPath("$.data.records[0].databaseName").value("session_hub"))
        .andExpect(jsonPath("$.data.records[0].databaseType").value("redis"))
        .andExpect(jsonPath("$.data.records[0].connectionUsername").value("sandbox_cache"))
        .andExpect(jsonPath("$.data.records[0].connectionPassword").doesNotExist())
        .andExpect(jsonPath("$.data.records[1].databaseName").value("event_bus"))
        .andExpect(jsonPath("$.data.records[1].databaseType").value("mongodb"))
        .andExpect(jsonPath("$.data.records[2].databaseName").value("ops_metrics"))
        .andExpect(jsonPath("$.data.records[2].credentialId").isEmpty())
        .andExpect(jsonPath("$.data.records[5].databaseName").value("order_prod"))
        .andExpect(jsonPath("$.data.records[5].credentialName").value("demo-fake-prod-root-password"))
        .andExpect(jsonPath("$.data.records[5].connectionUsername").value("orders_app"))
        .andExpect(jsonPath("$.data.records[5].connectionPassword").doesNotExist());
  }

  @Test
  void getDatabasesSupportsFiltersAndPagination() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/assets/databases")
            .param("keyword", "archive")
            .param("environment", "staging")
            .param("databaseType", "oracle")
            .param("lifecycleStatus", "managed")
            .param("connectivityStatus", "warning")
            .param("current", "1")
            .param("size", "1")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.current").value(1))
        .andExpect(jsonPath("$.data.size").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.summary.managedDatabases").value(1))
        .andExpect(jsonPath("$.data.summary.warningDatabases").value(1))
        .andExpect(jsonPath("$.data.summary.onlineDatabases").value(0))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].databaseName").value("billing_archive"));
  }

  @Test
  void createDatabaseReturnsCreatedRecord() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "billing_stg",
                  "databaseType": "sqlserver",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 1434,
                  "instanceName": "sqlserver-stg-a",
                  "credentialId": 2,
                  "ownerName": "Release DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "online",
                  "connectionUsername": "billing_app",
                  "connectionPassword": "Billing@123456",
                  "description": "计费预发数据库",
                  "lastCheckedAt": "2026-04-18T12:00:00"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.databaseName").value("billing_stg"))
        .andExpect(jsonPath("$.data.databaseType").value("sqlserver"))
        .andExpect(jsonPath("$.data.hostId").value(3))
        .andExpect(jsonPath("$.data.credentialId").value(2))
        .andExpect(jsonPath("$.data.connectionUsername").value("billing_app"))
        .andExpect(jsonPath("$.data.connectionPassword").doesNotExist())
        .andExpect(jsonPath("$.data.lastCheckedAt").value("2026-04-18T12:00:00"));
  }

  @Test
  void createDatabasePersistsConnectionUsernameAndSealedPassword() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "mysql_detectable",
                  "databaseType": "mysql",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 3308,
                  "instanceName": "mysql-stg-detect",
                  "credentialId": 2,
                  "ownerName": "Platform DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "unknown",
                  "connectionUsername": "orders_app",
                  "connectionPassword": "Orders@123456",
                  "description": "可检测数据库"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.connectionUsername").value("orders_app"))
        .andExpect(jsonPath("$.data.connectionPassword").doesNotExist());

    String stored = jdbcTemplate.queryForObject(
        "SELECT connection_password FROM asset_database WHERE database_name = ?",
        String.class,
        "mysql_detectable");

    org.assertj.core.api.Assertions.assertThat(stored)
        .startsWith("sealed:v1:")
        .isNotEqualTo("Orders@123456");
  }

  @Test
  void createDatabaseAcceptsSupportedMainstreamTypes() throws Exception {
    String accessToken = login();
    List<SupportedDatabaseCase> cases = List.of(
        new SupportedDatabaseCase("mysql_registry", "mysql", 3307, "mysql-stg-reg"),
        new SupportedDatabaseCase("postgres_registry", "postgresql", 5433, "pg-stg-reg"),
        new SupportedDatabaseCase("oracle_registry", "oracle", 1522, "oracle-stg-reg"),
        new SupportedDatabaseCase("sqlserver_registry", "sqlserver", 1435, "sqlserver-stg-reg"),
        new SupportedDatabaseCase("mongodb_registry", "mongodb", 27018, "mongodb-stg-reg"),
        new SupportedDatabaseCase("redis_registry", "redis", 6380, "redis-stg-reg")
    );

    for (SupportedDatabaseCase databaseCase : cases) {
      mockMvc.perform(post("/api/assets/databases")
              .header("Authorization", "Bearer " + accessToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "databaseName": "%s",
                    "databaseType": "%s",
                    "environment": "staging",
                    "hostId": 3,
                    "port": %d,
                    "instanceName": "%s",
                    "credentialId": 2,
                    "ownerName": "Registry DBA",
                    "lifecycleStatus": "managed",
                    "connectivityStatus": "unknown",
                    "description": "类型支持校验"
                  }
                  """.formatted(
                  databaseCase.databaseName(),
                  databaseCase.databaseType(),
                  databaseCase.port(),
                  databaseCase.instanceName())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("0000"))
          .andExpect(jsonPath("$.data.databaseName").value(databaseCase.databaseName()))
          .andExpect(jsonPath("$.data.databaseType").value(databaseCase.databaseType()));
    }
  }

  @Test
  void createDatabaseRejectsUnsupportedDatabaseType() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "legacy_registry",
                  "databaseType": "db2",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 50000,
                  "instanceName": "db2-stg-a",
                  "credentialId": 2,
                  "ownerName": "Legacy DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "unknown",
                  "description": "暂不支持的数据库类型"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("databaseType must be one of mysql, postgresql, oracle, sqlserver, mongodb, redis"));
  }

  @Test
  void updateDatabaseReturnsUpdatedRecord() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "staging_ops",
                  "databaseType": "oracle",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 1523,
                  "instanceName": "oracle-stg-a",
                  "credentialId": 2,
                  "ownerName": "QA DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "warning",
                  "description": "原始记录"
                }
                """))
        .andExpect(status().isOk());

    Long createdId = jdbcTemplate.queryForObject(
        "SELECT id FROM asset_database WHERE database_name = ?",
        Long.class,
        "staging_ops");

    mockMvc.perform(put("/api/assets/databases/{id}", createdId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "staging_ops",
                  "databaseType": "sqlserver",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 1436,
                  "instanceName": "sqlserver-stg-b",
                  "credentialId": null,
                  "ownerName": "Platform DBA",
                  "lifecycleStatus": "disabled",
                  "connectivityStatus": "offline",
                  "description": "更新后的记录",
                  "lastCheckedAt": "2026-04-18T18:30:00"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.databaseType").value("sqlserver"))
        .andExpect(jsonPath("$.data.instanceName").value("sqlserver-stg-b"))
        .andExpect(jsonPath("$.data.ownerName").value("Platform DBA"))
        .andExpect(jsonPath("$.data.lifecycleStatus").value("disabled"))
        .andExpect(jsonPath("$.data.connectivityStatus").value("offline"))
        .andExpect(jsonPath("$.data.credentialId").isEmpty())
        .andExpect(jsonPath("$.data.lastCheckedAt").value("2026-04-18T18:30:00"));
  }

  @Test
  void updateDatabaseKeepsExistingConnectionPasswordWhenBlank() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "redis_detectable",
                  "databaseType": "redis",
                  "environment": "sandbox",
                  "hostId": 4,
                  "port": 6382,
                  "instanceName": "redis-sbx-detect",
                  "credentialId": null,
                  "ownerName": "QA DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "unknown",
                  "connectionUsername": "sandbox_cache",
                  "connectionPassword": "Cache@123456",
                  "description": "待更新凭据"
                }
                """))
        .andExpect(status().isOk());

    Long createdId = jdbcTemplate.queryForObject(
        "SELECT id FROM asset_database WHERE database_name = ?",
        Long.class,
        "redis_detectable");
    String before = jdbcTemplate.queryForObject(
        "SELECT connection_password FROM asset_database WHERE id = ?",
        String.class,
        createdId);

    mockMvc.perform(put("/api/assets/databases/{id}", createdId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "redis_detectable",
                  "databaseType": "redis",
                  "environment": "sandbox",
                  "hostId": 4,
                  "port": 6382,
                  "instanceName": "redis-sbx-detect",
                  "credentialId": null,
                  "ownerName": "QA DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "online",
                  "connectionUsername": "sandbox_cache",
                  "connectionPassword": "   ",
                  "description": "沿用原密码"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.connectionUsername").value("sandbox_cache"))
        .andExpect(jsonPath("$.data.connectionPassword").doesNotExist());

    String after = jdbcTemplate.queryForObject(
        "SELECT connection_password FROM asset_database WHERE id = ?",
        String.class,
        createdId);

    org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
  }

  @Test
  void createDatabaseRejectsIncompleteConnectionCredentials() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "broken_detectable",
                  "databaseType": "mysql",
                  "environment": "staging",
                  "hostId": 3,
                  "port": 3309,
                  "instanceName": "mysql-stg-broken",
                  "credentialId": 2,
                  "ownerName": "Platform DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "unknown",
                  "connectionUsername": "orders_app",
                  "description": "缺少密码"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("connectionUsername and connectionPassword must be provided together"));
  }

  @Test
  void checkDatabaseConnectivityReturnsSingleResult() throws Exception {
    String accessToken = login();
    DatabaseConnectivityService.DatabaseConnectivityReport report =
        new DatabaseConnectivityService.DatabaseConnectivityReport(
            new DatabaseConnectivityService.DatabaseConnectivitySummary(1, 1, 0, 0),
            List.of(new DatabaseConnectivityService.DatabaseConnectivityItem(
                6L,
                "session_hub",
                "redis",
                "sandbox",
                "success",
                "connected",
                "online",
                java.time.LocalDateTime.parse("2026-04-21T10:15:00"))));
    doReturn(report).when(databaseConnectivityService).checkOneDatabase(6L);

    mockMvc.perform(post("/api/assets/databases/{id}/connectivity-check", 6L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.summary.total").value(1))
        .andExpect(jsonPath("$.data.results[0].databaseName").value("session_hub"))
        .andExpect(jsonPath("$.data.results[0].status").value("success"))
        .andExpect(jsonPath("$.data.results[0].connectivityStatus").value("online"));
  }

  @Test
  void checkDatabaseConnectivityWritesTrackingSnapshotForUnifiedTask() throws Exception {
    String accessToken = login();

    Long databaseId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 100 FROM asset_database", Long.class);
    jdbcTemplate.update(
        """
        INSERT INTO asset_database (
            id, database_name, database_type, environment, host_id, port, instance_name,
            credential_id, owner_name, lifecycle_status, connectivity_status,
            connection_username, connection_password, description, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        databaseId,
        "redis_tracking_snapshot",
        "redis",
        "sandbox",
        4L,
        6383,
        "redis-sbx-tracking",
        null,
        "QA DBA",
        "managed",
        "unknown",
        "sandbox_cache",
        seal("Cache@123456"),
        "追踪快照测试库");

    try {
      mockMvc.perform(post("/api/assets/databases/{id}/connectivity-check", databaseId)
              .header("Authorization", "Bearer " + accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("0000"))
          .andExpect(jsonPath("$.data.summary.total").value(1))
          .andExpect(jsonPath("$.data.summary.success").value(1));

      Long unifiedTaskId = jdbcTemplate.queryForObject(
          "SELECT id FROM unified_task_center WHERE task_type = ? ORDER BY id DESC LIMIT 1",
          Long.class,
          "database_connectivity");

      mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", unifiedTaskId)
              .header("Authorization", "Bearer " + accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("0000"))
          .andExpect(jsonPath("$.data.basicInfo.taskType").value("database_connectivity"))
          .andExpect(jsonPath("$.data.timeline.length()").value(2))
          .andExpect(jsonPath("$.data.logSummary").value(containsString("成功")))
          .andExpect(jsonPath("$.data.sourceLinks[0].route").value("/asset/database"))
          .andExpect(jsonPath("$.data.degraded").value(false));
    } finally {
      jdbcTemplate.update("DELETE FROM asset_database WHERE id = ?", databaseId);
    }
  }

  @Test
  void checkDatabaseConnectivityBySelectedRowsReturnsBatchReport() throws Exception {
    String accessToken = login();
    DatabaseConnectivityService.DatabaseConnectivityReport report =
        new DatabaseConnectivityService.DatabaseConnectivityReport(
            new DatabaseConnectivityService.DatabaseConnectivitySummary(2, 1, 0, 1),
            List.of(
                new DatabaseConnectivityService.DatabaseConnectivityItem(1L, "order_prod", "mysql", "production", "success", "connected", "online", java.time.LocalDateTime.parse("2026-04-21T10:15:00")),
                new DatabaseConnectivityService.DatabaseConnectivityItem(2L, "traffic_gate", "postgresql", "production", "skipped", "缺少连接用户名或密码", "unknown", null)));
    doReturn(report).when(databaseConnectivityService).checkSelectedDatabases(List.of(1L, 2L));

    mockMvc.perform(post("/api/assets/databases/connectivity-check:selected")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "ids": [1, 2]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.summary.total").value(2))
        .andExpect(jsonPath("$.data.summary.skipped").value(1))
        .andExpect(jsonPath("$.data.results[1].message").value("缺少连接用户名或密码"));
  }

  @Test
  void checkDatabaseConnectivityByCurrentPageReturnsBatchReport() throws Exception {
    String accessToken = login();
    DatabaseConnectivityService.DatabaseConnectivityReport report =
        new DatabaseConnectivityService.DatabaseConnectivityReport(
            new DatabaseConnectivityService.DatabaseConnectivitySummary(1, 0, 1, 0),
            List.of(new DatabaseConnectivityService.DatabaseConnectivityItem(
                4L,
                "ops_metrics",
                "sqlserver",
                "production",
                "failed",
                "认证失败",
                "offline",
                java.time.LocalDateTime.parse("2026-04-21T10:20:00"))));
    doReturn(report).when(databaseConnectivityService).checkSelectedDatabases(List.of(4L));

    mockMvc.perform(post("/api/assets/databases/connectivity-check:page")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "ids": [4]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.summary.failed").value(1))
        .andExpect(jsonPath("$.data.results[0].connectivityStatus").value("offline"));
  }

  @Test
  void checkDatabaseConnectivityByQueryReturnsBatchReport() throws Exception {
    String accessToken = login();
    DatabaseConnectivityService.DatabaseConnectivityReport report =
        new DatabaseConnectivityService.DatabaseConnectivityReport(
            new DatabaseConnectivityService.DatabaseConnectivitySummary(1, 1, 0, 0),
            List.of(new DatabaseConnectivityService.DatabaseConnectivityItem(
                1L,
                "order_prod",
                "mysql",
                "production",
                "success",
                "connected",
                "online",
                java.time.LocalDateTime.parse("2026-04-21T10:25:00"))));
    doReturn(report).when(databaseConnectivityService).checkDatabasesByQuery("prod", "production", "mysql", "managed", "warning");

    mockMvc.perform(post("/api/assets/databases/connectivity-check:query")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "keyword": "prod",
                  "environment": "production",
                  "databaseType": "mysql",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "warning"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.summary.success").value(1))
        .andExpect(jsonPath("$.data.results[0].databaseName").value("order_prod"));
  }

  @Test
  void deleteDatabaseRemovesRecord() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/databases")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "databaseName": "cleanup_target",
                  "databaseType": "redis",
                  "environment": "sandbox",
                  "hostId": 4,
                  "port": 6381,
                  "instanceName": "redis-sbx-delete",
                  "credentialId": 2,
                  "ownerName": "QA DBA",
                  "lifecycleStatus": "managed",
                  "connectivityStatus": "unknown",
                  "description": "待删除数据库"
                }
                """))
        .andExpect(status().isOk());

    Long createdId = jdbcTemplate.queryForObject(
        "SELECT id FROM asset_database WHERE database_name = ?",
        Long.class,
        "cleanup_target");

    mockMvc.perform(delete("/api/assets/databases/{id}", createdId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data").value(true));

    Integer remaining = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM asset_database WHERE id = ?",
        Integer.class,
        createdId);

    org.assertj.core.api.Assertions.assertThat(remaining).isZero();
  }

  @Test
  void createCredentialReturnsCreatedCredentialWithoutSecretAndPersistsProtectedValue() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "edge-maintenance-key",
                  "credentialType": "ssh_key",
                  "username": "ops",
                  "secret": "FAKE_EDGE_OPS_SSH_KEY_DO_NOT_USE",
                  "description": "边缘节点巡检临时密钥"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.name").value("edge-maintenance-key"))
        .andExpect(jsonPath("$.data.credentialType").value("ssh_key"))
        .andExpect(jsonPath("$.data.username").value("ops"))
        .andExpect(jsonPath("$.data.secret").doesNotExist());

    String storedSecret = jdbcTemplate.queryForObject(
        "SELECT secret FROM asset_credential WHERE name = ?",
        String.class,
        "edge-maintenance-key");

    org.assertj.core.api.Assertions.assertThat(storedSecret)
        .isEqualTo(protect(EDGE_SSH_KEY_PLACEHOLDER))
        .isNotEqualTo(EDGE_SSH_KEY_PLACEHOLDER)
        .startsWith("protected:v1:");
  }

  @Test
  void createCredentialRejectsUnsupportedCredentialType() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "unsupported-credential",
                  "credentialType": "password",
                  "username": "ops",
                  "secret": "FAKE_PASSWORD_DO_NOT_USE"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("credentialType must be one of ssh_password, ssh_key, api_token"));
  }

  @Test
  void createCredentialRejectsBlankSecret() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "blank-secret-credential",
                  "credentialType": "api_token",
                  "username": "ops",
                  "secret": "   "
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("secret is required"));
  }

  @Test
  void assetInternalFailuresReturn500InsteadOf401() throws Exception {
    String accessToken = login();
    doThrow(new IllegalStateException("simulated asset failure")).when(assetApplicationService).getGroups();

    mockMvc.perform(get("/api/assets/groups")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("500"))
        .andExpect(jsonPath("$.msg").value("Internal server error"));
  }

  private String login() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "envops-admin",
                  "password": "EnvOps@123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private String seal(String rawSecret) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
      byte[] iv = new byte[12];
      new java.security.SecureRandom().nextBytes(iv);
      cipher.init(
          javax.crypto.Cipher.ENCRYPT_MODE,
          new javax.crypto.spec.SecretKeySpec(
              java.security.MessageDigest.getInstance("SHA-256").digest(CREDENTIAL_PROTECTION_SECRET.getBytes(StandardCharsets.UTF_8)),
              "AES"),
          new javax.crypto.spec.GCMParameterSpec(128, iv));
      byte[] ciphertext = cipher.doFinal(rawSecret.trim().getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
      return "sealed:v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("failed to compute sealed database secret", exception);
    }
  }

  private String protect(String rawSecret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(CREDENTIAL_PROTECTION_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(rawSecret.getBytes(StandardCharsets.UTF_8)));
      return "protected:v1:" + encoded;
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("failed to compute expected protected secret", exception);
    }
  }

  @TestConfiguration
  static class TestBeans {
    @Bean
    @Primary
    DatabaseConnectivityChecker redisConnectivityChecker() {
      return new DatabaseConnectivityChecker() {
        @Override
        public String databaseType() {
          return "redis";
        }

        @Override
        public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
          return DatabaseConnectivityProbeResult.success("connected");
        }
      };
    }
  }

  private record SupportedDatabaseCase(String databaseName, String databaseType, int port, String instanceName) {
  }
}
