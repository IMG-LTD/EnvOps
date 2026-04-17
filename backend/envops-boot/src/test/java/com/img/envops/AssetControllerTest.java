package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.asset.application.AssetApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class AssetControllerTest {
  private static final String CREDENTIAL_PROTECTION_SECRET = "test-only-envops-credential-protection-secret-12345";
  private static final String EDGE_SSH_KEY_PLACEHOLDER = "FAKE_EDGE_OPS_SSH_KEY_DO_NOT_USE";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @SpyBean
  private AssetApplicationService assetApplicationService;

  @Test
  void getHostsReturnsPaginatedRecords() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/assets/hosts")
            .param("current", "1")
            .param("size", "10")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.current").value(1))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.total").value(4))
        .andExpect(jsonPath("$.data.summary.managedHosts").value(4))
        .andExpect(jsonPath("$.data.summary.onlineHosts").value(2))
        .andExpect(jsonPath("$.data.summary.warningHosts").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].hostName").value("host-prd-01"));
  }

  @Test
  void createHostReturnsCreatedHost() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/hosts")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "hostName": "host-new-01",
                  "ipAddress": "10.60.1.20",
                  "environment": "sandbox",
                  "clusterName": "cn-shenzhen-a",
                  "ownerName": "Asset Team",
                  "status": "online",
                  "lastHeartbeat": "2026-04-16T11:22:33"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.hostName").value("host-new-01"))
        .andExpect(jsonPath("$.data.ipAddress").value("10.60.1.20"))
        .andExpect(jsonPath("$.data.lastHeartbeat").value("2026-04-16T11:22:33"));
  }

  @Test
  void createCredentialReturnsCreatedCredentialWithoutSecretAndPersistsProtectedValue() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "edge-maintenance-key",
                  "credentialType": "ssh_key",
                  "username": "ops",
                  "secret": "FAKE_EDGE_OPS_SSH_KEY_DO_NOT_USE",
                  "description": "边缘节点巡检临时密钥"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.name").value("edge-maintenance-key"))
        .andExpect(jsonPath("$.data.credentialType").value("ssh_key"))
        .andExpect(jsonPath("$.data.username").value("ops"))
        .andExpect(jsonPath("$.data.secret").doesNotExist());

    String storedSecret = jdbcTemplate.queryForObject(
        "SELECT secret FROM asset_credential WHERE name = ?",
        String.class,
        "edge-maintenance-key");

    org.assertj.core.api.Assertions.assertThat(storedSecret)
        .isEqualTo(protect(EDGE_SSH_KEY_PLACEHOLDER))
        .isNotEqualTo(EDGE_SSH_KEY_PLACEHOLDER)
        .startsWith("protected:v1:");
  }

  @Test
  void createCredentialRejectsUnsupportedCredentialType() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "unsupported-credential",
                  "credentialType": "password",
                  "username": "ops",
                  "secret": "FAKE_PASSWORD_DO_NOT_USE"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("credentialType must be one of ssh_password, ssh_key, api_token"));
  }

  @Test
  void createCredentialRejectsBlankSecret() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/assets/credentials")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "blank-secret-credential",
                  "credentialType": "api_token",
                  "username": "ops",
                  "secret": "   "
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("secret is required"));
  }

  @Test
  void assetInternalFailuresReturn500InsteadOf401() throws Exception {
    String accessToken = login();
    doThrow(new IllegalStateException("simulated asset failure")).when(assetApplicationService).getGroups();

    mockMvc.perform(get("/api/assets/groups")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("500"))
        .andExpect(jsonPath("$.msg").value("Internal server error"));
  }

  private String login() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "envops-admin",
                  "password": "EnvOps@123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private String protect(String rawSecret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(CREDENTIAL_PROTECTION_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(rawSecret.getBytes(StandardCharsets.UTF_8)));
      return "protected:v1:" + encoded;
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("failed to compute expected protected secret", exception);
    }
  }
}
