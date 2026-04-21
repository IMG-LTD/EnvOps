package com.img.envops.modules.asset.application.connectivity;

public record DatabaseConnectivityProbeResult(boolean success, String message) {
  public static DatabaseConnectivityProbeResult success(String message) {
    return new DatabaseConnectivityProbeResult(true, message);
  }

  public static DatabaseConnectivityProbeResult failure(String message) {
    return new DatabaseConnectivityProbeResult(false, message);
  }
}
