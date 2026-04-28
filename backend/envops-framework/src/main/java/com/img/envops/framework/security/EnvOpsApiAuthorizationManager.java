package com.img.envops.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Supplier;

@Component
public class EnvOpsApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
  private final ApiAuthorizationRegistry apiAuthorizationRegistry;
  private final EffectivePermissionService effectivePermissionService;

  public EnvOpsApiAuthorizationManager(ApiAuthorizationRegistry apiAuthorizationRegistry,
                                        EffectivePermissionService effectivePermissionService) {
    this.apiAuthorizationRegistry = apiAuthorizationRegistry;
    this.effectivePermissionService = effectivePermissionService;
  }

  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    Authentication currentAuthentication = authentication.get();
    if (currentAuthentication == null
        || !currentAuthentication.isAuthenticated()
        || currentAuthentication instanceof AnonymousAuthenticationToken) {
      return new AuthorizationDecision(false);
    }

    HttpMethod method = normalizeMethod(HttpMethod.valueOf(context.getRequest().getMethod()));
    String path = normalizePath(context.getRequest());
    ApiAuthorizationRule rule = apiAuthorizationRegistry.findRule(method, path).orElse(null);

    if (rule == null) {
      return new AuthorizationDecision(true);
    }

    if (!StringUtils.hasText(rule.menuPermission()) && !StringUtils.hasText(rule.actionPermission())) {
      return new AuthorizationDecision(true);
    }

    Set<String> permissions = effectivePermissionService.findEffectivePermissionKeys(currentAuthentication.getName());
    boolean hasMenuPermission = !StringUtils.hasText(rule.menuPermission()) || permissions.contains(rule.menuPermission());
    boolean hasActionPermission = !StringUtils.hasText(rule.actionPermission()) || permissions.contains(rule.actionPermission());

    return new AuthorizationDecision(hasMenuPermission && hasActionPermission);
  }

  private HttpMethod normalizeMethod(HttpMethod method) {
    if (method == HttpMethod.HEAD) {
      return HttpMethod.GET;
    }
    return method;
  }

  private String normalizePath(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
      String path = requestUri.substring(contextPath.length());
      return stripMatrixParameters(StringUtils.hasText(path) ? path : "/");
    }
    return stripMatrixParameters(requestUri);
  }

  private String stripMatrixParameters(String path) {
    if (!StringUtils.hasText(path) || !path.contains(";")) {
      return path;
    }

    String[] segments = path.split("/", -1);
    for (int index = 0; index < segments.length; index++) {
      int matrixParameterIndex = segments[index].indexOf(';');
      if (matrixParameterIndex >= 0) {
        segments[index] = segments[index].substring(0, matrixParameterIndex);
      }
    }
    return String.join("/", segments);
  }
}
