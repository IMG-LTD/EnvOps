package com.img.envops;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvOpsBootSmokeTest {
  @Test
  void contextLoadsWithDefaultServerPortAndExplicitSecuritySecrets() {
    try (ConfigurableApplicationContext context = startApplication(Map.of(
        "ENVOPS_SECURITY_TOKEN_SECRET", "test-only-envops-token-secret-12345",
        "ENVOPS_CREDENTIAL_PROTECTION_SECRET", "test-only-envops-credential-protection-secret-12345"))) {
      assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("18080");
    }
  }

  @Test
  void contextUsesEnvStyleOverrideForServerPort() {
    try (ConfigurableApplicationContext context = startApplication(Map.of(
        "ENVOPS_SECURITY_TOKEN_SECRET", "test-only-envops-token-secret-12345",
        "ENVOPS_CREDENTIAL_PROTECTION_SECRET", "test-only-envops-credential-protection-secret-12345",
        "ENVOPS_SERVER_PORT", "19090"))) {
      assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("19090");
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
