package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.framework.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
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
class AuthRouteControllerTest {
  private static final String FORGED_ACCESS_TOKEN = "access-envops-admin";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Test
  void loginReturnsAccessTokenOnly() throws Exception {
    String accessToken = login();

    org.assertj.core.api.Assertions.assertThat(accessToken)
        .isNotBlank()
        .isNotEqualTo(FORGED_ACCESS_TOKEN);
  }

  @Test
  void loginRejectsInvalidCredentials() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "envops-admin",
                  "password": "wrong-password"
                }
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"))
        .andExpect(jsonPath("$.msg").value("Invalid username or password"));
  }

  @Test
  void getUserInfoRequiresAuth() throws Exception {
    mockMvc.perform(get("/api/auth/getUserInfo"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));
  }

  @Test
  void getUserInfoRejectsRefreshToken() throws Exception {
    String refreshToken = jwtTokenService.createRefreshToken("envops-admin");

    mockMvc.perform(get("/api/auth/getUserInfo")
            .header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));
  }

  @Test
  void getUserInfoRejectsForgedAccessToken() throws Exception {
    mockMvc.perform(get("/api/auth/getUserInfo")
            .header("Authorization", "Bearer " + FORGED_ACCESS_TOKEN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));
  }

  @Test
  void getUserInfoReturnsSeededUser() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/auth/getUserInfo")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.userId").value("1"))
        .andExpect(jsonPath("$.data.userName").value("envops-admin"))
        .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"))
        .andExpect(jsonPath("$.data.buttons[0]").value("envops:dashboard:view"));
  }

  @Test
  void getConstantRoutesReturnsLocalizedMeta() throws Exception {
    mockMvc.perform(get("/api/routes/getConstantRoutes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.data[0].id").exists())
        .andExpect(jsonPath("$.data[0].name").exists())
        .andExpect(jsonPath("$.data[0].path").exists())
        .andExpect(jsonPath("$.data[0].component").exists())
        .andExpect(jsonPath("$.data[0].meta.title").exists())
        .andExpect(jsonPath("$.data[0].meta.i18nKey").value("route.login"))
        .andExpect(jsonPath("$.data[0].meta.icon").exists())
        .andExpect(jsonPath("$.data[0].meta.order").exists());
  }

  @Test
  void getUserRoutesReturnsEnvOpsShellRoutesWithLocalizedMeta() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/routes/getUserRoutes")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.home").value("home"))
        .andExpect(jsonPath("$.data.routes.length()").value(8))
        .andExpect(jsonPath("$.data.routes[0].name").value("home"))
        .andExpect(jsonPath("$.data.routes[0].component").value("layout.base$view.home"))
        .andExpect(jsonPath("$.data.routes[0].meta.i18nKey").value("route.home"))
        .andExpect(jsonPath("$.data.routes[1].name").value("asset"))
        .andExpect(jsonPath("$.data.routes[1].children[0].name").value("asset_host"))
        .andExpect(jsonPath("$.data.routes[1].children[0].component").value("view.asset_host"))
        .andExpect(jsonPath("$.data.routes[1].children[0].meta.i18nKey").value("route.asset_host"))
        .andExpect(jsonPath("$.data.routes[2].name").value("monitor"))
        .andExpect(jsonPath("$.data.routes[2].children.length()").value(2))
        .andExpect(jsonPath("$.data.routes[2].children[0].component").value("view.monitor_detect-task"))
        .andExpect(jsonPath("$.data.routes[2].children[1].name").value("monitor_metric"))
        .andExpect(jsonPath("$.data.routes[2].children[1].component").value("view.monitor_metric"))
        .andExpect(jsonPath("$.data.routes[2].children[1].meta.i18nKey").value("route.monitor_metric"))
        .andExpect(jsonPath("$.data.routes[3].name").value("app"))
        .andExpect(jsonPath("$.data.routes[3].children.length()").value(5))
        .andExpect(jsonPath("$.data.routes[3].children[0].name").value("app_definition"))
        .andExpect(jsonPath("$.data.routes[3].children[0].component").value("view.app_definition"))
        .andExpect(jsonPath("$.data.routes[3].children[1].name").value("app_version"))
        .andExpect(jsonPath("$.data.routes[3].children[1].component").value("view.app_version"))
        .andExpect(jsonPath("$.data.routes[3].children[2].name").value("app_package"))
        .andExpect(jsonPath("$.data.routes[3].children[2].component").value("view.app_package"))
        .andExpect(jsonPath("$.data.routes[3].children[3].name").value("app_config-template"))
        .andExpect(jsonPath("$.data.routes[3].children[3].component").value("view.app_config-template"))
        .andExpect(jsonPath("$.data.routes[3].children[4].name").value("app_script-template"))
        .andExpect(jsonPath("$.data.routes[3].children[4].component").value("view.app_script-template"))
        .andExpect(jsonPath("$.data.routes[4].name").value("deploy"))
        .andExpect(jsonPath("$.data.routes[4].children[0].component").value("view.deploy_task"))
        .andExpect(jsonPath("$.data.routes[5].name").value("task"))
        .andExpect(jsonPath("$.data.routes[5].children[0].component").value("view.task_center"))
        .andExpect(jsonPath("$.data.routes[6].name").value("traffic"))
        .andExpect(jsonPath("$.data.routes[6].children[0].component").value("view.traffic_controller"))
        .andExpect(jsonPath("$.data.routes[7].name").value("system"))
        .andExpect(jsonPath("$.data.routes[7].children[0].name").value("system_user"))
        .andExpect(jsonPath("$.data.routes[7].children[0].component").value("view.system_user"))
        .andExpect(jsonPath("$.data.routes[7].children[0].meta.i18nKey").value("route.system_user"));
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
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
