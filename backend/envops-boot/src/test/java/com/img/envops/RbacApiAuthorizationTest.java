package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class RbacApiAuthorizationTest {
  @TestConfiguration
  static class UnknownApiTestConfiguration {
    @Bean
    UnknownApiController unknownApiController() {
      return new UnknownApiController();
    }
  }

  @RestController
  static class UnknownApiController {
    @GetMapping("/api/not-registered-rbac-endpoint")
    ResponseEntity<Void> notFound() {
      return ResponseEntity.notFound().build();
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void unauthenticatedKnownApiReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/assets/databases"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));
  }

  @Test
  void publicConstantRoutesEndpointRemainsPublic() throws Exception {
    mockMvc.perform(get("/api/routes/getConstantRoutes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"));
  }

  @Test
  void unknownApiEndpointRequiresAuthenticationButHasNoRbacRule() throws Exception {
    mockMvc.perform(get("/api/not-registered-rbac-endpoint"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));

    seedUserWithPermissions(85L, "unknown-api-user", "UnknownApiUser@123", List.of("home"));
    String token = login("unknown-api-user", "UnknownApiUser@123");

    mockMvc.perform(get("/api/not-registered-rbac-endpoint").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void databaseReadRequiresMenuPermission() throws Exception {
    seedUserWithPermissions(80L, "database-no-menu", "DatabaseNoMenu@123", List.of("home"));
    String token = login("database-no-menu", "DatabaseNoMenu@123");

    mockMvc.perform(get("/api/assets/databases").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void databaseConnectivityRequiresActionPermission() throws Exception {
    seedUserWithPermissions(81L, "database-reader", "DatabaseReader@123", List.of("asset_database"));
    String token = login("database-reader", "DatabaseReader@123");

    mockMvc.perform(post("/api/assets/databases/1/connectivity-check").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void actionPermissionWithoutMenuDoesNotAuthorizeDatabaseConnectivity() throws Exception {
    seedUserWithPermissions(86L, "database-action-only", "DatabaseActionOnly@123", List.of("asset:database:connectivity-check"));
    String token = login("database-action-only", "DatabaseActionOnly@123");

    mockMvc.perform(post("/api/assets/databases/1/connectivity-check").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void contextPathRequestStillEnforcesDatabaseMenuPermission() throws Exception {
    seedUserWithPermissions(87L, "database-context-no-menu", "DatabaseContextNoMenu@123", List.of("home"));
    String token = login("database-context-no-menu", "DatabaseContextNoMenu@123");

    mockMvc.perform(get("/envops/api/assets/databases")
            .contextPath("/envops")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void headRequestUsesGetDatabasePermissionRule() throws Exception {
    seedUserWithPermissions(88L, "database-head-no-menu", "DatabaseHeadNoMenu@123", List.of("home"));
    String token = login("database-head-no-menu", "DatabaseHeadNoMenu@123");

    mockMvc.perform(head("/api/assets/databases").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void deployExecuteRequiresActionPermission() throws Exception {
    seedUserWithPermissions(82L, "deploy-reader", "DeployReader@123", List.of("deploy_task"));
    String token = login("deploy-reader", "DeployReader@123");

    mockMvc.perform(post("/api/deploy/tasks/2001/execute").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void trafficPreviewRequiresActionPermission() throws Exception {
    seedUserWithPermissions(83L, "traffic-reader", "TrafficReader@123", List.of("traffic_controller"));
    String token = login("traffic-reader", "TrafficReader@123");

    mockMvc.perform(post("/api/traffic/policies/3001/preview").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"));
  }

  @Test
  void taskCenterReadSucceedsWithMenuPermission() throws Exception {
    seedUserWithPermissions(84L, "task-reader", "TaskReader@123", List.of("task_center"));
    String token = login("task-reader", "TaskReader@123");

    mockMvc.perform(get("/api/task-center/tasks").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"));
  }

  private void seedUserWithPermissions(Long userId, String userName, String password, List<String> permissionKeys) {
    long roleId = 8000L + userId;
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
        userId,
        userName,
        passwordEncoder.encode(password),
        "139" + String.format("%08d", userId % 100000000L),
        "ops",
        "PASSWORD",
        "ACTIVE");
    jdbcTemplate.update(
        "INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at) VALUES (?, ?, ?, ?, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        roleId,
        "TEST_ROLE_" + userId,
        "Test Role " + userId,
        "Test role");
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);

    for (String permissionKey : permissionKeys) {
      int insertedPermissionCount = jdbcTemplate.update(
          "INSERT INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission WHERE permission_key = ?",
          roleId,
          permissionKey);
      assertThat(insertedPermissionCount)
          .as("permission key %s should exist", permissionKey)
          .isEqualTo(1);
    }
  }

  private String login(String userName, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  \"userName\": \"%s\",
                  \"password\": \"%s\"
                }
                """.formatted(userName, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
