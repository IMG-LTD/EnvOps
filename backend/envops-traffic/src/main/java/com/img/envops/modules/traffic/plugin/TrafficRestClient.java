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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class TrafficRestClient {
  private static final String DEFAULT_ERROR_MESSAGE = "traffic rest service is unavailable";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final TrafficRestPluginProperties properties;

  public TrafficRestClient(TrafficRestPluginProperties properties) {
    this(properties, new ObjectMapper());
  }

  TrafficRestClient(TrafficRestPluginProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
    requestFactory.setReadTimeout(properties.getReadTimeoutMs());
    this.restTemplate = new RestTemplate(requestFactory);
  }

  public TrafficPluginResult preview(TrafficActionRequest request) {
    return postAction("/traffic/policies/preview", "preview", Map.of(
        "app", request.app(),
        "strategy", request.strategy(),
        "scope", request.scope(),
        "trafficRatio", request.trafficRatio(),
        "owner", request.owner()));
  }

  public TrafficPluginResult apply(TrafficActionRequest request) {
    return postAction("/traffic/policies/apply", "apply", Map.of(
        "app", request.app(),
        "strategy", request.strategy(),
        "scope", request.scope(),
        "trafficRatio", request.trafficRatio(),
        "owner", request.owner()));
  }

  public TrafficPluginResult rollback(TrafficRollbackRequest request) {
    return postAction("/traffic/policies/rollback", "rollback", Map.of(
        "app", request.app(),
        "rollbackToken", request.rollbackToken(),
        "reason", request.reason()));
  }

  private TrafficPluginResult postAction(String path, String action, Map<String, Object> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(properties.getToken());

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          normalizeBaseUrl(properties.getBaseUrl()) + path,
          HttpMethod.POST,
          new HttpEntity<>(body, headers),
          String.class);

      JsonNode json = objectMapper.readTree(response.getBody());
      boolean success = json.path("success").asBoolean(false);
      String message = readMessage(json);

      if (!success) {
        throw new IllegalArgumentException(message);
      }

      return new TrafficPluginResult(
          RestTrafficPlugin.PLUGIN_TYPE,
          RestTrafficPlugin.READY_STATUS,
          action,
          message,
          readText(body.get("app")),
          readText(body.get("strategy")),
          readText(body.get("scope")),
          readText(body.get("trafficRatio")),
          readText(body.get("owner")),
          readOptionalText(json, "rollbackToken"),
          readText(body.get("reason")));
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (ResourceAccessException | HttpStatusCodeException exception) {
      throw new TrafficExternalServiceException(DEFAULT_ERROR_MESSAGE);
    } catch (Exception exception) {
      throw new TrafficExternalServiceException(DEFAULT_ERROR_MESSAGE);
    }
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private String readMessage(JsonNode json) {
    String message = readOptionalText(json, "message");
    return message == null ? "traffic action failed" : message;
  }

  private String readOptionalText(JsonNode json, String fieldName) {
    JsonNode field = json.path(fieldName);
    if (field.isMissingNode() || field.isNull()) {
      return null;
    }
    String value = field.asText();
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  private String readText(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value);
    return text.trim().isEmpty() ? null : text;
  }
}
