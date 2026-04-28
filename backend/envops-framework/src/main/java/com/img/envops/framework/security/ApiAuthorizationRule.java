package com.img.envops.framework.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public record ApiAuthorizationRule(HttpMethod method, String pattern, String menuPermission, String actionPermission) {
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  public boolean matches(HttpMethod requestMethod, String requestPath) {
    return method == requestMethod && PATH_MATCHER.match(pattern, requestPath);
  }

  public static ApiAuthorizationRule read(HttpMethod method, String pattern, String menuPermission) {
    return new ApiAuthorizationRule(method, pattern, menuPermission, null);
  }

  public static ApiAuthorizationRule action(HttpMethod method, String pattern, String menuPermission, String actionPermission) {
    return new ApiAuthorizationRule(method, pattern, menuPermission, actionPermission);
  }
}
