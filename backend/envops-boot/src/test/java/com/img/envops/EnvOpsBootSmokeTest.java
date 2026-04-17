package com.img.envops;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvOpsBootSmokeTest {
  @Test
  void contextLoadsWithExplicitSecuritySecrets() {
    try (ConfigurableApplicationContext ignored = startApplication(Map.of(
        "envops.security.token-secret", "test-only-envops-token-secret-12345",
        "envops.security.credential-protection-secret", "test-only-envops-credential-protection-secret-12345"))) {
    }
  }

  @Test
  void startupFailsWhenSecuritySecretsAreMissing() {
    assertThatThrownBy(() -> startApplication(Map.of()))
        .hasRootCauseMessage("Could not resolve placeholder 'ENVOPS_SECURITY_TOKEN_SECRET' in value \"${ENVOPS_SECURITY_TOKEN_SECRET}\"");
  }

  private ConfigurableApplicationContext startApplication(Map<String, Object> properties) {
    SpringApplication application = new SpringApplication(EnvOpsApplication.class);
    application.setWebApplicationType(WebApplicationType.NONE);

    return application.run(toArgs(properties));
  }

  private String[] toArgs(Map<String, Object> properties) {
    return properties.entrySet().stream()
        .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
        .toArray(String[]::new);
  }
}
