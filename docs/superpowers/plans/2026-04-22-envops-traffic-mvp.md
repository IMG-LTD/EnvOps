# EnvOps Traffic MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn Traffic from skeleton/not-ready into a real MVP that supports `REST + weighted_routing` with true `preview` / `apply` / `rollback` execution, real rollback tokens, clear failure semantics, and aligned UI/docs.

**Architecture:** Keep the existing Traffic module and replace the `REST` plugin skeleton with a real HTTP-backed implementation. Add a small REST client plus strongly scoped plugin configuration, tighten `TrafficApplicationService` so only `REST + weighted_routing` can execute, and update the frontend page to expose the MVP boundaries instead of a blanket not-ready warning. Preserve the existing `traffic_policy` table and mixed seed data, but only policies inside the MVP scope become actionable.

**Tech Stack:** Spring Boot 3.3, spring-web, MyBatis, JUnit 5, MockMvc, H2 seed SQL, Vue 3, TypeScript, Naive UI, Vitest

---

## File structure

### Backend files

- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficRestPluginProperties.java`
  - Holds `baseUrl`, `token`, `connectTimeoutMs`, `readTimeoutMs` under `envops.traffic.rest`.
- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficRestClient.java`
  - Sends preview/apply/rollback HTTP requests to the external REST service and parses the minimal response contract.
- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficExternalServiceException.java`
  - Represents upstream connectivity failures and maps to HTTP 502 via the global exception handler.
- Create: `backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java`
  - Verifies request payloads, bearer token forwarding, rollback calls, `2xx + success=false` business failure handling, and timeout/5xx failure mapping.
- Modify: `backend/envops-traffic/pom.xml`
  - Add test dependencies needed by the new plugin test.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
  - Enforce `REST + weighted_routing` scope, stop generating fake rollback tokens, update status transitions, and surface plugin readiness in a deterministic way.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/RestTrafficPlugin.java`
  - Replace skeleton behavior with real HTTP-backed preview/apply/rollback.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficPluginResult.java`
  - Add any fields needed by the frontend for actionable result display without expanding beyond the approved scope.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficActionRequest.java`
  - Keep the exact request payload needed by preview/apply.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficRollbackRequest.java`
  - Keep the exact request payload needed by rollback.
- Modify: `backend/envops-framework/src/main/java/com/img/envops/framework/web/GlobalExceptionHandler.java`
  - Map upstream external-service failures to `502` while preserving existing `400` and `404` behavior.
- Modify: `backend/envops-boot/src/main/resources/application.yml`
  - Add `envops.traffic.rest.*` configuration entries wired to environment variables.
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
  - Adjust traffic seed data so it always includes one rollback-ready REST weighted-routing policy, one apply-ready REST weighted-routing policy, and one unsupported policy for disabled UI state.
- Modify: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
  - Replace not-ready assertions with real success/failure integration cases backed by `MockWebServer`.

### Frontend files

- Modify: `frontend/src/views/traffic/controller/index.vue`
  - Replace the blanket not-ready presentation with MVP boundary messaging, support-aware button enabling, explicit failure feedback, and latest-action summaries for both success and failure.
- Modify: `frontend/src/views/traffic/traffic-contract.spec.ts`
  - Update the contract test from “everything disabled” to the new MVP behavior.
- Modify: `frontend/src/service/api/traffic.ts`
  - Keep request signatures aligned with the backend action responses if response shape changes.
- Modify: `frontend/src/typings/api/traffic.d.ts`
  - Keep action result and policy/plugin record types aligned with the MVP response contract.
- Modify: `frontend/src/locales/langs/en-us.ts`
- Modify: `frontend/src/locales/langs/zh-cn.ts`
  - Replace not-ready copy with MVP-scope copy and add labels/messages for unsupported reasons and latest action feedback.
- Modify: `frontend/src/typings/app.d.ts`
  - Update locale typing keys if new traffic strings are added.

### Docs files

- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Create: `release/0.0.5-release-notes.md`
  - Update the Traffic section from not-ready to “limited but real MVP” and record the exact scope.

---

### Task 1: Make the REST plugin a real external client

**Files:**
- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficRestPluginProperties.java`
- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficRestClient.java`
- Create: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficExternalServiceException.java`
- Create: `backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java`
- Modify: `backend/envops-traffic/pom.xml`
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/RestTrafficPlugin.java`
- Test: `backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java`

- [ ] **Step 1: Write the failing plugin tests for real preview/apply/rollback calls**

Create `backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java` with concrete tests for request payloads and failure handling:

```java
package com.img.envops.modules.traffic.plugin;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestTrafficPluginTest {
  @Test
  void previewSendsWeightedRoutingPayloadAndUsesBearerToken() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("""
              {"success":true,"message":"preview accepted","rollbackToken":"rb-preview-123"}
              """));
      server.start();

      TrafficRestPluginProperties properties = new TrafficRestPluginProperties();
      properties.setBaseUrl(server.url("/").toString());
      properties.setToken("traffic-rest-token");
      properties.setConnectTimeoutMs(3000);
      properties.setReadTimeoutMs(5000);

      RestTrafficPlugin plugin = new RestTrafficPlugin(properties, new TrafficRestClient(properties));
      TrafficPluginResult result = plugin.preview(new TrafficActionRequest(
          "checkout-gateway",
          "weighted_routing",
          "prod / cn-beijing-b",
          "10%",
          "platform-team"));

      var recorded = server.takeRequest();
      assertThat(recorded.getPath()).isEqualTo("/traffic/policies/preview");
      assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer traffic-rest-token");
      assertThat(recorded.getBody().readUtf8()).contains("\"strategy\":\"weighted_routing\"");
      assertThat(result.status()).isEqualTo("READY");
      assertThat(result.rollbackToken()).isEqualTo("rb-preview-123");
    }
  }

  @Test
  void applyRejectsBusinessFailureResponse() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("""
              {"success":false,"message":"ratio exceeds allowed limit"}
              """));
      server.start();

      TrafficRestPluginProperties properties = new TrafficRestPluginProperties();
      properties.setBaseUrl(server.url("/").toString());
      properties.setToken("traffic-rest-token");
      properties.setConnectTimeoutMs(3000);
      properties.setReadTimeoutMs(5000);

      RestTrafficPlugin plugin = new RestTrafficPlugin(properties, new TrafficRestClient(properties));

      assertThatThrownBy(() -> plugin.apply(new TrafficActionRequest(
          "checkout-gateway",
          "weighted_routing",
          "prod / cn-beijing-b",
          "10%",
          "platform-team")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("ratio exceeds allowed limit");
    }
  }
}
```

- [ ] **Step 2: Run the targeted plugin test and verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test
```

