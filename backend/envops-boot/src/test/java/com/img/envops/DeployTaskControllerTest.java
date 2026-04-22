package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.deploy.application.DeployTaskApplicationService;
import com.img.envops.modules.deploy.infrastructure.mapper.DeployTaskMapper;
import com.img.envops.modules.deploy.executor.SshConnectionOptions;
import com.img.envops.modules.deploy.executor.SshProcessRunner;
import com.img.envops.modules.task.infrastructure.bootstrap.UnifiedTaskCenterBackfillRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DeployTaskControllerTest.TestBeans.class)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "envops.deploy.async=false",
    "envops.storage.local-base-dir=${java.io.tmpdir}/envops-task2-deploy-storage"
})
class DeployTaskControllerTest {
  private static final AtomicLong EXECUTABLE_VERSION_SEQUENCE = new AtomicLong(1900);
  private static final String TEST_PRIVATE_KEY_PATH = System.getProperty("java.io.tmpdir") + "/envops-task2-deploy-storage/keys/release.pem";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DeployTaskApplicationService deployTaskApplicationService;

  @Autowired
  private DeployTaskMapper deployTaskMapper;

  @Autowired
  private FakeSshProcessRunner fakeSshProcessRunner;

  @Autowired
  private UnifiedTaskCenterBackfillRunner unifiedTaskCenterBackfillRunner;

  @Value("${envops.storage.local-base-dir}")
  private String storageBaseDir;

  @BeforeEach
  void resetFakeSshProcessRunner() throws Exception {
    fakeSshProcessRunner.reset();
    Path storagePath = Paths.get(storageBaseDir).toAbsolutePath().normalize();
    Files.createDirectories(storagePath);
    Files.createDirectories(storagePath.resolve("keys"));
    Files.writeString(storagePath.resolve("keys").resolve("release.pem"), "test-private-key");
  }

