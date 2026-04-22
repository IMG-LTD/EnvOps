package com.img.envops.modules.traffic.plugin;

import com.img.envops.modules.traffic.application.TrafficRestPluginProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestTrafficPlugin implements TrafficPlugin {
  public static final String PLUGIN_TYPE = "REST";
  public static final String PLUGIN_NAME = "REST Traffic Plugin";
  public static final String READY_STATUS = "READY";
  public static final String NOT_READY_STATUS = "NOT_READY";

  private final TrafficRestPluginProperties properties;
  private final TrafficRestClient trafficRestClient;

  @Autowired
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
    return properties.isReady() ? READY_STATUS : NOT_READY_STATUS;
  }

  @Override
  public boolean supportsPreview() {
    return properties.isReady();
  }

  @Override
  public boolean supportsApply() {
    return properties.isReady();
  }

  @Override
  public boolean supportsRollback() {
    return properties.isReady();
  }

  @Override
  public TrafficPluginResult preview(TrafficActionRequest request) {
    return trafficRestClient.preview(request);
  }

  @Override
  public TrafficPluginResult apply(TrafficActionRequest request) {
    return trafficRestClient.apply(request);
  }

  @Override
  public TrafficPluginResult rollback(TrafficRollbackRequest request) {
    return trafficRestClient.rollback(request);
  }
}
