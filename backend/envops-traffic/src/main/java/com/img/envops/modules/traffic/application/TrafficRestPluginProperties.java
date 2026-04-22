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

  public boolean isReady() {
    return hasText(baseUrl) && hasText(token);
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