Expected:
- The build fails because `TrafficRestPluginProperties`, `TrafficRestClient`, and the new `RestTrafficPlugin` constructor do not exist.
- If the test compiles early, it still fails because `RestTrafficPlugin` is still `NOT_IMPLEMENTED` and never performs HTTP calls.

- [ ] **Step 3: Add configuration, HTTP client, and real REST plugin implementation**

Update `backend/envops-traffic/pom.xml` to add MockWebServer for tests:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>4.12.0</version>
    <scope>test</scope>
</dependency>
```

Create `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficRestPluginProperties.java`:

```java
package com.img.envops.modules.traffic.application;

public class TrafficRestPluginProperties {
  private String baseUrl;
  private String token;
  private int connectTimeoutMs = 3000;
  private int readTimeoutMs = 5000;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public void setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
  }

  public int getReadTimeoutMs() {
    return readTimeoutMs;
  }

  public void setReadTimeoutMs(int readTimeoutMs) {
    this.readTimeoutMs = readTimeoutMs;
  }
}
```

Create `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficExternalServiceException.java`:

```java
package com.img.envops.modules.traffic.plugin;

public class TrafficExternalServiceException extends RuntimeException {
  public TrafficExternalServiceException(String message) {
    super(message);
  }
}
```

Create `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficRestClient.java`:

```java
package com.img.envops.modules.traffic.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.traffic.application.TrafficRestPluginProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class TrafficRestClient {
  private final RestTemplate restTemplate;
  private final TrafficRestPluginProperties properties;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public TrafficRestClient(TrafficRestPluginProperties properties) {
    this.properties = properties;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(properties.getReadTimeoutMs());
    this.restTemplate = new RestTemplate(requestFactory);
  }

  public TrafficPluginResult postAction(String path, String action, Map<String, Object> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(properties.getToken());

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          properties.getBaseUrl() + path,
          HttpMethod.POST,
          new HttpEntity<>(body, headers),
          String.class);

      JsonNode json = objectMapper.readTree(response.getBody());
      boolean success = json.path("success").asBoolean(false);
      String message = json.path("message").asText("Traffic action failed");

      if (!success) {
        throw new IllegalArgumentException(message);
      }

      String rollbackToken = json.path("rollbackToken").isMissingNode() || json.path("rollbackToken").isNull()
          ? null
          : json.path("rollbackToken").asText();

      return new TrafficPluginResult(
          RestTrafficPlugin.PLUGIN_TYPE,
          "READY",
          action,
          message,
          String.valueOf(body.getOrDefault("app", "")),
          (String) body.get("strategy"),
          (String) body.get("scope"),
          (String) body.get("trafficRatio"),
          (String) body.get("owner"),
          rollbackToken,
          (String) body.get("reason"));
    } catch (ResourceAccessException exception) {
      throw new TrafficExternalServiceException("traffic rest service is unavailable");
    } catch (TrafficExternalServiceException exception) {
      throw exception;
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new TrafficExternalServiceException("traffic rest service is unavailable");
    }
  }
}
```

Replace `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/RestTrafficPlugin.java` with a real implementation:

```java
package com.img.envops.modules.traffic.plugin;

