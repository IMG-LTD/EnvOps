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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class RbacControllerTest {
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void seedDataCreatesFixedPermissionPointsAndSystemRbacRoute() {
    Integer permissionCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_permission WHERE enabled = TRUE",
        Integer.class);
    Integer superAdminPermissionCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM sys_role_permission rp
            JOIN sys_role r ON r.id = rp.role_id
            WHERE r.role_key = 'SUPER_ADMIN'
            """,
        Integer.class);
    Integer orphanRoleBindingCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM sys_role_permission rp
            LEFT JOIN sys_role r ON r.id = rp.role_id
            WHERE r.id IS NULL
            """,
        Integer.class);
    Integer orphanPermissionBindingCount = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM sys_role_permission rp
            LEFT JOIN sys_permission p ON p.id = rp.permission_id
            WHERE p.id IS NULL
            """,
        Integer.class);
    String systemRbacRoute = jdbcTemplate.queryForObject(
        "SELECT route_name FROM sys_menu_route WHERE route_name = 'system_rbac'",
        String.class);
    Integer superAdminRbacPermission = jdbcTemplate.queryForObject(
        """
            SELECT COUNT(*)
            FROM sys_role_permission rp
            JOIN sys_role r ON r.id = rp.role_id
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE r.role_key = 'SUPER_ADMIN'
              AND p.permission_key = 'system:role:manage'
            """,
        Integer.class);

    Assertions.assertThat(permissionCount).isEqualTo(49);
    Assertions.assertThat(superAdminPermissionCount).isEqualTo(permissionCount);
    Assertions.assertThat(orphanRoleBindingCount).isZero();
    Assertions.assertThat(orphanPermissionBindingCount).isZero();
    Assertions.assertThat(systemRbacRoute).isEqualTo("system_rbac");
    Assertions.assertThat(superAdminRbacPermission).isEqualTo(1);
  }

  @Test
  void superAdminCanReadRolesAndPermissionTree() throws Exception {
    String token = login("envops-admin", "EnvOps@123");

    mockMvc.perform(get("/api/system/rbac/roles").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data[0].roleKey").value("SUPER_ADMIN"))
        .andExpect(jsonPath("$.data[0].builtIn").value(true));

    MvcResult permissions = mockMvc.perform(get("/api/system/rbac/permissions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data[?(@.moduleKey == 'system')]").exists())
        .andReturn();

    JsonNode permissionData = objectMapper.readTree(permissions.getResponse().getContentAsString()).path("data");
    JsonNode systemModule = findByField(permissionData, "moduleKey", "system");
    Assertions.assertThat(systemModule.path("moduleName").asText()).isEqualTo("System");

    JsonNode systemRbac = findByField(systemModule.path("permissions"), "permissionKey", "system_rbac");
    Assertions.assertThat(systemRbac.path("enabled").asBoolean()).isTrue();

    JsonNode roleManage = findByField(systemRbac.path("children"), "permissionKey", "system:role:manage");
    Assertions.assertThat(roleManage.path("enabled").asBoolean()).isTrue();
  }

  @Test
  void superAdminCanCreateRoleAndReplacePermissions() throws Exception {
    String token = login("envops-admin", "EnvOps@123");

    MvcResult created = mockMvc.perform(post("/api/system/rbac/roles")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "roleKey": "OPS_VIEWER",
                  "roleName": "Ops Viewer",
                  "description": "Ops read only role",
                  "enabled": true
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.roleKey").value("OPS_VIEWER"))
        .andReturn();

    long roleId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

    mockMvc.perform(put("/api/system/rbac/roles/" + roleId + "/permissions")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "permissionKeys": ["home", "task", "task_center", "task_tracking_[id]"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.permissionKeys", org.hamcrest.Matchers.hasItem("task_center")));
  }

  @Test
  void savingUnknownRolePermissionFails() throws Exception {
    String token = login("envops-admin", "EnvOps@123");

    mockMvc.perform(put("/api/system/rbac/roles/5/permissions")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "permissionKeys": ["not:a:permission"]
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"));
  }

  private JsonNode findByField(JsonNode nodes, String fieldName, String expectedValue) {
    for (JsonNode node : nodes) {
      if (expectedValue.equals(node.path(fieldName).asText())) {
        return node;
      }
    }
    Assertions.fail("Expected node with " + fieldName + "=" + expectedValue);
    return objectMapper.createObjectNode();
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
