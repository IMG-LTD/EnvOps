package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class TrafficControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

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
    assertThat(collectTextValues(data, "status")).contains("ENABLED", "PREVIEW", "REVIEW");

    JsonNode policy3001 = findObjectById(data, 3001);
    assertThat(fieldNames(policy3001))
        .contains("id", "app", "strategy", "scope", "trafficRatio", "owner", "status", "pluginType", "rollbackToken");
    assertThat(policy3001.path("app").asText()).isEqualTo("checkout-gateway");
  }

  @Test
  void getTrafficPluginsReturnsNginxAndRestDirectoryAfterLogin() throws Exception {
    String accessToken = login();

    MvcResult result = mockMvc.perform(get("/api/traffic/plugins")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode data = readData(result);
    assertThat(data.isArray()).isTrue();
    assertThat(data).hasSize(2);
    assertThat(collectTextValues(data, "type")).containsExactly("NGINX", "REST");
    for (JsonNode plugin : data) {
      assertThat(plugin.path("status").asText()).isEqualTo("NOT_IMPLEMENTED");
      assertThat(plugin.path("supportsPreview").asBoolean()).isFalse();
      assertThat(plugin.path("supportsApply").asBoolean()).isFalse();
      assertThat(plugin.path("supportsRollback").asBoolean()).isFalse();
    }
  }

  @Test
  void previewTrafficPolicyReturnsBadRequestWhenPluginIsNotReady() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3001/preview")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("traffic plugin is not ready: NGINX"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3001);
    assertThat(policy.path("status").asText()).isEqualTo("ENABLED");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("traffic-rb-3001");
  }

  @Test
  void applyTrafficPolicyReturnsBadRequestWhenPluginIsNotReady() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3003/apply")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("traffic plugin is not ready: NGINX"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3003);
    assertThat(policy.path("status").asText()).isEqualTo("REVIEW");
    assertThat(policy.path("rollbackToken").isNull()).isTrue();
  }

  @Test
  void rollbackTrafficPolicyReturnsBadRequestWhenPluginIsNotReady() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3002/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("traffic plugin is not ready: REST"));

    JsonNode policy = getPolicyAfterLogin(accessToken, 3002);
    assertThat(policy.path("status").asText()).isEqualTo("PREVIEW");
    assertThat(policy.path("rollbackToken").asText()).isEqualTo("traffic-rb-3002");
  }

  @Test
  void rollbackTrafficPolicyWithoutRollbackTokenReturnsBadRequest() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/traffic/policies/3003/rollback")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("rollbackToken is required for policy: 3003"));
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