import com.img.envops.modules.traffic.application.TrafficRestPluginProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestTrafficPlugin implements TrafficPlugin {
  public static final String PLUGIN_TYPE = "REST";
  public static final String PLUGIN_NAME = "REST Traffic Plugin";

  private final TrafficRestPluginProperties properties;
  private final TrafficRestClient trafficRestClient;

  public RestTrafficPlugin(
      @Value("${envops.traffic.rest.base-url:}") String baseUrl,
      @Value("${envops.traffic.rest.token:}") String token,
      @Value("${envops.traffic.rest.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${envops.traffic.rest.read-timeout-ms:5000}") int readTimeoutMs) {
    TrafficRestPluginProperties pluginProperties = new TrafficRestPluginProperties();
    pluginProperties.setBaseUrl(baseUrl);
    pluginProperties.setToken(token);
    pluginProperties.setConnectTimeoutMs(connectTimeoutMs);
    pluginProperties.setReadTimeoutMs(readTimeoutMs);
    this.properties = pluginProperties;
    this.trafficRestClient = new TrafficRestClient(pluginProperties);
  }

  RestTrafficPlugin(TrafficRestPluginProperties properties, TrafficRestClient trafficRestClient) {
    this.properties = properties;
    this.trafficRestClient = trafficRestClient;
  }

  @Override
  public String pluginType() {
    return PLUGIN_TYPE;
  }

  @Override
  public String pluginName() {
    return PLUGIN_NAME;
  }

  @Override
  public String pluginStatus() {
    return isReady() ? "READY" : "NOT_READY";
  }

  @Override
  public boolean supportsPreview() {
    return isReady();
  }

  @Override
  public boolean supportsApply() {
    return isReady();
  }

  @Override
  public boolean supportsRollback() {
    return isReady();
  }

  @Override
  public TrafficPluginResult preview(TrafficActionRequest request) {
    return trafficRestClient.postAction("/traffic/policies/preview", "preview", buildActionBody(request));
  }

  @Override
  public TrafficPluginResult apply(TrafficActionRequest request) {
    return trafficRestClient.postAction("/traffic/policies/apply", "apply", buildActionBody(request));
  }

  @Override
  public TrafficPluginResult rollback(TrafficRollbackRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("app", request.app());
    body.put("rollbackToken", request.rollbackToken());
    body.put("reason", request.reason());
    return trafficRestClient.postAction("/traffic/policies/rollback", "rollback", body);
  }

  private Map<String, Object> buildActionBody(TrafficActionRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("app", request.app());
    body.put("strategy", request.strategy());
    body.put("scope", request.scope());
    body.put("trafficRatio", request.trafficRatio());
    body.put("owner", request.owner());
    return body;
  }

  private boolean isReady() {
    return StringUtils.hasText(properties.getBaseUrl()) && StringUtils.hasText(properties.getToken());
  }
}
```

- [ ] **Step 4: Run the plugin test to verify it passes**

Run:

```bash
mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test
```

Expected: PASS, with the preview request hitting `/traffic/policies/preview`, bearer auth set, and business failure returned as `IllegalArgumentException`.

- [ ] **Step 5: Commit the backend plugin implementation**

Run:

```bash
git add backend/envops-traffic/pom.xml \
  backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficRestPluginProperties.java \
  backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficExternalServiceException.java \
  backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/TrafficRestClient.java \
  backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/plugin/RestTrafficPlugin.java \
  backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java
git commit -m "feat: add rest traffic plugin client"
```

Expected: a commit containing only the REST plugin implementation and its focused unit test.

---

### Task 2: Enforce MVP scope and truthful policy state transitions in the backend

**Files:**
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
- Modify: `backend/envops-framework/src/main/java/com/img/envops/framework/web/GlobalExceptionHandler.java`
- Modify: `backend/envops-boot/src/main/resources/application.yml`
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
- Modify: `backend/envops-boot/pom.xml`
- Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`

- [ ] **Step 1: Write the failing controller tests for MVP scope and real rollback token semantics**

Replace the current not-ready expectations in `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java` with concrete success and failure tests.

Add the success path test for apply:

```java
@Test
void applyTrafficPolicyUpdatesEnabledStatusAndRollbackTokenAfterRealRestSuccess() throws Exception {
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
```

Add the unsupported strategy test:

```java
@Test
void applyTrafficPolicyRejectsUnsupportedStrategyWithoutChangingState() throws Exception {
  String accessToken = login();

  mockMvc.perform(post("/api/traffic/policies/3003/apply")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("400"))
      .andExpect(jsonPath("$.msg").value("traffic strategy is not supported in v0.0.5: header_canary"));

  JsonNode policy = getPolicyAfterLogin(accessToken, 3003);
  assertThat(policy.path("status").asText()).isEqualTo("REVIEW");
  assertThat(policy.path("rollbackToken").isNull()).isTrue();
}
```

