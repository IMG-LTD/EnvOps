package com.img.envops.modules.traffic.plugin;

import com.img.envops.common.exception.ExternalServiceException;

public class TrafficExternalServiceException extends ExternalServiceException {
  public TrafficExternalServiceException(String message) {
    super(message);
  }
}
