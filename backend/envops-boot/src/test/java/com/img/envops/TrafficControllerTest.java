package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class TrafficControllerTest {
  private static final String NGINX_TRAFFIC_PLUGIN_CLASS = "com.img.envops.modules.traffic.plugin.NginxTrafficPlugin";
  private static final String REST_TRAFFIC_PLUGIN_CLASS = "com.img.envops.modules.traffic.plugin.RestTrafficPlugin";
  private static final String ACTION_REQUEST_CLASS = "com.img.envops.modules.traffic.plugin.TrafficActionRequest";
  private static final String ROLLBACK_REQUEST_CLASS = "com.img.envops.modules.traffic.plugin.TrafficRollbackRequest";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void trafficApplicationServiceDependsOnPluginAbstraction() throws Exception {
    Object trafficApplicationService = applicationContext.getBean(Class.forName("com.img.envops.modules.traffic.application.TrafficApplicationService"));

    Type[] genericInterfaces = trafficApplicationService.getClass().getGenericInterfaces();
    assertThat(genericInterfaces).isEmpty();
    assertThat(Arrays.stream(trafficApplicationService.getClass().getDeclaredFields())
        .map(field -> field.getType().getName()))
        .contains("java.util.List")
        .doesNotContain(NGINX_TRAFFIC_PLUGIN_CLASS, REST_TRAFFIC_PLUGIN_CLASS);
  }

  @Test
  void getTrafficPoliciesRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/traffic/policies"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"))
        .andExpect(jsonPath("$.msg").value("Unauthorized"));
  }

  @Test
  void getTrafficPoliciesReturnsStaticDirectoryAfterLogin() throws Exception {
    String accessToken = login();

    MvcResult result = mockMvc.perform(get("/api/traffic/policies")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    JsonNode data = readData(result);
    assertThat(data.isArray()).isTrue();
    assertThat(data).hasSizeGreaterThanOrEqualTo(2);
    assertThat(collectTextValues(data, "pluginType")).contains("NGINX", "REST");
    assertThat(fieldNames(data.get(0)))
        .contains("app", "strategy", "scope", "trafficRatio", "owner", "status", "pluginType");
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
      assertThat(plugin.path("supportsPreview").asBoolean()).isTrue();
      assertThat(plugin.path("supportsApply").asBoolean()).isTrue();
      assertThat(plugin.path("supportsRollback").asBoolean()).isTrue();
    }
  }

  @Test
  void trafficPluginsReturnStableSkeletonResultsWithoutSecrets() throws Exception {
    Object nginxPlugin = applicationContext.getBean(Class.forName(NGINX_TRAFFIC_PLUGIN_CLASS));
    Object restPlugin = applicationContext.getBean(Class.forName(REST_TRAFFIC_PLUGIN_CLASS));

    JsonNode nginxPreviewResult = invokeRecordMethod(
        nginxPlugin,
        "preview",
        ACTION_REQUEST_CLASS,
        "payment-gateway",
        "header_canary",
        "prod/cn-shanghai-a",
        "20%",
        "traffic-team");
    assertSkeletonResult(nginxPreviewResult, "NGINX", "preview", "payment-gateway");

    JsonNode nginxApplyResult = invokeRecordMethod(
        nginxPlugin,
        "apply",
        ACTION_REQUEST_CLASS,
        "payment-gateway",
        "header_canary",
        "prod/cn-shanghai-a",
        "20%",
        "traffic-team");
    assertSkeletonResult(nginxApplyResult, "NGINX", "apply", "payment-gateway");

    JsonNode nginxRollbackResult = invokeRecordMethod(
        nginxPlugin,
        "rollback",
        ROLLBACK_REQUEST_CLASS,
        "payment-gateway",
        "rollback-20260416-01",
        "incident mitigated");
    assertRollbackSkeletonResult(nginxRollbackResult, "NGINX", "payment-gateway", "rollback-20260416-01");

    JsonNode restPreviewResult = invokeRecordMethod(
        restPlugin,
        "preview",
        ACTION_REQUEST_CLASS,
        "traffic-admin",
        "blue_green",
        "staging/all",
        "100%",
        "release-team");
    assertSkeletonResult(restPreviewResult, "REST", "preview", "traffic-admin");

    JsonNode restApplyResult = invokeRecordMethod(
        restPlugin,
        "apply",
        ACTION_REQUEST_CLASS,
        "traffic-admin",
        "blue_green",
        "staging/all",
        "100%",
        "release-team");
    assertSkeletonResult(restApplyResult, "REST", "apply", "traffic-admin");

    JsonNode restRollbackResult = invokeRecordMethod(
        restPlugin,
        "rollback",
        ROLLBACK_REQUEST_CLASS,
        "traffic-admin",
        "rollback-20260416-02",
        "validation complete");
    assertRollbackSkeletonResult(restRollbackResult, "REST", "traffic-admin", "rollback-20260416-02");
  }

  private void assertSkeletonResult(JsonNode result, String pluginType, String action, String app) {
    assertThat(result.path("pluginType").asText()).isEqualTo(pluginType);
    assertThat(result.path("status").asText()).isEqualTo("NOT_IMPLEMENTED");
    assertThat(result.path("action").asText()).isEqualTo(action);
    assertThat(result.path("app").asText()).isEqualTo(app);
    assertThat(result.path("message").asText())
        .contains("skeleton")
        .contains("not connected");
    assertThat(fieldNames(result)).doesNotContain("secret");
    assertDoesNotThrow(() -> objectMapper.treeToValue(result, Object.class));
  }

  private void assertRollbackSkeletonResult(JsonNode result, String pluginType, String app, String rollbackToken) {
    assertSkeletonResult(result, pluginType, "rollback", app);
    assertThat(result.path("rollbackToken").asText()).isEqualTo(rollbackToken);
  }

  private JsonNode invokeRecordMethod(Object target,
                                      String methodName,
                                      String requestClassName,
                                      Object... args) throws Exception {
    Class<?> requestClass = Class.forName(requestClassName);
    Object request = instantiateRecord(requestClass, args);
    Object result = target.getClass().getMethod(methodName, requestClass).invoke(target, request);
    return objectMapper.valueToTree(result);
  }

  private Object instantiateRecord(Class<?> recordType, Object... args) throws Exception {
    Class<?>[] parameterTypes = Arrays.stream(recordType.getRecordComponents())
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
    Constructor<?> constructor = recordType.getDeclaredConstructor(parameterTypes);
    return constructor.newInstance(args);
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