Add the upstream failure test:

```java
@Test
void rollbackTrafficPolicyReturnsBadGatewayWhenExternalRestServiceIsUnavailable() throws Exception {
  String accessToken = login();

  mockMvc.perform(post("/api/traffic/policies/3002/rollback")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isBadGateway())
      .andExpect(jsonPath("$.code").value("502"))
      .andExpect(jsonPath("$.msg").value("traffic rest service is unavailable"));

  JsonNode policy = getPolicyAfterLogin(accessToken, 3002);
  assertThat(policy.path("status").asText()).isEqualTo("PREVIEW");
  assertThat(policy.path("rollbackToken").asText()).isEqualTo("rb-apply-3002");
}
```

- [ ] **Step 2: Run the controller test and verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test
```

Expected:
- The new apply success test fails because `TrafficApplicationService` still auto-generates tokens and does not require `REST + weighted_routing`.
- The unsupported strategy test fails because unsupported strategies are not rejected explicitly.
- The bad gateway test fails because upstream failures are not yet mapped to `502`.

- [ ] **Step 3: Implement truthful scope checks, status transitions, exception mapping, and seed data**

Update `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java` to stop pretending everything supported by a ready plugin is valid.

Replace the core action methods with explicit MVP checks:

```java
public TrafficPolicyActionRecord previewPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "preview");
  TrafficPluginResult pluginResult = plugin.preview(buildActionRequest(policy));
  String rollbackToken = normalizeOptionalText(pluginResult.rollbackToken());
  TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "PREVIEW", rollbackToken);
  return new TrafficPolicyActionRecord("preview", updatedPolicy, pluginResult);
}

public TrafficPolicyActionRecord applyPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "apply");
  TrafficPluginResult pluginResult = plugin.apply(buildActionRequest(policy));
  String rollbackToken = normalizeOptionalText(pluginResult.rollbackToken());

  if (rollbackToken == null) {
    throw new IllegalArgumentException("rollbackToken is required from traffic rest service for apply: " + policyId);
  }

  TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ENABLED", rollbackToken);
  return new TrafficPolicyActionRecord("apply", updatedPolicy, pluginResult);
}

public TrafficPolicyActionRecord rollbackPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  String rollbackToken = normalizeOptionalText(policy.getRollbackToken());

  if (rollbackToken == null) {
    throw new IllegalArgumentException("rollbackToken is required for policy: " + policyId);
  }

  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "rollback");
  TrafficPluginResult pluginResult = plugin.rollback(new TrafficRollbackRequest(
      policy.getApp(),
      rollbackToken,
      "manual rollback"));
  TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ROLLED_BACK", rollbackToken);
  return new TrafficPolicyActionRecord("rollback", updatedPolicy, pluginResult);
}

private void validateMvpScope(TrafficPolicyMapper.TrafficPolicyRow policy) {
  if (!"REST".equalsIgnoreCase(String.valueOf(policy.getPluginType()))) {
    throw new IllegalArgumentException("traffic plugin is not supported in v0.0.5: " + policy.getPluginType());
  }

  if (!"weighted_routing".equalsIgnoreCase(String.valueOf(policy.getStrategy()))) {
    throw new IllegalArgumentException("traffic strategy is not supported in v0.0.5: " + policy.getStrategy());
  }
}
```

Also change `requirePolicy` to throw `NotFoundException` instead of `IllegalArgumentException`:

```java
if (policy == null) {
  throw new NotFoundException("traffic policy not found: " + policyId);
}
```

Update `backend/envops-framework/src/main/java/com/img/envops/framework/web/GlobalExceptionHandler.java`:

```java
@ExceptionHandler(TrafficExternalServiceException.class)
public ResponseEntity<R<Void>> handleTrafficExternalServiceException(TrafficExternalServiceException exception) {
  return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(R.fail("502", exception.getMessage()));
}
```

Update `backend/envops-boot/src/main/resources/application.yml` to add real plugin configuration keys:

```yaml
envops:
  security:
    token-secret: ${ENVOPS_SECURITY_TOKEN_SECRET}
    credential-protection-secret: ${ENVOPS_CREDENTIAL_PROTECTION_SECRET}
  traffic:
    rest:
      base-url: ${ENVOPS_TRAFFIC_REST_BASE_URL:}
      token: ${ENVOPS_TRAFFIC_REST_TOKEN:}
      connect-timeout-ms: ${ENVOPS_TRAFFIC_REST_CONNECT_TIMEOUT_MS:3000}
      read-timeout-ms: ${ENVOPS_TRAFFIC_REST_READ_TIMEOUT_MS:5000}
