package com.img.envops;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class RbacControllerTest {
  @Autowired
  private JdbcTemplate jdbcTemplate;

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
}
