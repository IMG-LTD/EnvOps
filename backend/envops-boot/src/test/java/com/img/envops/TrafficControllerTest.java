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
  }

  @Test
  void rollbackTrafficPolicyWithoutRollbackTokenReturnsBadRequest() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("rollbackToken is required for policy: 3001"));
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
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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