```

Update both runtime and test `data.sql` traffic seeds to this exact shape:

```sql
INSERT INTO traffic_policy (id, app, strategy, scope, traffic_ratio, owner, status, plugin_type, rollback_token, created_at, updated_at)
VALUES
    (3001, 'checkout-gateway', 'weighted_routing', 'prod / cn-beijing-b', '10%', 'platform-team', 'REVIEW', 'REST', NULL, TIMESTAMP '2026-04-22 10:00:00', TIMESTAMP '2026-04-22 10:00:00'),
    (3002, 'billing-admin', 'weighted_routing', 'staging / all', '20%', 'release-team', 'PREVIEW', 'REST', 'rb-apply-3002', TIMESTAMP '2026-04-22 10:05:00', TIMESTAMP '2026-04-22 10:05:00'),
    (3003, 'ops-worker', 'header_canary', 'prod / cn-shanghai-a', '5%', 'traffic-team', 'REVIEW', 'NGINX', NULL, TIMESTAMP '2026-04-22 10:10:00', TIMESTAMP '2026-04-22 10:10:00');
```

In the test class, back the controller tests with `MockWebServer` so success and failure semantics are real instead of mocked inside the service.

Add the test dependency in `backend/envops-boot/pom.xml`:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>4.12.0</version>
    <scope>test</scope>
</dependency>
```

Set the REST plugin environment properties so the plugin becomes ready during controller tests:

```java
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "envops.traffic.rest.base-url=http://127.0.0.1:${traffic.rest.mock-port}",
    "envops.traffic.rest.token=test-only-traffic-rest-token"
})
```

Start `MockWebServer` in `@BeforeAll`, inject its port through `@DynamicPropertySource`, and enqueue exact upstream responses inside each test:

```java
private static MockWebServer trafficRestServer;

@BeforeAll
static void beforeAll() throws Exception {
  trafficRestServer = new MockWebServer();
  trafficRestServer.start();
}

@AfterAll
static void afterAll() throws Exception {
  trafficRestServer.shutdown();
}

@DynamicPropertySource
static void registerTrafficRestProperties(DynamicPropertyRegistry registry) {
  registry.add("envops.traffic.rest.base-url", () -> trafficRestServer.url("/").toString());
  registry.add("envops.traffic.rest.token", () -> "test-only-traffic-rest-token");
}
```

For the success-path tests, enqueue real success bodies before calling the controller:

```java
trafficRestServer.enqueue(new MockResponse()
    .setHeader("Content-Type", "application/json")
    .setBody("""
        {"success":true,"message":"traffic rule applied","rollbackToken":"rb-apply-3001"}
        """));
```

For the bad-gateway test, shut the server down before the request, or point the property at an unused port, so the plugin hits a real connection failure.

- [ ] **Step 4: Run the controller test to verify it passes**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test
```

Expected: PASS, with `404` for missing policies, `400` for unsupported plugin/strategy or missing rollback token, and `502` for upstream REST unavailability.

- [ ] **Step 5: Commit the application-layer MVP enforcement**

Run:

```bash
git add backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java \
  backend/envops-framework/src/main/java/com/img/envops/framework/web/GlobalExceptionHandler.java \
  backend/envops-boot/src/main/resources/application.yml \
  backend/envops-boot/src/main/resources/data.sql \
  backend/envops-boot/src/test/resources/data.sql \
  backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java
git commit -m "feat: enforce traffic mvp backend scope"
```

Expected: a commit that truthfully enforces MVP scope and removes fake rollback token generation.

---

### Task 3: Update the Traffic page to expose the MVP boundaries and real action results

**Files:**
- Modify: `frontend/src/views/traffic/controller/index.vue`
- Modify: `frontend/src/views/traffic/traffic-contract.spec.ts`
- Modify: `frontend/src/service/api/traffic.ts`
- Modify: `frontend/src/typings/api/traffic.d.ts`
- Modify: `frontend/src/locales/langs/en-us.ts`
- Modify: `frontend/src/locales/langs/zh-cn.ts`
- Modify: `frontend/src/typings/app.d.ts`
- Test: `frontend/src/views/traffic/traffic-contract.spec.ts`

- [ ] **Step 1: Rewrite the failing frontend contract test around the MVP behavior**

Update `frontend/src/views/traffic/traffic-contract.spec.ts` so it asserts one actionable row, one rollback-ready row, one unsupported row, and boundary messaging instead of a blanket degraded page.

Replace the seeded mock data in `beforeEach` with:

```ts
mocks.fetchGetTrafficPolicies.mockResolvedValue({
  error: null,
  data: [
    {
      id: 3001,
      app: 'checkout-gateway',
      strategy: 'weighted_routing',
      scope: 'prod / cn-beijing-b',
      trafficRatio: '10%',
      owner: 'platform-team',
      status: 'REVIEW',
      pluginType: 'REST',
      rollbackToken: null
    },
    {
      id: 3002,
      app: 'billing-admin',
      strategy: 'weighted_routing',
      scope: 'staging / all',
      trafficRatio: '20%',
      owner: 'release-team',
      status: 'PREVIEW',
      pluginType: 'REST',
      rollbackToken: 'rb-apply-3002'
    },
    {
      id: 3003,
      app: 'ops-worker',
      strategy: 'header_canary',
      scope: 'prod / cn-shanghai-a',
      trafficRatio: '5%',
      owner: 'traffic-team',
      status: 'REVIEW',
      pluginType: 'NGINX',
      rollbackToken: null
    }
  ]
});

