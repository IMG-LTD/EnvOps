package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "envops.traffic.rest.token=test-only-traffic-rest-token"
})
class TrafficControllerTest {
  private static MockWebServer trafficRestServer;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeAll
  static void beforeAll() throws IOException {
    trafficRestServer = new MockWebServer();
    trafficRestServer.start();
  }

  @AfterAll
  static void afterAll() throws IOException {
    trafficRestServer.shutdown();
  }

  @DynamicPropertySource
  static void registerTrafficRestProperties(DynamicPropertyRegistry registry) {
    registry.add("envops.traffic.rest.base-url", () -> trafficRestServer.url("/").toString());
    registry.add("envops.traffic.rest.token", () -> "test-only-traffic-rest-token");
  }

  @Test
  void getTrafficPoliciesRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/traffic/policies"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"))
        .andExpect(jsonPath("$.msg").value("Unauthorized"));
  }

  @Test
  void previewTrafficPolicyRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/traffic/policies/3001/preview"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"))
        .andExpect(jsonPath("$.msg").value("Unauthorized"));
  }

  @Test
  void getTrafficPoliciesReturnsPersistedPoliciesAfterLogin() throws Exception {
    String accessToken = login();

    MvcResult result = mockMvc.perform(get("/api/traffic/policies")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode data = readData(result);
    assertThat(data.isArray()).isTrue();
    assertThat(data).hasSize(3);
    assertThat(collectTextValues(data, "pluginType")).contains("NGINX", "REST");
    assertThat(collectTextValues(data, "status")).contains("REVIEW", "PREVIEW");

    JsonNode policy3001 = findObjectById(data, 3001);
    assertThat(fieldNames(policy3001))
        .contains("id", "app", "strategy", "scope", "trafficRatio", "owner", "status", "pluginType", "rollbackToken");
    assertThat(policy3001.path("app").asText()).isEqualTo("checkout-gateway");
  }

  @Test
  void getTrafficPluginsReturnsRestReadyAndNginxNotReadyAfterLogin() throws Exception {
    String accessToken = login();

    MvcResult result = mockMvc.perform(get("/api/traffic/plugins")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode data = readData(result);
    assertThat(data.isArray()).isTrue();
    assertThat(data).hasSize(2);
    JsonNode nginxPlugin = findPluginByType(data, "NGINX");
    JsonNode restPlugin = findPluginByType(data, "REST");

    assertThat(nginxPlugin.path("status").asText()).isEqualTo("NOT_IMPLEMENTED");
    assertThat(nginxPlugin.path("supportsPreview").asBoolean()).isFalse();
    assertThat(nginxPlugin.path("supportsApply").asBoolean()).isFalse();
    assertThat(nginxPlugin.path("supportsRollback").asBoolean()).isFalse();

    assertThat(restPlugin.path("status").asText()).isEqualTo("READY");
    assertThat(restPlugin.path("supportsPreview").asBoolean()).isTrue();
    assertThat(restPlugin.path("supportsApply").asBoolean()).isTrue();
    assertThat(restPlugin.path("supportsRollback").asBoolean()).isTrue();
  }

  @Test
  void previewTrafficPolicyUpdatesPreviewStatusAfterRealRestSuccess() throws Exception {
    trafficRestServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("""
            {"success":true,"message":"preview accepted","rollbackToken":"rb-preview-3001"}
            """));

    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/preview")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.action").value("preview"))
        .andExpect(jsonPath("$.data.policy.status").value("PREVIEW"))
        .andExpect(jsonPath("$.data.policy.rollbackToken").value("rb-preview-3001"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3001);
    assertThat(policy.path("status").asText()).isEqualTo("PREVIEW");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("rb-preview-3001");

    JsonNode task = getLatestTrafficTask(accessToken, "Preview checkout-gateway", null);
    assertThat(task.path("taskName").asText()).isEqualTo("Traffic Preview");
    assertThat(task.path("status").asText()).isEqualTo("success");
    assertThat(task.path("summary").asText()).isEqualTo("Preview checkout-gateway，策略 weighted_routing，插件 REST");
    assertThat(task.path("sourceRoute").asText()).isEqualTo("/traffic/controller");
    assertThat(task.path("errorSummary").isNull()).isTrue();

    JsonNode detail = getTaskDetail(accessToken, task.path("id").asLong());
    assertThat(detail.path("detailPreview").path("action").asText()).isEqualTo("preview");
    assertThat(detail.path("detailPreview").path("app").asText()).isEqualTo("checkout-gateway");
    assertThat(detail.path("detailPreview").path("strategy").asText()).isEqualTo("weighted_routing");
    assertThat(detail.path("detailPreview").path("plugin").asText()).isEqualTo("REST");
    assertThat(detail.path("detailPreview").path("rollbackTokenAvailable").asBoolean()).isTrue();
    assertThat(detail.path("detailPreview").path("sourceRoute").asText()).isEqualTo("/traffic/controller");
    assertThat(detail.path("detailPreview").path("errorSummary").isNull()).isTrue();

    JsonNode tracking = getTaskTracking("Bearer " + accessToken, task.path("id").asLong());
    assertThat(tracking.path("basicInfo").path("taskType").asText()).isEqualTo("traffic_action");
    assertThat(tracking.path("timeline")).hasSize(2);
    assertThat(tracking.path("logSummary").asText()).contains("preview");
    assertThat(tracking.path("sourceLinks").get(0).path("route").asText()).isEqualTo("/traffic/controller");
    assertThat(tracking.path("degraded").asBoolean()).isFalse();
  }

  @Test
  void applyTrafficPolicyUpdatesEnabledStatusAndRollbackTokenAfterRealRestSuccess() throws Exception {
    trafficRestServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("""
            {"success":true,"message":"traffic rule applied","rollbackToken":"rb-apply-3001"}
            """));

    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/apply")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.action").value("apply"))
        .andExpect(jsonPath("$.data.policy.status").value("ENABLED"))
        .andExpect(jsonPath("$.data.policy.rollbackToken").value("rb-apply-3001"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3001);
    assertThat(policy.path("status").asText()).isEqualTo("ENABLED");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("rb-apply-3001");

    JsonNode task = getLatestTrafficTask(accessToken, "Apply checkout-gateway", null);
    assertThat(task.path("taskName").asText()).isEqualTo("Traffic Apply");
    assertThat(task.path("status").asText()).isEqualTo("success");
    assertThat(task.path("summary").asText()).isEqualTo("Apply checkout-gateway，策略 weighted_routing，插件 REST");
    assertThat(task.path("sourceRoute").asText()).isEqualTo("/traffic/controller");
    assertThat(task.path("errorSummary").isNull()).isTrue();

    JsonNode detail = getTaskDetail(accessToken, task.path("id").asLong());
    assertThat(detail.path("detailPreview").path("action").asText()).isEqualTo("apply");
    assertThat(detail.path("detailPreview").path("rollbackTokenAvailable").asBoolean()).isTrue();
    assertThat(detail.path("detailPreview").path("errorSummary").isNull()).isTrue();
  }

  @Test
  void rollbackTrafficPolicyUpdatesRolledBackStatusAfterRealRestSuccess() throws Exception {
    trafficRestServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("""
            {"success":true,"message":"rollback applied"}
            """));

    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3002/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.action").value("rollback"))
        .andExpect(jsonPath("$.data.policy.status").value("ROLLED_BACK"))
        .andExpect(jsonPath("$.data.policy.rollbackToken").value("rb-apply-3002"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3002);
    assertThat(policy.path("status").asText()).isEqualTo("ROLLED_BACK");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("rb-apply-3002");

    JsonNode task = getLatestTrafficTask(accessToken, "Rollback billing-admin", null);
    assertThat(task.path("taskName").asText()).isEqualTo("Traffic Rollback");
    assertThat(task.path("status").asText()).isEqualTo("success");
    assertThat(task.path("summary").asText()).isEqualTo("Rollback billing-admin，策略 weighted_routing，插件 REST");
    assertThat(task.path("errorSummary").isNull()).isTrue();

    JsonNode detail = getTaskDetail(accessToken, task.path("id").asLong());
    assertThat(detail.path("detailPreview").path("action").asText()).isEqualTo("rollback");
    assertThat(detail.path("detailPreview").path("rollbackTokenAvailable").asBoolean()).isTrue();
    assertThat(detail.path("detailPreview").path("sourceRoute").asText()).isEqualTo("/traffic/controller");
  }

  @Test
  void applyTrafficPolicyRejectsUnsupportedStrategyWithoutChangingState() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3003/apply")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("traffic plugin is not supported in v0.0.5: NGINX"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3003);
    assertThat(policy.path("status").asText()).isEqualTo("REVIEW");
    assertThat(policy.path("rollbackToken").isNull()).isTrue();

    JsonNode tasks = getTrafficTasks(accessToken, "ops-worker", null);
    assertThat(tasks).isEmpty();
  }

  @Test
  void applyTrafficPolicyRequiresRollbackTokenFromRealRestService() throws Exception {
    trafficRestServer.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("""
            {"success":true,"message":"traffic rule applied"}
            """));

    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/apply")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("rollbackToken is required from traffic rest service for apply: 3001"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3001);
    assertThat(policy.path("status").asText()).isEqualTo("REVIEW");
    assertThat(policy.path("rollbackToken").isNull()).isTrue();

    JsonNode task = getLatestTrafficTask(accessToken, "Apply checkout-gateway", "failed");
    assertThat(task.path("taskName").asText()).isEqualTo("Traffic Apply");
    assertThat(task.path("status").asText()).isEqualTo("failed");
    assertThat(task.path("summary").asText()).isEqualTo("Apply checkout-gateway，策略 weighted_routing，插件 REST");
    assertThat(task.path("errorSummary").asText()).isEqualTo("rollbackToken is required from traffic rest service");

    JsonNode detail = getTaskDetail(accessToken, task.path("id").asLong());
    assertThat(detail.path("detailPreview").path("action").asText()).isEqualTo("apply");
    assertThat(detail.path("detailPreview").path("rollbackTokenAvailable").asBoolean()).isFalse();
    assertThat(detail.path("detailPreview").path("errorSummary").asText()).isEqualTo("rollbackToken is required from traffic rest service");
  }

  @Test
  void rollbackTrafficPolicyWithoutRollbackTokenReturnsBadRequest() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("rollbackToken is required for policy: 3001"));

    JsonNode tasks = getTrafficTasks(accessToken, "Rollback checkout-gateway", null);
    assertThat(tasks).isEmpty();
  }

  @Test
  void rollbackTrafficPolicyReturnsBadGatewayWhenExternalRestServiceIsUnavailable() throws Exception {
    trafficRestServer.shutdown();

    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3002/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("502"))
        .andExpect(jsonPath("$.msg").value("traffic rest service is unavailable"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3002);
    assertThat(policy.path("status").asText()).isEqualTo("PREVIEW");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("rb-apply-3002");

    JsonNode task = getLatestTrafficTask(accessToken, "Rollback billing-admin", "failed");
    assertThat(task.path("taskName").asText()).isEqualTo("Traffic Rollback");
    assertThat(task.path("status").asText()).isEqualTo("failed");
    assertThat(task.path("summary").asText()).isEqualTo("Rollback billing-admin，策略 weighted_routing，插件 REST");
    assertThat(task.path("errorSummary").asText()).isEqualTo("traffic rest service is unavailable");

    JsonNode detail = getTaskDetail(accessToken, task.path("id").asLong());
    assertThat(detail.path("detailPreview").path("action").asText()).isEqualTo("rollback");
    assertThat(detail.path("detailPreview").path("rollbackTokenAvailable").asBoolean()).isTrue();
    assertThat(detail.path("detailPreview").path("errorSummary").asText()).isEqualTo("traffic rest service is unavailable");

    trafficRestServer = new MockWebServer();
    trafficRestServer.start();
  }

  @Test
  void previewTrafficPolicyReturnsNotFoundForMissingPolicy() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3999/preview")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("404"))
        .andExpect(jsonPath("$.msg").value("traffic policy not found: 3999"));
  }

  private JsonNode getPolicyAfterLogin(String accessToken, long policyId) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/traffic/policies")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    return findObjectById(readData(result), policyId);
  }

  private JsonNode getLatestTrafficTask(String accessToken, String keyword, String statusFilter) throws Exception {
    JsonNode tasks = getTrafficTasks(accessToken, keyword, statusFilter);
    assertThat(tasks.isArray()).isTrue();
    assertThat(tasks).isNotEmpty();
    return tasks.get(0);
  }

  private JsonNode getTrafficTasks(String accessToken, String keyword, String statusFilter) throws Exception {
    var requestBuilder = get("/api/task-center/tasks")
        .param("taskType", "traffic_action")
        .param("keyword", keyword)
        .header("Authorization", "Bearer " + accessToken);
    if (statusFilter != null) {
      requestBuilder.param("status", statusFilter);
    }

    MvcResult result = mockMvc.perform(requestBuilder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();
    return readData(result).path("records");
  }

  private JsonNode getTaskDetail(String accessToken, long taskId) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/task-center/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();
    return readData(result);
  }

  private JsonNode getTaskTracking(String accessToken, long taskId) throws Exception {
    String body = mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", taskId)
            .header("Authorization", accessToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(body).path("data");
  }

  private JsonNode findObjectById(JsonNode arrayNode, long id) {
    for (JsonNode item : arrayNode) {
      if (item.path("id").asLong() == id) {
        return item;
      }
    }

    throw new IllegalArgumentException("record not found: " + id);
  }

  private JsonNode findPluginByType(JsonNode arrayNode, String pluginType) {
    for (JsonNode item : arrayNode) {
      if (pluginType.equals(item.path("type").asText())) {
        return item;
      }
    }

    throw new IllegalArgumentException("plugin not found: " + pluginType);
  }

  private JsonNode readData(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
  }

  private List<String> collectTextValues(JsonNode arrayNode, String fieldName) {
    List<String> values = new ArrayList<>();
    for (JsonNode item : arrayNode) {
      values.add(item.path(fieldName).asText());
    }
    return values;
  }

  private List<String> fieldNames(JsonNode node) {
    Iterator<String> iterator = node.fieldNames();
    List<String> fields = new ArrayList<>();
    iterator.forEachRemaining(fields::add);
    return fields;
  }

  private String login() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  \"userName\": \"envops-admin\",
                  \"password\": \"EnvOps@123\"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
