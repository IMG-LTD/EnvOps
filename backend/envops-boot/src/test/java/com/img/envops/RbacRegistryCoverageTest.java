package com.img.envops;

import com.img.envops.framework.security.ApiAuthorizationRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

@SpringBootTest
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class RbacRegistryCoverageTest {
  private static final Set<String> PUBLIC_OR_AUTH_ONLY = Set.of(
      "POST /api/auth/login",
      "POST /api/auth/sendCode",
      "POST /api/auth/codeLogin",
      "GET /api/auth/getUserInfo",
      "GET /api/routes/getConstantRoutes",
      "GET /api/routes/getUserRoutes"
  );

  @Autowired
  private RequestMappingHandlerMapping requestMappingHandlerMapping;

  @Autowired
  private ApiAuthorizationRegistry apiAuthorizationRegistry;

  @Test
  void currentApiMappingsAreCoveredByRbacRegistry() {
    Set<String> uncovered = new TreeSet<>();

    for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
      Set<String> paths = info.getPatternValues();
      Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();

      for (String path : paths) {
        if (!path.startsWith("/api/")) {
          continue;
        }

        if (methods.isEmpty()) {
          uncovered.add("ANY " + path);
          continue;
        }

        for (org.springframework.web.bind.annotation.RequestMethod method : methods) {
          String signature = method.name() + " " + path;
          if (PUBLIC_OR_AUTH_ONLY.contains(signature)) {
            continue;
          }

          if (apiAuthorizationRegistry.findRule(HttpMethod.valueOf(method.name()), path).isEmpty()) {
            uncovered.add(signature);
          }
        }
      }
    }

    Assertions.assertThat(uncovered).isEmpty();
  }
}
