package com.img.envops.modules.traffic.plugin;

import com.img.envops.modules.traffic.application.TrafficRestPluginProperties;
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

      RestTrafficPlugin plugin = new RestTrafficPlugin(createReadyProperties(server), new TrafficRestClient(createReadyProperties(server)));

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
      assertThat(result.status()).isEqualTo(RestTrafficPlugin.READY_STATUS);
      assertThat(result.rollbackToken()).isEqualTo("rb-preview-123");
      assertThat(result.action()).isEqualTo("preview");
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

      RestTrafficPlugin plugin = new RestTrafficPlugin(createReadyProperties(server), new TrafficRestClient(createReadyProperties(server)));

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

  @Test
  void rollbackSendsRollbackTokenAndReason() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("""
              {"success":true,"message":"rollback applied"}
              """));
      server.start();

      RestTrafficPlugin plugin = new RestTrafficPlugin(createReadyProperties(server), new TrafficRestClient(createReadyProperties(server)));

      TrafficPluginResult result = plugin.rollback(new TrafficRollbackRequest(
          "checkout-gateway",
          "rb-apply-456",
          "manual rollback"));

      var recorded = server.takeRequest();
      assertThat(recorded.getPath()).isEqualTo("/traffic/policies/rollback");
      assertThat(recorded.getBody().readUtf8()).contains("\"rollbackToken\":\"rb-apply-456\"", "\"reason\":\"manual rollback\"");
      assertThat(result.action()).isEqualTo("rollback");
      assertThat(result.rollbackToken()).isNull();
      assertThat(result.reason()).isEqualTo("manual rollback");
    }
  }

  @Test
  void previewRaisesExternalServiceExceptionWhenServiceIsUnavailable() {
    TrafficRestPluginProperties properties = new TrafficRestPluginProperties();
    properties.setBaseUrl("http://127.0.0.1:1");
    properties.setToken("traffic-rest-token");
    properties.setConnectTimeoutMs(50);
    properties.setReadTimeoutMs(50);

    RestTrafficPlugin plugin = new RestTrafficPlugin(properties, new TrafficRestClient(properties));

    assertThatThrownBy(() -> plugin.preview(new TrafficActionRequest(
        "checkout-gateway",
        "weighted_routing",
        "prod / cn-beijing-b",
        "10%",
        "platform-team")))
        .isInstanceOf(TrafficExternalServiceException.class)
        .hasMessage("traffic rest service is unavailable");
  }

  private TrafficRestPluginProperties createReadyProperties(MockWebServer server) {
    TrafficRestPluginProperties properties = new TrafficRestPluginProperties();
    properties.setBaseUrl(server.url("/").toString());
    properties.setToken("traffic-rest-token");
    properties.setConnectTimeoutMs(3000);
    properties.setReadTimeoutMs(5000);
    return properties;
  }
}