mocks.fetchGetTrafficPlugins.mockResolvedValue({
  error: null,
  data: [
    {
      type: 'REST',
      name: 'REST Traffic Plugin',
      status: 'READY',
      supportsPreview: true,
      supportsApply: true,
      supportsRollback: true
    },
    {
      type: 'NGINX',
      name: 'Nginx Traffic Plugin',
      status: 'NOT_READY',
      supportsPreview: false,
      supportsApply: false,
      supportsRollback: false
    }
  ]
});
```

Add this concrete UI test:

```ts
it('enables actions only for REST weighted-routing policies and keeps unsupported rows disabled', async () => {
  const page = await mountTrafficPage();

  try {
    expect(page.container.textContent).toContain('Traffic MVP currently supports REST plugin and weighted routing only');
    const rows = Array.from(page.container.querySelectorAll('tbody tr'));
    expect(rows).toHaveLength(3);

    const checkoutButtons = Array.from(rows[0].querySelectorAll('button'));
    const billingButtons = Array.from(rows[1].querySelectorAll('button'));
    const opsButtons = Array.from(rows[2].querySelectorAll('button'));

    expect(checkoutButtons[0]?.hasAttribute('disabled')).toBe(false);
    expect(checkoutButtons[1]?.hasAttribute('disabled')).toBe(false);
    expect(checkoutButtons[2]?.hasAttribute('disabled')).toBe(true);

    expect(billingButtons[0]?.hasAttribute('disabled')).toBe(false);
    expect(billingButtons[1]?.hasAttribute('disabled')).toBe(false);
    expect(billingButtons[2]?.hasAttribute('disabled')).toBe(false);

    expect(opsButtons[0]?.hasAttribute('disabled')).toBe(true);
    expect(opsButtons[1]?.hasAttribute('disabled')).toBe(true);
    expect(opsButtons[2]?.hasAttribute('disabled')).toBe(true);
  } finally {
    page.unmount();
  }
});
```

Add this failure-feedback test:

```ts
it('shows error feedback when a traffic action fails', async () => {
  mocks.fetchPostApplyTrafficPolicy.mockResolvedValueOnce({
    error: { message: 'traffic rest service is unavailable' },
    data: null
  });

  const page = await mountTrafficPage();

  try {
    const rows = Array.from(page.container.querySelectorAll('tbody tr'));
    const checkoutApplyButton = Array.from(rows[0].querySelectorAll('button')).find(button =>
      button.textContent?.includes('Apply')
    );

    checkoutApplyButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.fetchPostApplyTrafficPolicy).toHaveBeenCalledWith(3001);
    const errorMock = window.$message?.error as ReturnType<typeof vi.fn> | undefined;
    expect(errorMock?.mock.calls[0]?.[0]).toContain('traffic rest service is unavailable');
  } finally {
    page.unmount();
  }
});
```

- [ ] **Step 2: Run the frontend contract test and verify it fails**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts
```

Expected:
- The first test fails because the page still shows the old not-ready wording and does not differentiate unsupported rows from supported ones.
- The second test fails because action errors are not surfaced with `window.$message.error`.

- [ ] **Step 3: Update the page logic, locale copy, and typings to match the MVP**

In `frontend/src/views/traffic/controller/index.vue`, change the policy mapping so support depends on both plugin type and strategy, not just plugin readiness:

```ts
const trafficPolicies = computed(() =>
  trafficPolicyList.value.map(item => {
    const statusKey = getTrafficPolicyStatusKey(item.status);
    const plugin = trafficPluginsByType.value.get(normalizeLookupValue(item.pluginType));
    const isPluginReady = normalizeLookupValue(plugin?.status) === 'ready';
    const isRestPlugin = normalizeLookupValue(item.pluginType) === 'rest';
    const isWeightedRouting = normalizeLookupValue(item.strategy) === 'weighted_routing';
    const isSupportedPolicy = isPluginReady && isRestPlugin && isWeightedRouting;
    const rollbackToken = normalizeOptionalText(item.rollbackToken);

    return {
      key: item.id,
      id: item.id,
      app: getDisplayText(item.app),
      strategy: getTrafficStrategyLabel(item.strategy),
      scope: getDisplayText(item.scope),
      ratio: getTrafficRatioLabel(item.trafficRatio),
      owner: getTrafficOwnerLabel(item.owner),
      pluginType: getDisplayText(item.pluginType),
      rollbackToken,
      unsupportedReason: getUnsupportedReason({ isPluginReady, isRestPlugin, isWeightedRouting, rollbackToken }),
      status: getTrafficPolicyStatusLabel(statusKey),
      statusType: getTrafficPolicyTagType(statusKey),
      statusKey,
      canPreview: isSupportedPolicy,
      canApply: isSupportedPolicy,
      canRollback: isSupportedPolicy && Boolean(rollbackToken)
    };
  })
);
```

