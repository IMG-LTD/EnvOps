package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
      jdbcTemplate.update(
          "INSERT INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission WHERE permission_key = ?",
          roleId,
          permissionKey);
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
