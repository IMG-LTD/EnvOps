package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class UserControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void getSystemUsersRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/system/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"));
  }

  @Test
  void nonSuperAdminCannotAccessSystemUsers() throws Exception {
    seedUserWithRole(30L, "ops-observer", "Observer@123", 6L, "OPS_OBSERVER", "Ops Observer");
    String accessToken = login("ops-observer", "Observer@123");

    mockMvc.perform(get("/api/system/users")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"))
        .andExpect(jsonPath("$.msg").value("Forbidden"));
  }

  @Test
  void getSystemUsersReturnsSeededUsersAfterLogin() throws Exception {
    String accessToken = login("envops-admin", "EnvOps@123");

    mockMvc.perform(get("/api/system/users")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.length()").value(4))
        .andExpect(jsonPath("$.data[0].userName").value("envops-admin"))
        .andExpect(jsonPath("$.data[0].teamKey").value("platform"))
        .andExpect(jsonPath("$.data[0].loginType").value("PASSWORD"))
        .andExpect(jsonPath("$.data[0].roles", containsInAnyOrder("SUPER_ADMIN", "PLATFORM_ADMIN")))
        .andExpect(jsonPath("$.data[1].userName").value("release-admin"))
        .andExpect(jsonPath("$.data[1].loginType").value("PASSWORD_OTP"))
        .andExpect(jsonPath("$.data[1].roles", containsInAnyOrder("SUPER_ADMIN", "RELEASE_MANAGER")))
        .andExpect(jsonPath("$.data[2].userName").value("traffic-owner"))
        .andExpect(jsonPath("$.data[2].status").value("REVIEW"))
        .andExpect(jsonPath("$.data[2].roles", containsInAnyOrder("TRAFFIC_OWNER")))
        .andExpect(jsonPath("$.data[3].userName").value("qa-observer"))
        .andExpect(jsonPath("$.data[3].status").value("DISABLED"))
        .andExpect(jsonPath("$.data[3].roles", containsInAnyOrder("OBSERVER")));
  }

  @Test
  void createSystemUserReturnsCreatedUserAndPersistsRoles() throws Exception {
    String accessToken = login("envops-admin", "EnvOps@123");

    mockMvc.perform(post("/api/system/users")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "ops-manager",
                  "password": "OpsManager@123",
                  "phone": "13500135000",
                  "teamKey": "envops",
                  "loginType": "PASSWORD_OTP",
                  "status": "ACTIVE",
                  "roles": ["PLATFORM_ADMIN", "OBSERVER"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.userName").value("ops-manager"))
        .andExpect(jsonPath("$.data.teamKey").value("envops"))
        .andExpect(jsonPath("$.data.loginType").value("PASSWORD_OTP"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.roles", containsInAnyOrder("PLATFORM_ADMIN", "OBSERVER")));

    String storedPhone = jdbcTemplate.queryForObject(
        "SELECT phone FROM sys_user WHERE user_name = ?",
        String.class,
        "ops-manager");
    List<String> storedRoles = jdbcTemplate.queryForList(
        """
            SELECT r.role_key
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            JOIN sys_user u ON u.id = ur.user_id
            WHERE u.user_name = ?
            ORDER BY r.id
            """,
        String.class,
        "ops-manager");

    Assertions.assertThat(storedPhone).isEqualTo("13500135000");
    Assertions.assertThat(storedRoles).containsExactlyInAnyOrder("PLATFORM_ADMIN", "OBSERVER");
    Assertions.assertThat(login("ops-manager", "OpsManager@123")).isNotBlank();
  }

  @Test
  void createSystemUserRejectsInvalidLoginType() throws Exception {
    String accessToken = login("envops-admin", "EnvOps@123");

    mockMvc.perform(post("/api/system/users")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "invalid-login-type-user",
                  "password": "Invalid@123",
                  "phone": "13500135001",
                  "teamKey": "platform",
                  "loginType": "PASSWORD_SMS",
                  "status": "ACTIVE",
                  "roles": ["PLATFORM_ADMIN"]
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("loginType must be one of [PASSWORD, PASSWORD_OTP, SSO]"));
  }

  @Test
  void updateSystemUserReturnsUpdatedUserAndReplacesRoles() throws Exception {
    String accessToken = login("envops-admin", "EnvOps@123");

    mockMvc.perform(put("/api/system/users/21")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "traffic-owner",
                  "password": "Traffic@456",
                  "phone": "13700137111",
                  "teamKey": "platform",
                  "loginType": "PASSWORD",
                  "status": "ACTIVE",
                  "roles": ["PLATFORM_ADMIN", "OBSERVER"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.userName").value("traffic-owner"))
        .andExpect(jsonPath("$.data.phone").value("13700137111"))
        .andExpect(jsonPath("$.data.teamKey").value("platform"))
        .andExpect(jsonPath("$.data.loginType").value("PASSWORD"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.roles", containsInAnyOrder("PLATFORM_ADMIN", "OBSERVER")));

    String storedPassword = jdbcTemplate.queryForObject(
        "SELECT password FROM sys_user WHERE id = ?",
        String.class,
        21L);
    List<String> storedRoles = jdbcTemplate.queryForList(
        """
            SELECT r.role_key
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
            ORDER BY r.id
            """,
        String.class,
        21L);

    Assertions.assertThat(storedPassword).isEqualTo("Traffic@456");
    Assertions.assertThat(storedRoles).containsExactlyInAnyOrder("PLATFORM_ADMIN", "OBSERVER");
    Assertions.assertThat(login("traffic-owner", "Traffic@456")).isNotBlank();
  }

  private void seedUserWithRole(Long userId,
                                String userName,
                                String password,
                                Long roleId,
                                String roleKey,
                                String roleName) {
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
        userId,
        userName,
        password,
        "139" + String.format("%08d", userId % 100000000L),
        "ops",
        "PASSWORD",
        "ACTIVE");
    jdbcTemplate.update("INSERT INTO sys_role (id, role_key, role_name) VALUES (?, ?, ?)", roleId, roleKey, roleName);
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
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
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