Add explicit error handling in `handlePolicyAction`:

```ts
async function handlePolicyAction(policyId: number, action: TrafficActionType) {
  actingPolicyId.value = policyId;

  try {
    const actionRequestMap: Record<TrafficActionType, (id: number) => ReturnType<typeof fetchPostPreviewTrafficPolicy>> = {
      preview: fetchPostPreviewTrafficPolicy,
      apply: fetchPostApplyTrafficPolicy,
      rollback: fetchPostRollbackTrafficPolicy
    };

    const { data, error } = await actionRequestMap[action](policyId);

    if (error) {
      const message = String(error.message || t('page.envops.trafficController.messages.actionFailed'));
      latestActionResult.value = null;
      window.$message?.error(message);
      return;
    }

    latestActionResult.value = data;
    window.$message?.success(getTrafficActionSuccessMessage(action));
    await loadTrafficData();
  } finally {
    actingPolicyId.value = null;
  }
}
```

Add an unsupported-reason helper and render plugin/support data in the table:

```ts
function getUnsupportedReason(input: {
  isPluginReady: boolean;
  isRestPlugin: boolean;
  isWeightedRouting: boolean;
  rollbackToken: string | null;
}) {
  if (!input.isPluginReady) {
    return t('page.envops.trafficController.messages.pluginNotReady');
  }

  if (!input.isRestPlugin) {
    return t('page.envops.trafficController.messages.pluginNotSupported');
  }

  if (!input.isWeightedRouting) {
    return t('page.envops.trafficController.messages.strategyNotSupported');
  }

  if (!input.rollbackToken) {
    return t('page.envops.trafficController.messages.rollbackTokenMissing');
  }

  return '';
}
```

Add these table rows under the status cell:

```vue
<div class="mt-4px text-12px text-#999">
  {{ item.pluginType }}
  <template v-if="item.unsupportedReason"> · {{ item.unsupportedReason }}</template>
</div>
```

Update the alert copy in both locale files. Replace the old hero/warning strings with:

```ts
hero: {
  title: 'Traffic Controller',
  description:
    'Execute a limited Traffic MVP with real REST-based preview, apply, and rollback for weighted routing policies.'
},
messages: {
  latestAction: 'Latest Traffic Action',
  notReadyWarning:
    'Traffic MVP currently supports REST plugin and weighted routing only. NGINX, blue-green, and header canary remain outside this release.',
  pluginNotReady: 'Plugin not ready',
  pluginNotSupported: 'Plugin not supported in v0.0.5',
  strategyNotSupported: 'Strategy not supported in v0.0.5',
  rollbackTokenMissing: 'Rollback token required',
  actionFailed: 'Traffic action failed',
  previewSuccess: 'Traffic policy previewed successfully',
  applySuccess: 'Traffic policy applied successfully',
  rollbackSuccess: 'Traffic policy rolled back successfully'
}
```

Add matching Chinese text in `frontend/src/locales/langs/zh-cn.ts`.

If `frontend/src/typings/app.d.ts` needs the new locale keys enumerated, add them there.

- [ ] **Step 4: Run the frontend contract test to verify it passes**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts
pnpm --dir frontend typecheck
```

Expected: PASS, with one supported apply-ready row, one rollback-ready row, one unsupported row, explicit boundary copy, and visible error feedback.

- [ ] **Step 5: Commit the Traffic page MVP UI changes**

Run:

```bash
git add frontend/src/views/traffic/controller/index.vue \
  frontend/src/views/traffic/traffic-contract.spec.ts \
  frontend/src/service/api/traffic.ts \
  frontend/src/typings/api/traffic.d.ts \
  frontend/src/locales/langs/en-us.ts \
  frontend/src/locales/langs/zh-cn.ts \
  frontend/src/typings/app.d.ts
git commit -m "feat: expose traffic mvp boundaries in ui"
```

Expected: a commit that makes the page honest, actionable, and aligned with the backend scope.

---

### Task 4: Align docs, release notes, and final verification with the new Traffic MVP

**Files:**
- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Create: `release/0.0.5-release-notes.md`
- Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
- Test: `backend/envops-traffic/src/test/java/com/img/envops/modules/traffic/plugin/RestTrafficPluginTest.java`
- Test: `frontend/src/views/traffic/traffic-contract.spec.ts`

- [ ] **Step 1: Write the failing release-material assertions into the docs update plan**

Before editing docs, add this checklist to your working notes and verify each target still says the old not-ready story so you know what must change:

```text
README.md: Traffic section still says skeleton / not-ready
项目详细说明: Traffic still says not-ready
开发技术说明: Traffic still says not-ready
用户操作手册: Traffic still says buttons disabled and not usable
release/0.0.5-release-notes.md: file does not exist yet
```

- [ ] **Step 2: Update docs and release notes to the exact MVP scope**

In `README.md`, replace the Traffic bullets with this exact scope:

```md
3. **Traffic**
   - 当前 Traffic 已支持最小真实切流能力。
   - 当前版本只支持 `REST + weighted_routing`。
   - 支持手动 `preview` / `apply` / `rollback`。
   - `rollback` 依赖外部流量服务返回的真实 `rollbackToken`。
   - `NGINX`、`blue_green`、`header_canary` 仍不在当前版本范围内。