  @Test
  void createDeployTaskReturnsPendingApprovalAcrossDeployEndpoints() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    MvcResult createResult = mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "install-order-service-prod",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1, 1, 2],
                  "batchStrategy": "ROLLING",
                  "batchSize": 1,
                  "params": {
                    "deployDir": "/data/apps/order-service",
                    "port": 8080,
                    "profile": "prod",
                    "environment": "production",
                    "sshUser": "deploy",
                    "sshPort": 22,
                    "privateKeyPath": "%s",
                    "remoteBaseDir": "/opt/envops/releases",
                    "rollbackCommand": "bash /opt/envops/bin/rollback.sh",
                    "accessToken": "prod-secret-token"
                  }
                }
                """.formatted(TEST_PRIVATE_KEY_PATH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.taskNo", not(blankOrNullString())))
        .andExpect(jsonPath("$.data.taskName").value("install-order-service-prod"))
        .andExpect(jsonPath("$.data.taskType").value("INSTALL"))
        .andExpect(jsonPath("$.data.appId").value(1001))
        .andExpect(jsonPath("$.data.versionId").value(1401))
        .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.data.batchStrategy").value("ROLLING"))
        .andExpect(jsonPath("$.data.batchSize").value(1))
        .andExpect(jsonPath("$.data.targetCount").value(2))
        .andExpect(jsonPath("$.data.successCount").value(0))
        .andExpect(jsonPath("$.data.failCount").value(0))
        .andExpect(jsonPath("$.data.operatorName").value("release-admin"))
        .andExpect(jsonPath("$.data.params.environment").value("production"))
        .andExpect(jsonPath("$.data.params.sshUser").value("deploy"))
        .andExpect(jsonPath("$.data.params.sshPort").value("22"))
        .andExpect(jsonPath("$.data.params.remoteBaseDir").value("/opt/envops/releases"))
        .andExpect(jsonPath("$.data.params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.params.port").doesNotExist())
        .andExpect(jsonPath("$.data.params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.params.privateKeyPath").doesNotExist())
        .andExpect(jsonPath("$.data.params.rollbackCommand").doesNotExist())
        .andExpect(jsonPath("$.data.params.accessToken").doesNotExist())
        .andReturn();

    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
    long taskId = created.path("id").asLong();

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("PENDING_APPROVAL");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT task_no FROM deploy_task WHERE id = ?", String.class, taskId))
        .isNotBlank();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT operator_name FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("release-admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_host WHERE task_id = ?", Integer.class, taskId))
        .isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_param WHERE task_id = ?", Integer.class, taskId))
        .isEqualTo(6);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_param WHERE task_id = ? AND param_key = ?", Integer.class, taskId, "environment"))
        .isEqualTo(1);

    mockMvc.perform(get("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.records.length()").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.data.records[0].id").value((int) taskId))
        .andExpect(jsonPath("$.data.records[0].taskName").value("install-order-service-prod"))
        .andExpect(jsonPath("$.data.records[0].status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.data.records[0].params.environment").value("production"))
        .andExpect(jsonPath("$.data.records[0].params.sshUser").value("deploy"))
        .andExpect(jsonPath("$.data.records[0].params.sshPort").value("22"))
        .andExpect(jsonPath("$.data.records[0].params.remoteBaseDir").value("/opt/envops/releases"))
        .andExpect(jsonPath("$.data.records[0].params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.port").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.privateKeyPath").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.rollbackCommand").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.accessToken").doesNotExist());

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.taskNo").value(created.path("taskNo").asText()))
        .andExpect(jsonPath("$.data.taskName").value("install-order-service-prod"))
        .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.data.params.environment").value("production"))
        .andExpect(jsonPath("$.data.params.sshUser").value("deploy"))
        .andExpect(jsonPath("$.data.params.sshPort").value("22"))
        .andExpect(jsonPath("$.data.params.remoteBaseDir").value("/opt/envops/releases"))
        .andExpect(jsonPath("$.data.params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.params.port").doesNotExist())
        .andExpect(jsonPath("$.data.params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.params.privateKeyPath").doesNotExist())
        .andExpect(jsonPath("$.data.params.rollbackCommand").doesNotExist())
        .andExpect(jsonPath("$.data.params.accessToken").doesNotExist());

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].hostId").value(1))
        .andExpect(jsonPath("$.data.records[0].status").value("PENDING"))
        .andExpect(jsonPath("$.data.records[1].hostId").value(2));

    mockMvc.perform(get("/api/deploy/tasks/{id}/logs", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].logLevel").value("INFO"))
        .andExpect(jsonPath("$.data.records[0].logContent").value("Task created"));
  }

  @Test
  void getDeployTaskDetailReturnsProgressSummary() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(
        accessToken,
        "detail-progress-summary",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L, 2L, 3L),
        "ALL",
        0,
        createRequiredParams());

    jdbcTemplate.update("UPDATE deploy_task_host SET status = 'CANCEL_REQUESTED' WHERE task_id = ? AND host_id = ?", taskId, 2L);
    jdbcTemplate.update("UPDATE deploy_task_host SET status = 'FAILED' WHERE task_id = ? AND host_id = ?", taskId, 3L);

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.totalHosts").value(3))
        .andExpect(jsonPath("$.data.pendingHosts").value(1))
        .andExpect(jsonPath("$.data.runningHosts").value(1))
        .andExpect(jsonPath("$.data.successHosts").value(0))
        .andExpect(jsonPath("$.data.failedHosts").value(1))
        .andExpect(jsonPath("$.data.cancelledHosts").value(0));
  }

  @Test
  void getDeployTaskHostsSupportsStatusKeywordAndPaging() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(
        accessToken,
        "hosts-filter-paging",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L, 2L, 3L),
        "ALL",
        0,
        createRequiredParams());

    jdbcTemplate.update("UPDATE deploy_task_host SET status = 'FAILED' WHERE task_id = ? AND host_id = ?", taskId, 3L);

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .param("status", "PENDING")
            .param("keyword", "host-prd")
            .param("page", "2")
            .param("pageSize", "1")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.pageSize").value(1))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].hostId").value(2))
        .andExpect(jsonPath("$.data.records[0].status").value("PENDING"));
  }

  @Test
  void getDeployTaskHostsSupportsKeywordFilteringByIpAddress() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(
        accessToken,
        "hosts-filter-ip-address",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L, 2L, 3L),
        "ALL",
        0,
        createRequiredParams());

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .param("keyword", "10.20.1.12")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].hostId").value(2))
        .andExpect(jsonPath("$.data.records[0].hostName").value("host-prd-02"))
        .andExpect(jsonPath("$.data.records[0].ipAddress").value("10.20.1.12"));
  }

  @Test
  void getDeployTaskLogsSupportsHostKeywordAndPaging() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    createDeployTask(
        accessToken,
        "logs-filter-paging-seed",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L, 2L),
        "ALL",
        0,
        createRequiredParams());
    long taskId = createDeployTask(
        accessToken,
        "logs-filter-paging",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L, 2L),
        "ALL",
        0,
        createRequiredParams());

    long hostOneTaskHostId = jdbcTemplate.queryForObject(
        "SELECT id FROM deploy_task_host WHERE task_id = ? AND host_id = ?",
        Long.class,
        taskId,
        1L);
    long hostTwoTaskHostId = jdbcTemplate.queryForObject(
        "SELECT id FROM deploy_task_host WHERE task_id = ? AND host_id = ?",
        Long.class,
        taskId,
        2L);

    org.assertj.core.api.Assertions.assertThat(hostTwoTaskHostId).isNotEqualTo(2L);

    jdbcTemplate.update(
        "INSERT INTO deploy_task_log (task_id, task_host_id, log_level, log_content, created_at) VALUES (?, ?, ?, ?, ?)",
        taskId,
        hostOneTaskHostId,
        "INFO",
        "target log other host",
        java.sql.Timestamp.valueOf("2026-04-18 10:00:00"));
    jdbcTemplate.update(
        "INSERT INTO deploy_task_log (task_id, task_host_id, log_level, log_content, created_at) VALUES (?, ?, ?, ?, ?)",
        taskId,
        hostTwoTaskHostId,
        "INFO",
        "target log page one",
        java.sql.Timestamp.valueOf("2026-04-18 10:01:00"));
    jdbcTemplate.update(
        "INSERT INTO deploy_task_log (task_id, task_host_id, log_level, log_content, created_at) VALUES (?, ?, ?, ?, ?)",
        taskId,
        hostTwoTaskHostId,
        "INFO",
        "target log page two",
        java.sql.Timestamp.valueOf("2026-04-18 10:02:00"));

    mockMvc.perform(get("/api/deploy/tasks/{id}/logs", taskId)
            .param("hostId", "2")
            .param("keyword", "target log")
            .param("page", "2")
            .param("pageSize", "1")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.pageSize").value(1))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].taskHostId").value((int) hostTwoTaskHostId))
        .andExpect(jsonPath("$.data.records[0].logContent").value("target log page two"));
  }

  @Test
  void actionEndpointsDoNotExposeDetailOnlyHostSummaryFields() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "action-response-no-detail-fields");
    approveDeployTask(accessToken, taskId, "approved for action response test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/cancel", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        .andExpect(jsonPath("$.data.totalHosts").doesNotExist())
        .andExpect(jsonPath("$.data.pendingHosts").doesNotExist())
        .andExpect(jsonPath("$.data.runningHosts").doesNotExist())
        .andExpect(jsonPath("$.data.successHosts").doesNotExist())
        .andExpect(jsonPath("$.data.failedHosts").doesNotExist())
        .andExpect(jsonPath("$.data.cancelledHosts").doesNotExist());
  }

  @Test
  void getDeployTaskHostsRejectsNonPositivePage() throws Exception {
    String accessToken = login();
    long taskId = createDeployTask(accessToken, "hosts-invalid-page");

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .param("page", "0")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("page must be greater than 0"));
  }

  @Test
  void getDeployTaskLogsRejectsNonPositivePageSize() throws Exception {
    String accessToken = login();
    long taskId = createDeployTask(accessToken, "logs-invalid-page-size");

    mockMvc.perform(get("/api/deploy/tasks/{id}/logs", taskId)
            .param("pageSize", "0")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("pageSize must be greater than 0"));
  }

  @Test
  void getDeployTasksSupportsFilteringPaginationAndSorting() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long matchedOlderTaskId = createDeployTask(
        accessToken,
        "deploy-filter-prod-alpha",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(matchedOlderTaskId, "profile", "prod");
    long matchedNewerTaskId = createDeployTask(
        accessToken,
        "deploy-filter-prod-beta",
        "INSTALL",
        1001L,
        1401L,
        List.of(2L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(matchedNewerTaskId, "namespace", "production");
    long differentAppTaskId = createDeployTask(
        accessToken,
        "deploy-filter-prod-gateway",
        "INSTALL",
        1002L,
        1402L,
        List.of(3L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(differentAppTaskId, "environment", "prod");
    long differentTaskTypeId = createDeployTask(
        accessToken,
        "deploy-filter-prod-upgrade",
        "UPGRADE",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(differentTaskTypeId, "env", "prod");
    long differentEnvironmentTaskId = createDeployTask(
        accessToken,
        "deploy-filter-sandbox",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(differentEnvironmentTaskId, "environment", "sandbox");

    approveDeployTask(accessToken, matchedOlderTaskId, "approve alpha");
    approveDeployTask(accessToken, matchedNewerTaskId, "approve beta");
    approveDeployTask(accessToken, differentAppTaskId, "approve gateway");
    approveDeployTask(accessToken, differentTaskTypeId, "approve upgrade");
    approveDeployTask(accessToken, differentEnvironmentTaskId, "approve sandbox");

    updateDeployTaskTimestamps(matchedOlderTaskId, "2026-04-16 10:00:00", "2026-04-16 10:00:00");
    updateDeployTaskTimestamps(matchedNewerTaskId, "2026-04-16 11:00:00", "2026-04-16 11:00:00");
    updateDeployTaskTimestamps(differentAppTaskId, "2026-04-16 10:30:00", "2026-04-16 10:30:00");
    updateDeployTaskTimestamps(differentTaskTypeId, "2026-04-16 10:45:00", "2026-04-16 10:45:00");
    updateDeployTaskTimestamps(differentEnvironmentTaskId, "2026-04-16 11:15:00", "2026-04-16 11:15:00");

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-filter-prod")
            .param("status", "PENDING")
            .param("taskType", "INSTALL")
            .param("appId", "1001")
            .param("environment", "production")
            .param("createdFrom", "2026-04-16T09:30:00")
            .param("createdTo", "2026-04-16T11:30:00")
            .param("page", "2")
            .param("pageSize", "1")
            .param("sortBy", "createdAt")
            .param("sortOrder", "desc")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.pageSize").value(1))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) matchedOlderTaskId))
        .andExpect(jsonPath("$.data.records[0].taskName").value("deploy-filter-prod-alpha"))
        .andExpect(jsonPath("$.data.records[0].taskType").value("INSTALL"))
        .andExpect(jsonPath("$.data.records[0].status").value("PENDING"))
        .andExpect(jsonPath("$.data.records[0].params.profile").value("prod"));
  }

  @Test
  void getDeployTasksSupportsCanonicalEnvironmentAliasesAcrossEnvironmentGroups() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long stagingTaskId = createDeployTask(
        accessToken,
        "deploy-filter-stage-preprod",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(stagingTaskId, "profile", "preprod");
    long sandboxTaskId = createDeployTask(
        accessToken,
        "deploy-filter-sandbox-dev",
        "INSTALL",
        1001L,
        1401L,
        List.of(2L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(sandboxTaskId, "env", "dev");
    long unmatchedTaskId = createDeployTask(
        accessToken,
        "deploy-filter-unmatched-production",
        "INSTALL",
        1001L,
        1401L,
        List.of(3L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(unmatchedTaskId, "environment", "prod");

    approveDeployTask(accessToken, stagingTaskId, "approve staging");
    approveDeployTask(accessToken, sandboxTaskId, "approve sandbox");
    approveDeployTask(accessToken, unmatchedTaskId, "approve production");

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-filter-")
            .param("status", "PENDING")
            .param("environment", "staging")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) stagingTaskId))
        .andExpect(jsonPath("$.data.records[0].params.profile").value("preprod"));

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-filter-")
            .param("status", "PENDING")
            .param("environment", "sandbox")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) sandboxTaskId))
        .andExpect(jsonPath("$.data.records[0].params.env").value("dev"));
  }

  @Test
  void getDeployTasksEnvironmentFilterUsesFirstMeaningfulParamPriorityForConflicts() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long conflictTaskId = createDeployTask(
        accessToken,
        "deploy-filter-conflict-priority",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParamsWithoutEnvironment());
    insertTaskParam(conflictTaskId, "environment", "dev");
    insertTaskParam(conflictTaskId, "profile", "prod");

    approveDeployTask(accessToken, conflictTaskId, "approve conflicting environment task");

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-filter-conflict-priority")
            .param("status", "PENDING")
            .param("environment", "sandbox")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) conflictTaskId))
        .andExpect(jsonPath("$.data.records[0].params.environment").value("dev"))
        .andExpect(jsonPath("$.data.records[0].params.profile").value("prod"));

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-filter-conflict-priority")
            .param("status", "PENDING")
            .param("environment", "production")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records.length()").value(0));
  }

  @Test
  void getDeployTasksTreatsRunningStatusFilterAsRunningLikeIncludingCancelRequested() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long runningTaskId = createDeployTask(accessToken, "deploy-running-filter-running");
    long cancelRequestedTaskId = createDeployTask(accessToken, "deploy-running-filter-cancel-requested");
    long failedTaskId = createDeployTask(accessToken, "deploy-running-filter-failed");
    long cancelledTaskId = createDeployTask(accessToken, "deploy-running-filter-cancelled");

    updateTaskCenterTaskSnapshot(runningTaskId, "RUNNING", 1, 0, "2026-04-16 16:00:00");
    updateTaskCenterTaskSnapshot(cancelRequestedTaskId, "CANCEL_REQUESTED", 1, 0, "2026-04-16 16:01:00");
    updateTaskCenterTaskSnapshot(failedTaskId, "FAILED", 1, 1, "2026-04-16 16:02:00");
    updateTaskCenterTaskSnapshot(cancelledTaskId, "CANCELLED", 1, 0, "2026-04-16 16:03:00");

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-running-filter-")
            .param("status", "RUNNING")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[*].id", containsInAnyOrder((int) runningTaskId, (int) cancelRequestedTaskId)))
        .andExpect(jsonPath("$.data.records[*].status", containsInAnyOrder("RUNNING", "CANCEL_REQUESTED")));

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-running-filter-")
            .param("status", "FAILED")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) failedTaskId))
        .andExpect(jsonPath("$.data.records[0].status").value("FAILED"));

    mockMvc.perform(get("/api/deploy/tasks")
            .param("keyword", "deploy-running-filter-")
            .param("status", "CANCELLED")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value((int) cancelledTaskId))
        .andExpect(jsonPath("$.data.records[0].status").value("CANCELLED"));
  }

  @Test
  void getDeployTasksRejectsInvalidSortBy() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("sortBy", "taskName")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("sortBy must be one of createdAt, updatedAt, taskNo, status"));
  }

  @Test
  void getDeployTasksRejectsInvalidSortOrder() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("sortOrder", "descending")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("sortOrder must be asc or desc"));
  }

  @Test
  void getDeployTasksRejectsNonPositivePage() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("page", "0")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("page must be greater than 0"));
  }

  @Test
  void getDeployTasksRejectsNonPositivePageSize() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("pageSize", "0")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("pageSize must be greater than 0"));
  }

  @Test
  void getDeployTasksRejectsInvalidPage() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("page", "abc")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("Invalid value for parameter 'page': abc"));
  }

  @Test
  void getDeployTasksRejectsInvalidAppId() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("appId", "x")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("Invalid value for parameter 'appId': x"));
  }

  @Test
  void getDeployTasksRejectsInvalidCreatedFrom() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks")
            .param("createdFrom", "bad-date")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("Invalid value for parameter 'createdFrom': bad-date"));
  }

  @Test
  void getUnifiedTaskCenterTasksRejectsInvalidPage() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/task-center/tasks")
            .param("page", "0")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("page must be greater than 0"));
  }

  @Test
  void getUnifiedTaskCenterTasksRejectsInvalidStatus() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/task-center/tasks")
            .param("status", "cancelled")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("status must be one of pending, running, success, failed"));
  }

  @Test
  void getUnifiedTaskCenterTasksRejectsInvalidStartedRange() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/task-center/tasks")
            .param("startedFrom", "2026-04-22T10:00:00")
            .param("startedTo", "2026-04-22T09:00:00")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("startedFrom must be before or equal to startedTo"));
  }

  @Test
  void getUnifiedTaskCenterTasksSupportsKeywordTaskTypeStatusAndStartedRange() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    jdbcTemplate.update(
        """
        INSERT INTO unified_task_center (
            id, task_type, task_name, status, triggered_by, started_at, finished_at,
            summary, detail_preview, source_id, source_route, module_name, error_summary,
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        9101L,
        "deploy",
        "task-center-deploy-alpha",
        "pending",
        "release-admin",
        java.sql.Timestamp.valueOf("2026-04-22 10:00:00"),
        null,
        "pending alpha rollout",
        "{\"app\":\"alpha\",\"sourceRoute\":\"/deploy/task?taskId=9101\"}",
        91001L,
        "/deploy/task?taskId=9101",
        "deploy",
        null,
        java.sql.Timestamp.valueOf("2026-04-22 10:00:00"),
        java.sql.Timestamp.valueOf("2026-04-22 10:00:00"));
    jdbcTemplate.update(
        """
        INSERT INTO unified_task_center (
            id, task_type, task_name, status, triggered_by, started_at, finished_at,
            summary, detail_preview, source_id, source_route, module_name, error_summary,
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        9102L,
        "database_connectivity",
        "task-center-database-beta",
        "failed",
        "envops-admin",
        java.sql.Timestamp.valueOf("2026-04-22 10:30:00"),
        java.sql.Timestamp.valueOf("2026-04-22 10:32:00"),
        "failed beta connectivity check",
        "{\"mode\":\"batch\",\"summary\":\"failed beta connectivity check\",\"sourceRoute\":\"/asset/database?keyword=beta\"}",
        91002L,
        "/asset/database?keyword=beta",
        "asset",
        "beta databases failed authentication",
        java.sql.Timestamp.valueOf("2026-04-22 10:30:00"),
        java.sql.Timestamp.valueOf("2026-04-22 10:30:00"));
    jdbcTemplate.update(
        """
        INSERT INTO unified_task_center (
            id, task_type, task_name, status, triggered_by, started_at, finished_at,
            summary, detail_preview, source_id, source_route, module_name, error_summary,
            created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        9103L,
        "traffic_action",
        "task-center-traffic-gamma",
        "running",
        "ops-worker",
        java.sql.Timestamp.valueOf("2026-04-22 11:00:00"),
        null,
        "running gamma traffic action",
        "{\"action\":\"apply\",\"sourceRoute\":\"/traffic/controller?policyId=3001\"}",
        91003L,
        "/traffic/controller?policyId=3001",
        "traffic",
        null,
        java.sql.Timestamp.valueOf("2026-04-22 11:00:00"),
        java.sql.Timestamp.valueOf("2026-04-22 11:00:00"));

    mockMvc.perform(get("/api/task-center/tasks")
            .param("keyword", "task-center")
            .param("page", "1")
            .param("pageSize", "2")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(2))
        .andExpect(jsonPath("$.data.total").value(3))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].id").value(9103))
        .andExpect(jsonPath("$.data.records[0].taskType").value("traffic_action"))
        .andExpect(jsonPath("$.data.records[0].status").value("running"))
        .andExpect(jsonPath("$.data.records[0].sourceRoute").value("/traffic/controller?policyId=3001"))
        .andExpect(jsonPath("$.data.records[1].id").value(9102));

    mockMvc.perform(get("/api/task-center/tasks")
            .param("keyword", "beta")
            .param("taskType", "database_connectivity")
            .param("status", "failed")
            .param("startedFrom", "2026-04-22T10:15:00")
            .param("startedTo", "2026-04-22T10:45:00")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records.length()").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value(9102))
        .andExpect(jsonPath("$.data.records[0].taskType").value("database_connectivity"))
        .andExpect(jsonPath("$.data.records[0].taskName").value("task-center-database-beta"))
        .andExpect(jsonPath("$.data.records[0].status").value("failed"))
        .andExpect(jsonPath("$.data.records[0].startedAt").value("2026-04-22T10:30:00"));
  }

  @Test
  void getUnifiedTaskCenterTaskDetailReturnsLightDrawerPayload() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    mockMvc.perform(get("/api/task-center/tasks/{id}", 9002L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value(9002))
        .andExpect(jsonPath("$.data.taskType").value("database_connectivity"))
        .andExpect(jsonPath("$.data.taskName").value("批量数据库连通性检测"))
        .andExpect(jsonPath("$.data.status").value("failed"))
        .andExpect(jsonPath("$.data.summary").value("批量检测 20 条，成功 16，失败 3，跳过 1"))
        .andExpect(jsonPath("$.data.startedAt").value("2026-04-21T09:00:00"))
        .andExpect(jsonPath("$.data.finishedAt").value("2026-04-21T09:02:00"))
        .andExpect(jsonPath("$.data.sourceRoute").value("/asset/database"))
        .andExpect(jsonPath("$.data.errorSummary").value("3 databases failed authentication"))
        .andExpect(jsonPath("$.data.detailPreview.mode").value("batch"))
        .andExpect(jsonPath("$.data.detailPreview.total").value(20))
        .andExpect(jsonPath("$.data.detailPreview.success").value(16))
        .andExpect(jsonPath("$.data.detailPreview.failed").value(3))
        .andExpect(jsonPath("$.data.detailPreview.skipped").value(1))
        .andExpect(jsonPath("$.data.detailPreview.sourceRoute").value("/asset/database"));
  }

  @Test
  void getUnifiedTaskCenterTaskDetailReturnsServerErrorWhenPreviewPayloadIsBroken() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    jdbcTemplate.update("UPDATE unified_task_center SET detail_preview = ? WHERE id = ?", "{broken-json", 9002L);

    mockMvc.perform(get("/api/task-center/tasks/{id}", 9002L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("500"))
        .andExpect(jsonPath("$.msg").value("Internal server error"));
  }

  @Test
  void getUnifiedTaskCenterTaskDetailReturnsNotFoundWhenMissing() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/task-center/tasks/{id}", 999999L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("unified task not found: 999999"));
  }

  @Test
  void createDeployTaskCreatesPendingUnifiedProjection() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    long taskId = createDeployTask(
        accessToken,
        "deploy-task-center-create",
        "INSTALL",
        1001L,
        1401L,
        List.of(1L),
        "ALL",
        0,
        createRequiredParams("production"));

    mockMvc.perform(get("/api/task-center/tasks")
            .param("taskType", "deploy")
            .param("keyword", "deploy-task-center-create")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].taskType").value("deploy"))
        .andExpect(jsonPath("$.data.records[0].status").value("pending"))
        .andExpect(jsonPath("$.data.records[0].sourceRoute").value("/deploy/task?taskId=" + taskId));

    Long unifiedTaskId = jdbcTemplate.queryForObject(
        "SELECT id FROM unified_task_center WHERE task_type = ? AND source_id = ?",
        Long.class,
        "deploy",
        taskId);
    org.assertj.core.api.Assertions.assertThat(unifiedTaskId).isNotNull();

    mockMvc.perform(get("/api/task-center/tasks/{id}", unifiedTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.taskType").value("deploy"))
        .andExpect(jsonPath("$.data.taskName").value("deploy-task-center-create"))
        .andExpect(jsonPath("$.data.status").value("pending"))
        .andExpect(jsonPath("$.data.sourceRoute").value("/deploy/task?taskId=" + taskId))
        .andExpect(jsonPath("$.data.detailPreview.app").value("订单服务"))
        .andExpect(jsonPath("$.data.detailPreview.environment").value("production"))
        .andExpect(jsonPath("$.data.detailPreview.rawStatus").value("PENDING_APPROVAL"));
  }

  @Test
  void backfillsDeployRowsMissingUnifiedProjection() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    jdbcTemplate.update("DELETE FROM unified_task_center WHERE task_type = ? AND source_id = ?", "deploy", 2001L);
    unifiedTaskCenterBackfillRunner.run(new DefaultApplicationArguments(new String[0]));

    mockMvc.perform(get("/api/task-center/tasks")
            .param("taskType", "deploy")
            .param("keyword", "seed-order-service-install")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].taskType").value("deploy"))
        .andExpect(jsonPath("$.data.records[0].status").value("success"))
        .andExpect(jsonPath("$.data.records[0].sourceRoute").value("/deploy/task?taskId=2001"));
  }

  @Test
  void executeDeployTaskUpdatesUnifiedProjectionToRunningThenFinished() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueBlockingExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-02");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-02");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-02");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-02");

    long taskId = createDeployTask(
        accessToken,
        "execute-unified-projection-order-service-prod",
        versionId,
        List.of(1L, 2L),
        Map.of(
            "environment", "production",
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for unified projection execution test");

    CompletableFuture<Void> executeFuture = CompletableFuture.runAsync(() -> {
      try {
        mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0000"))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));
      } catch (Exception exception) {
        throw new CompletionException(exception);
      }
    });

    fakeSshProcessRunner.awaitBlockingExecStarted();

    Long unifiedTaskId = jdbcTemplate.queryForObject(
        "SELECT id FROM unified_task_center WHERE task_type = ? AND source_id = ?",
        Long.class,
        "deploy",
        taskId);
    org.assertj.core.api.Assertions.assertThat(unifiedTaskId).isNotNull();

    mockMvc.perform(get("/api/task-center/tasks/{id}", unifiedTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("running"))
        .andExpect(jsonPath("$.data.detailPreview.rawStatus").value("RUNNING"));

    fakeSshProcessRunner.releaseBlockingExec();
    executeFuture.get(10, TimeUnit.SECONDS);

    mockMvc.perform(get("/api/task-center/tasks")
            .param("taskType", "deploy")
            .param("keyword", "execute-unified-projection-order-service-prod")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].status").value("success"))
        .andExpect(jsonPath("$.data.records[0].sourceRoute").value("/deploy/task?taskId=" + taskId));

    mockMvc.perform(get("/api/task-center/tasks/{id}", unifiedTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("success"))
        .andExpect(jsonPath("$.data.detailPreview.app").value("订单服务"))
        .andExpect(jsonPath("$.data.detailPreview.environment").value("production"))
        .andExpect(jsonPath("$.data.detailPreview.targetCount").value(2))
        .andExpect(jsonPath("$.data.detailPreview.successCount").value(2))
        .andExpect(jsonPath("$.data.detailPreview.failCount").value(0))
        .andExpect(jsonPath("$.data.detailPreview.rawStatus").value("SUCCESS"));
  }

  @Test
  void approveDeployTaskTransitionsToPendingAndStoresApprovalAuditFields() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "approve-order-service-prod");

    mockMvc.perform(post("/api/deploy/tasks/{id}/approve", taskId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "approved for release window"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.approvalOperatorName").value("release-admin"))
        .andExpect(jsonPath("$.data.approvalComment").value("approved for release window"))
        .andExpect(jsonPath("$.data.approvalAt", not(blankOrNullString())));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("PENDING");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_operator_name FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("release-admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_comment FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("approved for release window");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.approvalOperatorName").value("release-admin"))
        .andExpect(jsonPath("$.data.approvalComment").value("approved for release window"))
        .andExpect(jsonPath("$.data.approvalAt", not(blankOrNullString())));

  }

  @Test
  void rejectDeployTaskTransitionsToRejectedAndStoresApprovalAuditFields() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "reject-order-service-prod");

    mockMvc.perform(post("/api/deploy/tasks/{id}/reject", taskId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "missing maintenance approval"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("REJECTED"))
        .andExpect(jsonPath("$.data.approvalOperatorName").value("release-admin"))
        .andExpect(jsonPath("$.data.approvalComment").value("missing maintenance approval"))
        .andExpect(jsonPath("$.data.approvalAt", not(blankOrNullString())));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("REJECTED");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_operator_name FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("release-admin");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_comment FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("missing maintenance approval");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT approval_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("REJECTED"))
        .andExpect(jsonPath("$.data.approvalOperatorName").value("release-admin"))
        .andExpect(jsonPath("$.data.approvalComment").value("missing maintenance approval"))
        .andExpect(jsonPath("$.data.approvalAt", not(blankOrNullString())));

  }

  @Test
  void updateApprovalDecisionRequiresPendingApprovalStatus() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "mapper-approval-guard");

    jdbcTemplate.update("UPDATE deploy_task SET status = 'PENDING' WHERE id = ?", taskId);

    DeployTaskMapper.DeployTaskEntity entity = new DeployTaskMapper.DeployTaskEntity();
    entity.setId(taskId);
    entity.setStatus("REJECTED");
    entity.setApprovalOperatorName("release-admin");
    entity.setApprovalComment("should be blocked");
    entity.setApprovalAt(java.time.LocalDateTime.now());
    entity.setUpdatedBy("release-admin");
    entity.setUpdatedAt(java.time.LocalDateTime.now());

    org.assertj.core.api.Assertions.assertThat(deployTaskMapper.updateApprovalDecision(entity)).isZero();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("PENDING");
  }

  @Test
  void approveOrRejectDeployTaskOutsidePendingApprovalReturnsConflict() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks/{id}/approve", 2001L)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "try approve"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("deploy task is not pending approval: 2001"));

    mockMvc.perform(post("/api/deploy/tasks/{id}/reject", 2001L)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "try reject"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("deploy task is not pending approval: 2001"));
  }

  @Test
  void createDeployTaskRejectsMissingExecutionParams() throws Exception {
    String accessToken = login("envops-admin", "EnvOps@123");

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "invalid-order-service",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "environment": "production",
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {
                    "remoteBaseDir": "/opt/envops/releases"
                  }
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.msg").value("sshUser is required"));
  }

  @Test
  void createDeployTaskRejectsInvalidTaskType() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "invalid-task-type",
                  "taskType": "DEPLOY",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {}
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("taskType must be one of INSTALL, UPGRADE, ROLLBACK"));
  }

  @Test
  void createDeployTaskRejectsRollbackTaskTypeFromPublicCreateApi() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "public-rollback-create",
                  "taskType": "ROLLBACK",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {
                    "rollbackCommand": "echo rollback-direct"
                  }
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("taskType ROLLBACK must be created via rollback API"));
  }

  @Test
  void createDeployTaskRejectsEmptyHostIds() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "empty-hosts",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [],
                  "batchStrategy": "ALL",
                  "params": {}
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("hostIds must not be empty"));
  }

  @Test
  void createDeployTaskRejectsVersionAppMismatch() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "mismatch-version-app",
                  "taskType": "UPGRADE",
                  "appId": 1001,
                  "versionId": 1402,
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {}
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("versionId does not belong to appId"));
  }

  @Test
  void createDeployTaskRejectsInvalidBatchStrategy() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "invalid-batch-strategy",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "CANARY",
                  "params": {}
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("batchStrategy must be one of ALL, ROLLING"));
  }

  @Test
  void createDeployTaskRejectsNonPositiveRollingBatchSize() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "rolling-batch-size-invalid",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "ROLLING",
                  "batchSize": 0,
                  "params": {}
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("batchSize must be greater than 0 when batchStrategy is ROLLING"));
  }

  @Test
  void createDeployTaskIgnoresLegacyParamsOutsidePublicContract() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    MvcResult createResult = mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "install-ignore-legacy-create-params",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {
                    "environment": "production",
                    "deployDir": "/data/apps/order-service",
                    "profile": "prod",
                    "accessToken": "prod-secret-token",
                    "sshUser": "deploy",
                    "sshPort": 22,
                    "privateKeyPath": "%s",
                    "remoteBaseDir": "/opt/envops/releases",
                    "rollbackCommand": "bash /opt/envops/bin/rollback.sh"
                  }
                }
                """.formatted(TEST_PRIVATE_KEY_PATH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.params.environment").value("production"))
        .andExpect(jsonPath("$.data.params.sshUser").value("deploy"))
        .andExpect(jsonPath("$.data.params.sshPort").value("22"))
        .andExpect(jsonPath("$.data.params.remoteBaseDir").value("/opt/envops/releases"))
        .andExpect(jsonPath("$.data.params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.params.accessToken").doesNotExist())
        .andReturn();

    long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_param WHERE task_id = ? AND param_key = ?", Integer.class, taskId, "deployDir"))
        .isEqualTo(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_param WHERE task_id = ? AND param_key = ?", Integer.class, taskId, "profile"))
        .isEqualTo(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task_param WHERE task_id = ? AND param_key = ?", Integer.class, taskId, "accessToken"))
        .isEqualTo(0);

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.params.accessToken").doesNotExist());

    mockMvc.perform(get("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.records[0].id").value((int) taskId))
        .andExpect(jsonPath("$.data.records[0].params.deployDir").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.profile").doesNotExist())
        .andExpect(jsonPath("$.data.records[0].params.accessToken").doesNotExist());
  }

  @Test
  void deployTaskCreationRequiresAuthenticatedOperator() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> deployTaskApplicationService.createDeployTask(
            new DeployTaskApplicationService.CreateDeployTaskCommand(
                "missing-operator",
                "INSTALL",
                1001L,
                1401L,
                java.util.List.of(1L),
                "ALL",
                0,
                java.util.Map.of(
                    "sshUser", "deploy",
                    "privateKeyPath", "/tmp/key.pem",
                    "remoteBaseDir", "/tmp/app")),
            null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("operatorName is required");
  }

  @Test
  void deployTaskReadModelStillHidesSensitiveKeysWhenSecretFlagIsMissing() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "install-with-history-secret-flag-gap");

    jdbcTemplate.update(
        "INSERT INTO deploy_task_param (task_id, param_key, param_value, secret_flag) VALUES (?, ?, ?, ?)",
        taskId,
        "access_key",
        "should-stay-hidden",
        1);
    jdbcTemplate.update(
        "INSERT INTO deploy_task_param (task_id, param_key, param_value, secret_flag) VALUES (?, ?, ?, ?)",
        taskId,
        "profile",
        "prod",
        0);
    jdbcTemplate.update("UPDATE deploy_task_param SET secret_flag = 0 WHERE task_id = ? AND param_key = ?", taskId, "access_key");

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.params.access_key").doesNotExist())
        .andExpect(jsonPath("$.data.params.profile").value("prod"));
  }

  @Test
  void deployTaskReadModelDoesNotExposeRollbackCommandInDetailOrListApis() throws Exception {
    String accessToken = login("release-admin", "Release@123");

    MvcResult createResult = mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "install-with-hidden-rollback-command",
                  "taskType": "INSTALL",
                  "appId": 1001,
                  "versionId": 1401,
                  "hostIds": [1],
                  "batchStrategy": "ALL",
                  "params": {
                    "environment": "production",
                    "sshUser": "deploy",
                    "sshPort": 22,
                    "privateKeyPath": "%s",
                    "remoteBaseDir": "/opt/envops/releases",
                    "rollbackCommand": "echo rollback-direct"
                  }
                }
                """.formatted(TEST_PRIVATE_KEY_PATH)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    long taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.params.environment").value("production"))
        .andExpect(jsonPath("$.data.params.rollbackCommand").doesNotExist());

    MvcResult listResult = mockMvc.perform(get("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode tasks = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data").path("records");
    JsonNode createdTask = null;
    for (JsonNode task : tasks) {
      if (task.path("id").asLong() == taskId) {
        createdTask = task;
        break;
      }
    }

    org.assertj.core.api.Assertions.assertThat(createdTask).isNotNull();
    org.assertj.core.api.Assertions.assertThat(createdTask.path("params").path("environment").asText())
        .isEqualTo("production");
    org.assertj.core.api.Assertions.assertThat(createdTask.path("params").has("rollbackCommand")).isFalse();
  }

  @Test
  void deployTaskEndpointsReturn404WhenTaskMissing() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/tasks/{id}", 999999L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("deploy task not found: 999999"));

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", 999999L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("deploy task not found: 999999"));

    mockMvc.perform(get("/api/deploy/tasks/{id}/logs", 999999L)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("deploy task not found: 999999"));

    mockMvc.perform(post("/api/deploy/tasks/{id}/approve", 999999L)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "approve missing task"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("deploy task not found: 999999"));

    mockMvc.perform(post("/api/deploy/tasks/{id}/reject", 999999L)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "comment": "reject missing task"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("deploy task not found: 999999"));
  }

  @Test
  void executeDeployTaskRunsApprovedTaskAndPersistsHostLogs() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-02");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-02");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-02");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-02");

    long taskId = createDeployTask(
        accessToken,
        "execute-order-service-prod",
        versionId,
        List.of(1L, 2L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for execution");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.successCount").value(2))
        .andExpect(jsonPath("$.data.failCount").value(0));

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].hostId").value(1))
        .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.records[1].hostId").value(2))
        .andExpect(jsonPath("$.data.records[1].status").value("SUCCESS"));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("SUCCESS");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT success_count FROM deploy_task WHERE id = ?", Integer.class, taskId))
        .isEqualTo(2);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT fail_count FROM deploy_task WHERE id = ?", Integer.class, taskId))
        .isEqualTo(0);

    List<String> logContents = getDeployTaskLogContents(accessToken, taskId);
    org.assertj.core.api.Assertions.assertThat(logContents)
        .anyMatch(log -> log.toLowerCase().contains("upload"))
        .anyMatch(log -> log.toLowerCase().contains("exec") || log.toLowerCase().contains("run"));
  }

  @Test
  void cancelDeployTaskStopsQueuedHostsAndMarksTaskCancelled() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueBlockingExecSuccess("exec script host-prd-01");

    long taskId = createDeployTask(
        accessToken,
        "cancel-order-service-prod",
        versionId,
        List.of(1L, 2L),
        "ROLLING",
        1,
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for cancellation test");

    CompletableFuture<Void> executeFuture = CompletableFuture.runAsync(() -> {
      try {
        mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
      } catch (Exception exception) {
        throw new CompletionException(exception);
      }
    });

    fakeSshProcessRunner.awaitBlockingExecStarted();

    mockMvc.perform(post("/api/deploy/tasks/{id}/cancel", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"));

    fakeSshProcessRunner.releaseBlockingExec();
    executeFuture.get(10, TimeUnit.SECONDS);

    mockMvc.perform(get("/api/deploy/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        .andExpect(jsonPath("$.data.successCount").value(1))
        .andExpect(jsonPath("$.data.failCount").value(0));

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].hostId").value(1))
        .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.records[1].hostId").value(2))
        .andExpect(jsonPath("$.data.records[1].status").value("CANCELLED"));

    List<String> logContents = getDeployTaskLogContents(accessToken, taskId);
    org.assertj.core.api.Assertions.assertThat(logContents)
        .anyMatch(log -> log.toLowerCase().contains("cancel"));
  }

  @Test
  void retryDeployTaskResetsFailedHostsAndRerunsTheSameTask() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-02");
    fakeSshProcessRunner.queueUploadFailure("upload package host-prd-02 failed");

    long taskId = createDeployTask(
        accessToken,
        "retry-order-service-prod",
        versionId,
        List.of(1L, 2L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for retry test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("FAILED"))
        .andExpect(jsonPath("$.data.successCount").value(1))
        .andExpect(jsonPath("$.data.failCount").value(1));

    Integer taskCountBeforeRetry = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task", Integer.class);
    List<Long> hostRowIdsBeforeRetry = jdbcTemplate.queryForList("SELECT id FROM deploy_task_host WHERE task_id = ? ORDER BY id", Long.class, taskId);

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-02 retry");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-02 retry");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-02 retry");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-02 retry");

    mockMvc.perform(post("/api/deploy/tasks/{id}/retry", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) taskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.successCount").value(2))
        .andExpect(jsonPath("$.data.failCount").value(0));

    mockMvc.perform(get("/api/deploy/tasks/{id}/hosts", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(10))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.records.length()").value(2))
        .andExpect(jsonPath("$.data.records[0].hostId").value(1))
        .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.records[1].hostId").value(2))
        .andExpect(jsonPath("$.data.records[1].status").value("SUCCESS"));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task", Integer.class))
        .isEqualTo(taskCountBeforeRetry);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForList("SELECT id FROM deploy_task_host WHERE task_id = ? ORDER BY id", Long.class, taskId))
        .containsExactlyElementsOf(hostRowIdsBeforeRetry);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT error_msg FROM deploy_task_host WHERE task_id = ? AND host_id = ?", String.class, taskId, 2L))
        .isNull();

    List<String> logContents = getDeployTaskLogContents(accessToken, taskId);
    org.assertj.core.api.Assertions.assertThat(logContents)
        .contains("Task retried");
  }

  @Test
  void rollbackDeployTaskCreatesChildRollbackTaskAndRunsRollbackAction() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("rollback direct command completed");

    long originalTaskId = createDeployTask(
        accessToken,
        "rollback-order-service-prod",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases",
            "rollbackCommand", "echo rollback-direct"));
    approveDeployTask(accessToken, originalTaskId, "approved for rollback test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) originalTaskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    Integer taskCountBeforeRollback = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task", Integer.class);

    MvcResult rollbackResult = mockMvc.perform(post("/api/deploy/tasks/{id}/rollback", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.id").value(not((int) originalTaskId)))
        .andExpect(jsonPath("$.data.taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.originTaskId").value((int) originalTaskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andReturn();

    long rollbackTaskId = objectMapper.readTree(rollbackResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    mockMvc.perform(get("/api/deploy/tasks/{id}", rollbackTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) rollbackTaskId))
        .andExpect(jsonPath("$.data.taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.originTaskId").value((int) originalTaskId));

    mockMvc.perform(get("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.records[0].id").value((int) rollbackTaskId))
        .andExpect(jsonPath("$.data.records[0].taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.records[0].originTaskId").value((int) originalTaskId));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deploy_task", Integer.class))
        .isEqualTo(taskCountBeforeRollback + 1);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, originalTaskId))
        .isEqualTo("SUCCESS");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT task_type FROM deploy_task WHERE id = ?", String.class, rollbackTaskId))
        .isEqualTo("ROLLBACK");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT origin_task_id FROM deploy_task WHERE id = ?", Long.class, rollbackTaskId))
        .isEqualTo(originalTaskId);
    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.lastExecutedCommand())
        .isEqualTo("echo rollback-direct");

    List<String> rollbackLogs = getDeployTaskLogContents(accessToken, rollbackTaskId);
    org.assertj.core.api.Assertions.assertThat(rollbackLogs)
        .anyMatch(log -> log.contains("Rollback task created from " + originalTaskId))
        .anyMatch(log -> log.contains("Executing rollback command"))
        .noneMatch(log -> log.contains("echo rollback-direct"));
  }

  @Test
  void rollbackDeployTaskWithRollbackCommandDoesNotRequireOriginalPackageOrScriptMetadata() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("rollback direct command completed");

    long originalTaskId = createDeployTask(
        accessToken,
        "rollback-missing-metadata-order-service-prod",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases",
            "rollbackCommand", "echo rollback-direct"));
    approveDeployTask(accessToken, originalTaskId, "approved for rollback missing metadata test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    jdbcTemplate.update("UPDATE app_version SET package_id = NULL, script_template_id = NULL WHERE id = ?", versionId);

    MvcResult rollbackResult = mockMvc.perform(post("/api/deploy/tasks/{id}/rollback", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.originTaskId").value((int) originalTaskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andReturn();

    long rollbackTaskId = objectMapper.readTree(rollbackResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.lastExecutedCommand())
        .isEqualTo("echo rollback-direct");
    org.assertj.core.api.Assertions.assertThat(getDeployTaskLogContents(accessToken, rollbackTaskId))
        .anyMatch(log -> log.contains("Executing rollback command"))
        .noneMatch(log -> log.contains("echo rollback-direct"));
  }

  @Test
  void rollbackDeployTaskRejectsRollbackParentTask() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("rollback direct command completed");

    long originalTaskId = createDeployTask(
        accessToken,
        "rollback-parent-guard-order-service-prod",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases",
            "rollbackCommand", "echo rollback-direct"));
    approveDeployTask(accessToken, originalTaskId, "approved for rollback parent guard test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    MvcResult rollbackResult = mockMvc.perform(post("/api/deploy/tasks/{id}/rollback", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andReturn();

    long rollbackTaskId = objectMapper.readTree(rollbackResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    mockMvc.perform(post("/api/deploy/tasks/{id}/rollback", rollbackTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("rollback task cannot be rolled back: " + rollbackTaskId));
  }

  @Test
  void rollbackDeployTaskWithoutRollbackCommandFallsBackToDeployScriptWithRollbackAction() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01 install");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01 install");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01 install");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01 install");
    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory host-prd-01 rollback");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01 rollback");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01 rollback");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01 rollback");

    long originalTaskId = createDeployTask(
        accessToken,
        "rollback-fallback-order-service-prod",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, originalTaskId, "approved for rollback fallback test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value((int) originalTaskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    MvcResult rollbackResult = mockMvc.perform(post("/api/deploy/tasks/{id}/rollback", originalTaskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.id").value(not((int) originalTaskId)))
        .andExpect(jsonPath("$.data.taskType").value("ROLLBACK"))
        .andExpect(jsonPath("$.data.originTaskId").value((int) originalTaskId))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andReturn();

    long rollbackTaskId = objectMapper.readTree(rollbackResult.getResponse().getContentAsString()).path("data").path("id").asLong();

    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.lastExecutedCommand())
        .contains("ENVOPS_TASK_ACTION=ROLLBACK")
        .contains("ENVOPS_PACKAGE_PATH='")
        .contains("bash '");

    List<String> rollbackLogs = getDeployTaskLogContents(accessToken, rollbackTaskId);
    org.assertj.core.api.Assertions.assertThat(rollbackLogs)
        .anyMatch(log -> log.contains("Executing deploy script with action ROLLBACK"));
  }

  @Test
  void executeDeployTaskMarksTaskFailedWhenContextBuildThrows() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    long taskId = createDeployTask(
        accessToken,
        "execute-missing-ssh-user",
        versionId,
        List.of(1L),
        createRequiredParams());
    approveDeployTask(accessToken, taskId, "approved for missing ssh user test");
    jdbcTemplate.update("DELETE FROM deploy_task_param WHERE task_id = ? AND param_key = ?", taskId, "sshUser");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("sshUser is required"));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("FAILED");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT started_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT finished_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT success_count FROM deploy_task WHERE id = ?", Integer.class, taskId))
        .isEqualTo(0);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT fail_count FROM deploy_task WHERE id = ?", Integer.class, taskId))
        .isEqualTo(0);
  }

  @Test
  void executeDeployTaskPreparesRemoteReleaseDirectoryBeforeUploadingFiles() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");

    long taskId = createDeployTask(
        accessToken,
        "prepare-remote-release-dir",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for remote dir preparation test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.executedCommands())
        .hasSize(2);
    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.executedCommands().get(0))
        .contains("mkdir -p")
        .contains("/opt/envops/releases/order-service/");
    org.assertj.core.api.Assertions.assertThat(fakeSshProcessRunner.uploadTargets())
        .hasSize(2)
        .allMatch(target -> target.startsWith("/opt/envops/releases/order-service/"));
  }

  @Test
  void executeDeployTaskShellEscapesRemotePathsInExecCommand() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersionWithVersionNo("2026-04 demo's build");

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");

    long taskId = createDeployTask(
        accessToken,
        "escape-remote-paths",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for shell escaping test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    String execCommand = fakeSshProcessRunner.executedCommands().get(1);
    org.assertj.core.api.Assertions.assertThat(execCommand)
        .contains("ENVOPS_TASK_ACTION=INSTALL")
        .contains("ENVOPS_PACKAGE_PATH='")
        .contains("bash '")
        .contains("'\"'\"'");
    org.assertj.core.api.Assertions.assertThat(Pattern.compile("ENVOPS_PACKAGE_PATH='[^']*'\\\"'\\\"'[^']*' bash '").matcher(execCommand).find())
        .isTrue();
  }

  @Test
  void executeDeployTaskShellEscapesRenderedScriptPackagePlaceholder() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersionWithVersionNoAndScript(
        "2026-04 quoted demo's build",
        "#!/usr/bin/env bash\njava -jar {{package}}\necho raw={{packagePath}}");

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory");
    fakeSshProcessRunner.queueUploadSuccess("uploaded package host-prd-01");
    fakeSshProcessRunner.queueUploadSuccess("uploaded script host-prd-01");
    fakeSshProcessRunner.queueExecSuccess("exec script host-prd-01");

    long taskId = createDeployTask(
        accessToken,
        "escape-rendered-script-paths",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for rendered script shell escaping test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("SUCCESS"));

    String renderedScript = Files.readString(Path.of(fakeSshProcessRunner.uploadSourcePaths().get(1)));
    org.assertj.core.api.Assertions.assertThat(renderedScript)
        .contains("java -jar '/opt/envops/releases/order-service/2026-04 quoted demo'\"'\"'s build/demo-" + versionId + ".jar'")
        .contains("echo raw=/opt/envops/releases/order-service/2026-04 quoted demo's build/demo-" + versionId + ".jar");
  }

  @Test
  void cancelDeployTaskWhilePendingFinalizesCancelledTaskAndHostsWithoutStartedAt() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "cancel-pending-order-service", 1401L, List.of(1L, 2L), createRequiredParams());
    approveDeployTask(accessToken, taskId, "approved for pending cancel test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/cancel", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("CANCELLED");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT started_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT finished_at FROM deploy_task WHERE id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForList("SELECT status FROM deploy_task_host WHERE task_id = ? ORDER BY id", String.class, taskId))
        .containsExactly("CANCELLED", "CANCELLED");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForList("SELECT started_at FROM deploy_task_host WHERE task_id = ? ORDER BY id", java.sql.Timestamp.class, taskId))
        .allMatch(java.util.Objects::isNull);
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForList("SELECT finished_at FROM deploy_task_host WHERE task_id = ? ORDER BY id", java.sql.Timestamp.class, taskId))
        .allMatch(java.util.Objects::nonNull);
  }

  @Test
  void updateExecutionSummaryDoesNotOverwriteCancelRequestedTask() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long taskId = createDeployTask(accessToken, "execution-summary-cas-guard");
    approveDeployTask(accessToken, taskId, "approved for cas guard test");

    DeployTaskMapper.DeployTaskEntity running = new DeployTaskMapper.DeployTaskEntity();
    running.setId(taskId);
    running.setStartedAt(java.time.LocalDateTime.now());
    running.setUpdatedBy("release-admin");
    running.setUpdatedAt(java.time.LocalDateTime.now());
    org.assertj.core.api.Assertions.assertThat(deployTaskMapper.markRunning(running)).isEqualTo(1);

    DeployTaskMapper.DeployTaskEntity cancelRequested = new DeployTaskMapper.DeployTaskEntity();
    cancelRequested.setId(taskId);
    cancelRequested.setUpdatedBy("release-admin");
    cancelRequested.setUpdatedAt(java.time.LocalDateTime.now());
    org.assertj.core.api.Assertions.assertThat(deployTaskMapper.markCancelRequested(cancelRequested)).isEqualTo(1);

    DeployTaskMapper.DeployTaskEntity finish = new DeployTaskMapper.DeployTaskEntity();
    finish.setId(taskId);
    finish.setStatus("SUCCESS");
    finish.setSuccessCount(1);
    finish.setFailCount(0);
    finish.setFinishedAt(java.time.LocalDateTime.now());
    finish.setUpdatedBy("release-admin");
    finish.setUpdatedAt(java.time.LocalDateTime.now());

    org.assertj.core.api.Assertions.assertThat(deployTaskMapper.updateExecutionSummary(finish)).isZero();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task WHERE id = ?", String.class, taskId))
        .isEqualTo("CANCEL_REQUESTED");
  }

  @Test
  void executeDeployTaskPreservesHostStartedAtWhenHostFails() throws Exception {
    String accessToken = login("release-admin", "Release@123");
    long versionId = createExecutableVersion();

    fakeSshProcessRunner.queueExecSuccess("prepared remote release directory");
    fakeSshProcessRunner.queueUploadFailure("upload package failed");

    long taskId = createDeployTask(
        accessToken,
        "host-failure-preserves-started-at",
        versionId,
        List.of(1L),
        Map.of(
            "sshUser", "deploy",
            "sshPort", 22,
            "privateKeyPath", TEST_PRIVATE_KEY_PATH,
            "remoteBaseDir", "/opt/envops/releases"));
    approveDeployTask(accessToken, taskId, "approved for host failure test");

    mockMvc.perform(post("/api/deploy/tasks/{id}/execute", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("FAILED"))
        .andExpect(jsonPath("$.data.successCount").value(0))
        .andExpect(jsonPath("$.data.failCount").value(1));

    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT status FROM deploy_task_host WHERE task_id = ?", String.class, taskId))
        .isEqualTo("FAILED");
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT started_at FROM deploy_task_host WHERE task_id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT finished_at FROM deploy_task_host WHERE task_id = ?", java.sql.Timestamp.class, taskId))
        .isNotNull();
    org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT error_msg FROM deploy_task_host WHERE task_id = ?", String.class, taskId))
        .isEqualTo("upload package failed");
  }

  private Map<String, Object> createRequiredParams() {
    return createRequiredParams("production");
  }

  private Map<String, Object> createRequiredParamsWithoutEnvironment() {
    return createRequiredParams(null);
  }

  private Map<String, Object> createRequiredParams(String environment) {
    Map<String, Object> params = new LinkedHashMap<>();
    if (environment != null) {
      params.put("environment", environment);
    }
    params.put("sshUser", "deploy");
    params.put("sshPort", 22);
    params.put("privateKeyPath", TEST_PRIVATE_KEY_PATH);
    params.put("remoteBaseDir", "/opt/envops/releases");
    return params;
  }

  private void insertTaskParam(long taskId, String key, String value) {
    jdbcTemplate.update(
        "INSERT INTO deploy_task_param (task_id, param_key, param_value, secret_flag) VALUES (?, ?, ?, ?)",
        taskId,
        key,
        value,
        0);
  }

  private long createDeployTask(String accessToken, String taskName) throws Exception {
    return createDeployTask(accessToken, taskName, "INSTALL", 1001L, 1401L, List.of(1L), "ALL", 0, createRequiredParams());
  }

  private long createDeployTask(String accessToken,
                                String taskName,
                                Long versionId,
                                List<Long> hostIds,
                                Map<String, Object> params) throws Exception {
    return createDeployTask(accessToken, taskName, "INSTALL", 1001L, versionId, hostIds, "ALL", 0, params);
  }

  private long createDeployTask(String accessToken,
                                String taskName,
                                Long versionId,
                                List<Long> hostIds,
                                String batchStrategy,
                                Integer batchSize,
                                Map<String, Object> params) throws Exception {
    return createDeployTask(accessToken, taskName, "INSTALL", 1001L, versionId, hostIds, batchStrategy, batchSize, params);
  }

  private long createDeployTask(String accessToken,
                                String taskName,
                                String taskType,
                                Long appId,
                                Long versionId,
                                List<Long> hostIds,
                                String batchStrategy,
                                Integer batchSize,
                                Map<String, Object> params) throws Exception {
    MvcResult createResult = mockMvc.perform(post("/api/deploy/tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "taskName", taskName,
                "taskType", taskType,
                "appId", appId,
                "versionId", versionId,
                "hostIds", hostIds,
                "batchStrategy", batchStrategy,
                "batchSize", batchSize,
                "params", params))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    return objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();
  }

  private void approveDeployTask(String accessToken, long taskId, String comment) throws Exception {
    mockMvc.perform(post("/api/deploy/tasks/{id}/approve", taskId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("comment", comment))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  private void updateDeployTaskTimestamps(long taskId, String createdAt, String updatedAt) {
    jdbcTemplate.update(
        "UPDATE deploy_task SET created_at = ?, updated_at = ? WHERE id = ?",
        java.sql.Timestamp.valueOf(createdAt),
        java.sql.Timestamp.valueOf(updatedAt),
        taskId);
  }

  private void updateTaskCenterTaskSnapshot(long taskId,
                                            String status,
                                            int targetCount,
                                            int failCount,
                                            String updatedAt) {
    jdbcTemplate.update(
        "UPDATE deploy_task SET status = ?, target_count = ?, fail_count = ?, success_count = ?, updated_at = ? WHERE id = ?",
        status,
        targetCount,
        failCount,
        Math.max(targetCount - failCount, 0),
        java.sql.Timestamp.valueOf(updatedAt),
        taskId);
  }

  private List<String> getDeployTaskLogContents(String accessToken, long taskId) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/deploy/tasks/{id}/logs", taskId)
            .param("pageSize", "200")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode logs = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("records");
    List<String> contents = new ArrayList<>();
    for (JsonNode log : logs) {
      contents.add(log.path("logContent").asText());
    }
    return contents;
  }

  private long createExecutableVersion() throws Exception {
    long baseId = EXECUTABLE_VERSION_SEQUENCE.incrementAndGet() * 10;
    return createExecutableVersion(baseId, "1.0." + baseId + "-test");
  }

  private long createExecutableVersionWithVersionNo(String versionNo) throws Exception {
    long baseId = EXECUTABLE_VERSION_SEQUENCE.incrementAndGet() * 10;
    return createExecutableVersion(baseId, versionNo);
  }

  private long createExecutableVersionWithVersionNoAndScript(String versionNo, String scriptContent) throws Exception {
    long baseId = EXECUTABLE_VERSION_SEQUENCE.incrementAndGet() * 10;
    return createExecutableVersion(baseId, versionNo, scriptContent);
  }

  private long createExecutableVersion(long baseId, String versionNo) throws Exception {
    return createExecutableVersion(baseId, versionNo, "#!/usr/bin/env bash\necho action=$ENVOPS_TASK_ACTION\necho package=$ENVOPS_PACKAGE_PATH");
  }

  private long createExecutableVersion(long baseId, String versionNo, String scriptContent) throws Exception {
    long packageId = baseId + 1;
    long scriptTemplateId = baseId + 2;
    long versionId = baseId + 3;
    String relativePackagePath = "packages/test/demo-" + versionId + ".jar";
    Path storagePath = Paths.get(storageBaseDir).toAbsolutePath().normalize();
    Path packagePath = storagePath.resolve(relativePackagePath).normalize();
    Files.createDirectories(packagePath.getParent());
    Files.writeString(packagePath, "demo-package-" + versionId);

    jdbcTemplate.update("DELETE FROM app_version WHERE id = ?", versionId);
    jdbcTemplate.update("DELETE FROM app_script_template WHERE id = ?", scriptTemplateId);
    jdbcTemplate.update("DELETE FROM app_package WHERE id = ?", packageId);

    jdbcTemplate.update("INSERT INTO app_package (id, package_name, package_type, file_path, file_size, file_hash, storage_type, deleted, created_by, updated_by, created_at, updated_at) VALUES (?, ?, 'JAR', ?, ?, ?, 'LOCAL', 0, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        packageId,
        "demo-" + versionId + ".jar",
        relativePackagePath,
        Files.size(packagePath),
        "sha256:test-" + versionId);
    jdbcTemplate.update("INSERT INTO app_script_template (id, template_code, template_name, script_type, script_content, deleted, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, 'BASH', ?, 0, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        scriptTemplateId,
        "deploy-script-" + versionId,
        "deploy-script-" + versionId,
        scriptContent);
    jdbcTemplate.update("INSERT INTO app_version (id, app_id, version_no, package_id, script_template_id, changelog, status, deleted, created_by, updated_by, created_at, updated_at) VALUES (?, 1001, ?, ?, ?, 'test version', 1, 0, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        versionId,
        versionNo,
        packageId,
        scriptTemplateId);
    return versionId;
  }

  private String login() throws Exception {
    return login("envops-admin", "EnvOps@123");
  }

  private String login(String userName, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "%s",
                  "password": "%s"
                }
                """.formatted(userName, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  @TestConfiguration
  static class TestBeans {
    @Bean
    FakeSshProcessRunner fakeSshProcessRunner() {
      return new FakeSshProcessRunner();
    }

    @Bean
    @Primary
    SshProcessRunner sshProcessRunner(FakeSshProcessRunner fakeSshProcessRunner) {
      return fakeSshProcessRunner;
    }
  }

  static final class FakeSshProcessRunner implements SshProcessRunner {
    private final Queue<UploadPlan> uploadPlans = new ArrayDeque<>();
    private final Queue<ExecPlan> execPlans = new ArrayDeque<>();
    private final AtomicInteger uploadCallCount = new AtomicInteger();
    private final AtomicInteger execCallCount = new AtomicInteger();
    private final List<String> executedCommands = new ArrayList<>();
    private final List<String> uploadTargets = new ArrayList<>();
    private final List<String> uploadSourcePaths = new ArrayList<>();
    private String lastExecutedCommand;
    private ExecPlan blockingExecPlan;

    void reset() {
      uploadPlans.clear();
      execPlans.clear();
      uploadCallCount.set(0);
      execCallCount.set(0);
      executedCommands.clear();
      uploadTargets.clear();
      uploadSourcePaths.clear();
      lastExecutedCommand = null;
      blockingExecPlan = null;
    }

    void queueUploadSuccess(String output) {
      uploadPlans.add(UploadPlan.success(output));
    }

    void queueUploadFailure(String failureMessage) {
      uploadPlans.add(UploadPlan.failure(failureMessage));
    }

    void queueExecSuccess(String output) {
      execPlans.add(ExecPlan.success(output));
    }

    void queueBlockingExecSuccess(String output) {
      blockingExecPlan = ExecPlan.blockingSuccess(output);
      execPlans.add(blockingExecPlan);
    }

    void awaitBlockingExecStarted() throws InterruptedException {
      ExecPlan blockingPlan = currentBlockingPlan();
      if (!blockingPlan.startedLatch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("blocking exec did not start in time");
      }
    }

    void releaseBlockingExec() {
      ExecPlan blockingPlan = currentBlockingPlan();
      blockingPlan.releaseLatch.countDown();
    }

    String lastExecutedCommand() {
      return lastExecutedCommand;
    }

    List<String> executedCommands() {
      return List.copyOf(executedCommands);
    }

    List<String> uploadTargets() {
      return List.copyOf(uploadTargets);
    }

    List<String> uploadSourcePaths() {
      return List.copyOf(uploadSourcePaths);
    }

    private ExecPlan currentBlockingPlan() {
      if (blockingExecPlan == null) {
        throw new AssertionError("no blocking exec plan queued");
      }
      return blockingExecPlan;
    }

    @Override
    public String exec(String ipAddress, SshConnectionOptions options, String command) {
      execCallCount.incrementAndGet();
      executedCommands.add(command);
      lastExecutedCommand = command;
      ExecPlan plan = execPlans.poll();
      if (plan == null) {
        throw new AssertionError("unexpected exec call for " + ipAddress + " command=" + command);
      }
      if (plan.blocking) {
        plan.startedLatch.countDown();
        try {
          if (!plan.releaseLatch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("blocking exec was not released in time");
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("blocking exec interrupted", exception);
        }
      }
      if (plan.failureMessage != null) {
        throw new IllegalStateException(plan.failureMessage);
      }
      return plan.output;
    }

    @Override
    public String upload(String ipAddress, SshConnectionOptions options, String sourcePath, String targetPath) {
      uploadCallCount.incrementAndGet();
      uploadTargets.add(targetPath);
      uploadSourcePaths.add(sourcePath);
      UploadPlan plan = uploadPlans.poll();
      if (plan == null) {
        throw new AssertionError("unexpected upload call for " + ipAddress + " target=" + targetPath);
      }
      if (plan.failureMessage != null) {
        throw new IllegalStateException(plan.failureMessage);
      }
      return plan.output;
    }
  }

  private record UploadPlan(String output, String failureMessage) {
    private static UploadPlan success(String output) {
      return new UploadPlan(output, null);
    }

    private static UploadPlan failure(String failureMessage) {
      return new UploadPlan(null, failureMessage);
    }
  }

  private record ExecPlan(String output,
                          String failureMessage,
                          boolean blocking,
                          CountDownLatch startedLatch,
                          CountDownLatch releaseLatch) {
    private static ExecPlan success(String output) {
      return new ExecPlan(output, null, false, new CountDownLatch(0), new CountDownLatch(0));
    }

    private static ExecPlan blockingSuccess(String output) {
      return new ExecPlan(output, null, true, new CountDownLatch(1), new CountDownLatch(1));
    }
  }
}