```

In `docs/envops-项目详细说明.md`, replace the Traffic subsection with this exact text:

```md
### 5.4 Traffic

Traffic 当前已从 skeleton 收敛成最小真实切流能力：

- 仅支持 `REST` 插件
- 仅支持 `weighted_routing` 策略
- 支持手动 `preview`、`apply`、`rollback`
- 只有外部 REST 流量服务返回成功，EnvOps 才会回写策略状态
- `rollback` 依赖外部服务返回的真实 `rollbackToken`
- `NGINX`、`blue_green`、`header_canary` 仍不在当前版本范围内
```

In `docs/envops-开发技术说明.md`, replace the Traffic subsection with:

```md
### 5.4 Traffic

Traffic 当前真实技术边界如下：

- 页面与接口已支持 `REST + weighted_routing` 的最小真实切流能力
- 后端动作覆盖 `preview`、`apply`、`rollback`
- `apply` 成功必须拿到真实 `rollbackToken`，否则视为失败
- 外部 REST 服务失败统一按上游失败处理，不更新本地策略状态
- 当前不支持 `NGINX`、`blue_green`、`header_canary`，也不包含批量切流、高级编排或灰度报表
```

In `docs/envops-用户操作手册.md`, replace the Traffic usage subsection with:

```md
### 4.4 Traffic

Traffic 当前提供有限可用的 MVP 页面：

1. 进入 Traffic 页面后，优先查看顶部范围提示。
2. 只有 `REST + weighted_routing` 的策略记录允许执行真实动作。
3. 点击 `Preview` 可调用外部流量服务执行预览。
4. 点击 `Apply` 可调用外部流量服务执行真实切流。
5. 只有存在 `rollbackToken` 的记录才允许点击 `Rollback`。
6. 如果外部服务失败，页面会直接提示失败原因，且不会伪造本地成功状态。
```

Create `release/0.0.5-release-notes.md` with this initial content:

```md
# EnvOps v0.0.5 Release Notes

## Highlights

1. Traffic 页面已从 skeleton/not-ready 收敛成最小真实切流能力。
2. 当前版本只支持 `REST + weighted_routing`。
3. `preview`、`apply`、`rollback` 都通过真实外部 REST 流量服务执行。
4. `apply` 成功后必须拿到真实 `rollbackToken`，否则整体视为失败。
5. 页面会明确展示 MVP 边界，不再把不支持的策略和插件包装成可执行能力。

## Deferred scope

1. `NGINX` 插件仍未纳入当前版本。
2. `blue_green` 与 `header_canary` 仍未纳入当前版本。
3. 批量切流、高级编排、灰度分析报表继续延期。

## Verification

- `mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test`
- `pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
```

- [ ] **Step 3: Run the full approved verification commands and confirm they pass**

Run:

```bash
mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test
pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts
pnpm --dir frontend typecheck
pnpm --dir frontend build
```

Expected: all five commands pass on fresh code. If any fail, stop and fix the code before touching commit or release work.

- [ ] **Step 4: Commit the docs and release materials**

Run:

```bash
git add README.md \
  docs/envops-项目详细说明.md \
  docs/envops-开发技术说明.md \
  docs/envops-用户操作手册.md \
  release/0.0.5-release-notes.md
git commit -m "docs: align traffic mvp release materials"
```

Expected: a doc-only commit that matches the actual shipped MVP behavior exactly.

---

## Self-review checklist

### Spec coverage

- REST plugin real execution: covered by Task 1
- `weighted_routing` only and `REST` only scope: covered by Task 2 and Task 3
- True `preview` / `apply` / `rollback` loop: covered by Task 1 and Task 2
- Real rollback token semantics: covered by Task 2
- UI state feedback and boundary messaging: covered by Task 3
- Verification commands and docs sync: covered by Task 4
- Excluded scope remains excluded: enforced throughout all four tasks

### Placeholder scan

Checked for placeholders such as `TBD`, `TODO`, `<traffic-related-specs>`, and vague “update as needed” instructions. All commands, file paths, test names, and release-note content are explicit.

### Type consistency

- Backend action names stay `preview` / `apply` / `rollback` across plugin tests, application service, and controller tests.
- Frontend keeps the existing `TrafficPolicyActionType` union and aligns new UI logic to the same three actions.
- Policy statuses used in tests and UI are `REVIEW`, `PREVIEW`, `ENABLED`, and `ROLLED_BACK`, matching the approved spec.
