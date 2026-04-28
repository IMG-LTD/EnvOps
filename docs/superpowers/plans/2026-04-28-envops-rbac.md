# EnvOps v0.0.8 RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build EnvOps v0.0.8 RBAC with fixed permission points, role-first permission management UI, user-role assignment, and backend-authoritative menu/action API authorization.

**Architecture:** Add DB-backed RBAC permissions in `envops-system`, expose role-first management APIs, and enforce current module APIs through a code-defined central authorization registry in `envops-framework`. Frontend dynamic routes remain backend-filtered; action permission codes from user info only drive UI button state and tooltips.

**Tech Stack:** Spring Boot 3.3, Spring Security, Java 21 records/classes, MyBatis annotation mappers, H2 schema/data SQL, JUnit 5, MockMvc, Vue 3, TypeScript, Pinia, Naive UI, elegant-router, vue-i18n, Vitest.

---

## Scope Guardrails

This plan implements only the approved v0.0.8 RBAC scope:

- Fixed permission points seeded by the system.
- Role-first Permission Management UI.
- User-role binding in User Management.
- Permission granularity: menu plus action.
- Full current module coverage: Home, Asset, Monitor, App, Deploy, Task Center, Traffic, System.
- Menu permission is required before module API access.
- Action permission is required for write, execute, approve, rollback, delete, and management operations.
- Backend authorization is authoritative.

This plan must not add:

- UI-created arbitrary API matchers.
- Organization hierarchy or department inheritance.
- Approval flow or audit center.
- Resource-level ownership permissions.
- JWT/login replacement.
- Task Center retry/cancel/orchestration behavior.

## File Structure

### Backend schema and seed data

- Modify `backend/envops-boot/src/main/resources/schema.sql`
  - Add `sys_permission` and `sys_role_permission`.
  - Extend `sys_role` with `description`, `enabled`, `built_in`, `created_at`, `updated_at`.

- Modify `backend/envops-boot/src/test/resources/schema.sql`
  - Mirror production schema.

- Modify `backend/envops-boot/src/main/resources/data.sql`
  - Add `system_rbac` route.
  - Seed menu/action permissions.
  - Seed role-permission defaults.

- Modify `backend/envops-boot/src/test/resources/data.sql`
  - Mirror production seed data.

### Backend authorization and RBAC application

- Create `backend/envops-common/src/main/java/com/img/envops/common/security/PermissionKeys.java`
  - Central constants for menu and action permission keys.

- Create `backend/envops-framework/src/main/java/com/img/envops/framework/security/EffectivePermissionService.java`
  - Framework-owned interface for resolving current user effective permissions.

- Create `backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRule.java`
  - Immutable API rule record.

- Create `backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRegistry.java`
  - Code-defined current API to menu/action permission mapping.

- Create `backend/envops-framework/src/main/java/com/img/envops/framework/security/EnvOpsApiAuthorizationManager.java`
  - Spring Security `AuthorizationManager<RequestAuthorizationContext>`.

- Modify `backend/envops-framework/src/main/java/com/img/envops/framework/security/SecurityConfig.java`
  - Replace hard-coded App/System `SUPER_ADMIN` matchers and blanket `/api/**` auth with central authorization manager.

- Create `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RbacMapper.java`
  - MyBatis access for roles, permissions, role-permission bindings, effective permission resolution, and last-admin checks.

- Create `backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacAuthorizationService.java`
  - Implements `EffectivePermissionService` from `envops-framework`.

- Create `backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacApplicationService.java`
  - Role list/create/update, permission tree, role-permission read/save.

- Create `backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/rbac/RbacController.java`
  - `/api/system/rbac` endpoints.

### Backend auth, route, and user integration

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/AuthApplicationService.java`
  - Return effective action permission codes in `UserInfo.buttons`.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/UserDetailsLookupService.java`
  - Load only enabled role keys as authorities.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/UserAuthMapper.java`
  - Support enabled role queries and role row fields.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RouteMenuMapper.java`
  - Filter routes through menu permissions instead of `required_role`.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/application/route/RouteApplicationService.java`
  - Keep tree building and include routes backed by enabled menu permissions.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/user/UserController.java`
  - Add user-role read/save endpoints.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/application/user/UserApplicationService.java`
  - Add user-role assignment methods and last-admin protection.

### Backend tests

- Create `backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java`
  - RBAC management API tests.

- Create `backend/envops-boot/src/test/java/com/img/envops/RbacApiAuthorizationTest.java`
  - Menu/action authorization behavior across modules.

- Create `backend/envops-boot/src/test/java/com/img/envops/RbacRegistryCoverageTest.java`
  - Current `/api/**` controller mapping coverage against registry.

- Modify `backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java`
  - User info buttons and route filtering expectations.

- Modify `backend/envops-boot/src/test/java/com/img/envops/UserControllerTest.java`
  - User-role assignment and last-admin tests.

### Frontend API, route, page, permissions

- Create `frontend/src/typings/api/system-rbac.d.ts`
  - RBAC role and permission DTOs.

- Create `frontend/src/service/api/system-rbac.ts`
  - RBAC API methods.

- Modify `frontend/src/service/api/index.ts`
  - Export RBAC service.

- Modify `frontend/src/typings/api/system-user.d.ts`
  - User-role assignment DTOs.

- Modify `frontend/src/service/api/system-user.ts`
  - User-role assignment API methods.

- Create `frontend/src/views/system/rbac/index.vue`
  - Role-first Permission Management page.

- Modify generated route artifacts:
  - `frontend/src/router/elegant/routes.ts`
  - `frontend/src/router/elegant/imports.ts`
  - `frontend/src/router/elegant/transform.ts`
  - `frontend/src/typings/elegant-router.d.ts`

- Modify `frontend/src/locales/langs/zh-cn.ts`
  - Chinese route/page/action strings.

- Modify `frontend/src/locales/langs/en-us.ts`
  - English route/page/action strings.

- Modify `frontend/src/typings/app.d.ts`
  - Locale schema and route key typing.

- Modify `frontend/src/views/system/user/index.vue`
  - User-role assignment UI.

- Modify `frontend/src/hooks/business/auth.ts`
  - Add all-permission helper while keeping current `hasAuth` behavior.

- Modify high-risk action pages for permission gating:
  - `frontend/src/views/asset/host/index.vue`
  - `frontend/src/views/asset/credential/index.vue`
  - `frontend/src/views/asset/database/index.vue`
  - `frontend/src/views/monitor/detect-task/index.vue`
  - `frontend/src/views/app/definition/index.vue`
  - `frontend/src/views/app/version/index.vue`
  - `frontend/src/views/app/package/index.vue`
  - `frontend/src/views/app/config-template/index.vue`
  - `frontend/src/views/app/script-template/index.vue`
  - `frontend/src/views/deploy/task/index.vue`
  - `frontend/src/views/traffic/controller/index.vue`

### Frontend tests

- Create `frontend/src/views/system/rbac-contract.spec.ts`
  - RBAC API/page/route/locale contract checks.

- Modify `frontend/src/views/system/user-contract.spec.ts`
  - User-role assignment contract checks.

- Modify existing contract specs where present:
  - `frontend/src/views/asset/database-contract.spec.ts`
  - `frontend/src/views/traffic/traffic-contract.spec.ts`
  - `frontend/src/store/modules/__tests__/route-envops.spec.ts`

- Create focused contract specs if not already present:
  - `frontend/src/views/app/app-rbac-contract.spec.ts`
  - `frontend/src/views/deploy/deploy-rbac-contract.spec.ts`
  - `frontend/src/views/monitor/monitor-rbac-contract.spec.ts`
  - `frontend/src/views/asset/asset-rbac-contract.spec.ts`

### Documentation and release notes

- Modify `README.md`
- Modify `docs/envops-项目详细说明.md`
- Modify `docs/envops-开发技术说明.md`
- Modify `docs/envops-用户操作手册.md`
- Create `release/0.0.8-release-notes.md`

---

## Permission Key Contract

Use these exact menu permission keys. Menu permission keys match route names.

```java
public static final String HOME = "home";
public static final String ASSET = "asset";
public static final String ASSET_HOST = "asset_host";
public static final String ASSET_GROUP = "asset_group";
public static final String ASSET_TAG = "asset_tag";
public static final String ASSET_CREDENTIAL = "asset_credential";
public static final String ASSET_DATABASE = "asset_database";
public static final String MONITOR = "monitor";
public static final String MONITOR_DETECT_TASK = "monitor_detect-task";
public static final String MONITOR_METRIC = "monitor_metric";
public static final String APP = "app";
public static final String APP_DEFINITION = "app_definition";
public static final String APP_VERSION = "app_version";
public static final String APP_PACKAGE = "app_package";
public static final String APP_CONFIG_TEMPLATE = "app_config-template";
public static final String APP_SCRIPT_TEMPLATE = "app_script-template";
public static final String DEPLOY = "deploy";
public static final String DEPLOY_TASK = "deploy_task";
public static final String TASK = "task";
public static final String TASK_CENTER = "task_center";
public static final String TASK_TRACKING = "task_tracking_[id]";
public static final String TRAFFIC = "traffic";
public static final String TRAFFIC_CONTROLLER = "traffic_controller";
public static final String SYSTEM = "system";
public static final String SYSTEM_USER = "system_user";
public static final String SYSTEM_RBAC = "system_rbac";
```

Use these exact action permission keys.

```java
public static final String ASSET_HOST_MANAGE = "asset:host:manage";
public static final String ASSET_GROUP_MANAGE = "asset:group:manage";
public static final String ASSET_TAG_MANAGE = "asset:tag:manage";
public static final String ASSET_CREDENTIAL_MANAGE = "asset:credential:manage";
public static final String ASSET_DATABASE_MANAGE = "asset:database:manage";
public static final String ASSET_DATABASE_CONNECTIVITY_CHECK = "asset:database:connectivity-check";
public static final String MONITOR_DETECT_TASK_EXECUTE = "monitor:detect-task:execute";
public static final String APP_DEFINITION_MANAGE = "app:definition:manage";
public static final String APP_VERSION_MANAGE = "app:version:manage";
public static final String APP_PACKAGE_MANAGE = "app:package:manage";
public static final String APP_CONFIG_TEMPLATE_MANAGE = "app:config-template:manage";
public static final String APP_SCRIPT_TEMPLATE_MANAGE = "app:script-template:manage";
public static final String DEPLOY_TASK_CREATE = "deploy:task:create";
public static final String DEPLOY_TASK_APPROVE = "deploy:task:approve";
public static final String DEPLOY_TASK_EXECUTE = "deploy:task:execute";
public static final String DEPLOY_TASK_CANCEL = "deploy:task:cancel";
public static final String DEPLOY_TASK_RETRY = "deploy:task:retry";
public static final String DEPLOY_TASK_ROLLBACK = "deploy:task:rollback";
public static final String TRAFFIC_POLICY_PREVIEW = "traffic:policy:preview";
public static final String TRAFFIC_POLICY_APPLY = "traffic:policy:apply";
public static final String TRAFFIC_POLICY_ROLLBACK = "traffic:policy:rollback";
public static final String SYSTEM_USER_MANAGE = "system:user:manage";
public static final String SYSTEM_ROLE_MANAGE = "system:role:manage";
```

---

### Task 1: Add RBAC schema and seed permissions

**Files:**
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
- Modify: `backend/envops-boot/src/test/resources/schema.sql`
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
- Create: `backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java`

- [ ] **Step 1: Write the failing seed test**

Create `backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java` with this initial test class:

```java
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
    Integer rolePermissionCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_role_permission",
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
    Assertions.assertThat(rolePermissionCount).isGreaterThan(0);
    Assertions.assertThat(systemRbacRoute).isEqualTo("system_rbac");
    Assertions.assertThat(superAdminRbacPermission).isEqualTo(1);
  }
}
```

- [ ] **Step 2: Run the failing seed test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacControllerTest#seedDataCreatesFixedPermissionPointsAndSystemRbacRoute test
```

Expected: FAIL because `sys_permission` or `sys_role_permission` does not exist.

- [ ] **Step 3: Extend `sys_role` and add RBAC tables**

Modify both `backend/envops-boot/src/main/resources/schema.sql` and `backend/envops-boot/src/test/resources/schema.sql`.

Add drops before `sys_role` dependent tables:

```sql
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
```

Replace the `sys_role` table definition with a compatible expanded definition:

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    role_key VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Add:

```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY,
    permission_key VARCHAR(128) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL,
    permission_type VARCHAR(32) NOT NULL,
    module_key VARCHAR(64) NOT NULL,
    parent_key VARCHAR(128),
    route_name VARCHAR(128),
    action_key VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);
```

- [ ] **Step 4: Expand seeded roles**

Modify both data files. Replace the current `INSERT INTO sys_role (id, role_key, role_name)` with:

```sql
INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at)
VALUES
    (1, 'SUPER_ADMIN', 'Super Admin', 'Built-in full platform administrator', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'PLATFORM_ADMIN', 'Platform Admin', 'Platform operator for assets and monitor data', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'RELEASE_MANAGER', 'Release Manager', 'Release operator for app and deploy flows', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'TRAFFIC_OWNER', 'Traffic Owner', 'Traffic policy operator', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'OBSERVER', 'Observer', 'Conservative read-only observer', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

- [ ] **Step 5: Add the `system_rbac` menu route**

In both data files, add this row after `system_user`:

```sql
    (272, 270, 'system_rbac', '/system/rbac', 'view.system_rbac', '权限管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL);
```

If the existing `sys_menu_route` insert is a single multi-row statement, add a comma after row `271` and append row `272` before the terminating semicolon.

- [ ] **Step 6: Seed fixed permissions**

Add this `sys_permission` seed block to both data files after `sys_menu_route` rows:

```sql
INSERT INTO sys_permission (id, permission_key, permission_name, permission_type, module_key, parent_key, route_name, action_key, sort_order, enabled)
VALUES
    (1000, 'home', 'Home', 'menu', 'home', NULL, 'home', NULL, 1, TRUE),
    (1100, 'asset', 'Asset', 'menu', 'asset', NULL, 'asset', NULL, 10, TRUE),
    (1110, 'asset_host', 'Host Management', 'menu', 'asset', 'asset', 'asset_host', NULL, 11, TRUE),
    (1111, 'asset:host:manage', 'Manage Hosts', 'action', 'asset', 'asset_host', NULL, 'manage', 12, TRUE),
    (1120, 'asset_group', 'Group Management', 'menu', 'asset', 'asset', 'asset_group', NULL, 13, TRUE),
    (1121, 'asset:group:manage', 'Manage Groups', 'action', 'asset', 'asset_group', NULL, 'manage', 14, TRUE),
    (1130, 'asset_tag', 'Tag Management', 'menu', 'asset', 'asset', 'asset_tag', NULL, 15, TRUE),
    (1131, 'asset:tag:manage', 'Manage Tags', 'action', 'asset', 'asset_tag', NULL, 'manage', 16, TRUE),
    (1140, 'asset_credential', 'Credential Management', 'menu', 'asset', 'asset', 'asset_credential', NULL, 17, TRUE),
    (1141, 'asset:credential:manage', 'Manage Credentials', 'action', 'asset', 'asset_credential', NULL, 'manage', 18, TRUE),
    (1150, 'asset_database', 'Database Resources', 'menu', 'asset', 'asset', 'asset_database', NULL, 19, TRUE),
    (1151, 'asset:database:manage', 'Manage Databases', 'action', 'asset', 'asset_database', NULL, 'manage', 20, TRUE),
    (1152, 'asset:database:connectivity-check', 'Run Database Connectivity Checks', 'action', 'asset', 'asset_database', NULL, 'connectivity-check', 21, TRUE),
    (1200, 'monitor', 'Monitor', 'menu', 'monitor', NULL, 'monitor', NULL, 30, TRUE),
    (1210, 'monitor_detect-task', 'Detect Task', 'menu', 'monitor', 'monitor', 'monitor_detect-task', NULL, 31, TRUE),
    (1211, 'monitor:detect-task:execute', 'Execute Detect Tasks', 'action', 'monitor', 'monitor_detect-task', NULL, 'execute', 32, TRUE),
    (1220, 'monitor_metric', 'Metric Snapshot', 'menu', 'monitor', 'monitor', 'monitor_metric', NULL, 33, TRUE),
    (1300, 'app', 'App', 'menu', 'app', NULL, 'app', NULL, 40, TRUE),
    (1310, 'app_definition', 'App Definition', 'menu', 'app', 'app', 'app_definition', NULL, 41, TRUE),
    (1311, 'app:definition:manage', 'Manage App Definitions', 'action', 'app', 'app_definition', NULL, 'manage', 42, TRUE),
    (1320, 'app_version', 'App Version', 'menu', 'app', 'app', 'app_version', NULL, 43, TRUE),
    (1321, 'app:version:manage', 'Manage App Versions', 'action', 'app', 'app_version', NULL, 'manage', 44, TRUE),
    (1330, 'app_package', 'App Package', 'menu', 'app', 'app', 'app_package', NULL, 45, TRUE),
    (1331, 'app:package:manage', 'Manage App Packages', 'action', 'app', 'app_package', NULL, 'manage', 46, TRUE),
    (1340, 'app_config-template', 'Config Template', 'menu', 'app', 'app', 'app_config-template', NULL, 47, TRUE),
    (1341, 'app:config-template:manage', 'Manage Config Templates', 'action', 'app', 'app_config-template', NULL, 'manage', 48, TRUE),
    (1350, 'app_script-template', 'Script Template', 'menu', 'app', 'app', 'app_script-template', NULL, 49, TRUE),
    (1351, 'app:script-template:manage', 'Manage Script Templates', 'action', 'app', 'app_script-template', NULL, 'manage', 50, TRUE),
    (1400, 'deploy', 'Deploy', 'menu', 'deploy', NULL, 'deploy', NULL, 60, TRUE),
    (1410, 'deploy_task', 'Deploy Task', 'menu', 'deploy', 'deploy', 'deploy_task', NULL, 61, TRUE),
    (1411, 'deploy:task:create', 'Create Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'create', 62, TRUE),
    (1412, 'deploy:task:approve', 'Approve Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'approve', 63, TRUE),
    (1413, 'deploy:task:execute', 'Execute Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'execute', 64, TRUE),
    (1414, 'deploy:task:cancel', 'Cancel Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'cancel', 65, TRUE),
    (1415, 'deploy:task:retry', 'Retry Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'retry', 66, TRUE),
    (1416, 'deploy:task:rollback', 'Rollback Deploy Tasks', 'action', 'deploy', 'deploy_task', NULL, 'rollback', 67, TRUE),
    (1500, 'task', 'Task', 'menu', 'task', NULL, 'task', NULL, 70, TRUE),
    (1510, 'task_center', 'Task Center', 'menu', 'task', 'task', 'task_center', NULL, 71, TRUE),
    (1511, 'task_tracking_[id]', 'Task Tracking', 'menu', 'task', 'task_center', 'task_tracking_[id]', NULL, 72, TRUE),
    (1600, 'traffic', 'Traffic', 'menu', 'traffic', NULL, 'traffic', NULL, 80, TRUE),
    (1610, 'traffic_controller', 'Traffic Controller', 'menu', 'traffic', 'traffic', 'traffic_controller', NULL, 81, TRUE),
    (1611, 'traffic:policy:preview', 'Preview Traffic Policies', 'action', 'traffic', 'traffic_controller', NULL, 'preview', 82, TRUE),
    (1612, 'traffic:policy:apply', 'Apply Traffic Policies', 'action', 'traffic', 'traffic_controller', NULL, 'apply', 83, TRUE),
    (1613, 'traffic:policy:rollback', 'Rollback Traffic Policies', 'action', 'traffic', 'traffic_controller', NULL, 'rollback', 84, TRUE),
    (1700, 'system', 'System', 'menu', 'system', NULL, 'system', NULL, 90, TRUE),
    (1710, 'system_user', 'System User', 'menu', 'system', 'system', 'system_user', NULL, 91, TRUE),
    (1711, 'system:user:manage', 'Manage System Users', 'action', 'system', 'system_user', NULL, 'manage', 92, TRUE),
    (1720, 'system_rbac', 'Permission Management', 'menu', 'system', 'system', 'system_rbac', NULL, 93, TRUE),
    (1721, 'system:role:manage', 'Manage Roles and Permissions', 'action', 'system', 'system_rbac', NULL, 'manage', 94, TRUE);
```

- [ ] **Step 7: Seed role-permission defaults**

Add these blocks to both data files:

```sql
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE enabled = TRUE;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE permission_key IN (
    'home',
    'asset', 'asset_host', 'asset:host:manage', 'asset_group', 'asset_tag',
    'asset_credential', 'asset:credential:manage', 'asset_database', 'asset:database:manage', 'asset:database:connectivity-check',
    'monitor', 'monitor_detect-task', 'monitor:detect-task:execute', 'monitor_metric',
    'task', 'task_center', 'task_tracking_[id]'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE permission_key IN (
    'home',
    'app', 'app_definition', 'app_version', 'app_package', 'app_config-template', 'app_script-template',
    'deploy', 'deploy_task', 'deploy:task:create', 'deploy:task:approve', 'deploy:task:execute',
    'deploy:task:cancel', 'deploy:task:retry', 'deploy:task:rollback',
    'task', 'task_center', 'task_tracking_[id]'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission
WHERE permission_key IN (
    'home',
    'traffic', 'traffic_controller', 'traffic:policy:preview', 'traffic:policy:apply', 'traffic:policy:rollback',
    'task', 'task_center', 'task_tracking_[id]'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 5, id FROM sys_permission
WHERE permission_key IN ('home', 'task', 'task_center', 'task_tracking_[id]');
```

- [ ] **Step 8: Run the seed test and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacControllerTest#seedDataCreatesFixedPermissionPointsAndSystemRbacRoute test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-boot/src/main/resources/schema.sql backend/envops-boot/src/test/resources/schema.sql backend/envops-boot/src/main/resources/data.sql backend/envops-boot/src/test/resources/data.sql backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java
git commit -m "feat: seed rbac permissions"
```

---

### Task 2: Add backend permission constants and RBAC mapper/service skeleton

**Files:**
- Create: `backend/envops-common/src/main/java/com/img/envops/common/security/PermissionKeys.java`
- Create: `backend/envops-framework/src/main/java/com/img/envops/framework/security/EffectivePermissionService.java`
- Create: `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RbacMapper.java`
- Create: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacAuthorizationService.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/AuthApplicationService.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/UserDetailsLookupService.java`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java`

- [ ] **Step 1: Write the failing user-info permission test**

Modify `getUserInfoReturnsSeededUser` in `AuthRouteControllerTest`:

```java
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
      .andExpect(jsonPath("$.data.buttons").isArray())
      .andExpect(jsonPath("$.data.buttons", org.hamcrest.Matchers.hasItem("system:role:manage")))
      .andExpect(jsonPath("$.data.buttons", org.hamcrest.Matchers.hasItem("system:user:manage")))
      .andExpect(jsonPath("$.data.buttons", org.hamcrest.Matchers.hasItem("asset:database:manage")));
}
```

- [ ] **Step 2: Run the failing user-info test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest#getUserInfoReturnsSeededUser test
```

Expected: FAIL because `buttons[0]` is still the hard-coded `envops:dashboard:view`.

- [ ] **Step 3: Create permission constants**

Create `PermissionKeys.java`:

```java
package com.img.envops.common.security;

public final class PermissionKeys {
  private PermissionKeys() {
  }

  public static final class Menu {
    private Menu() {
    }

    public static final String HOME = "home";
    public static final String ASSET = "asset";
    public static final String ASSET_HOST = "asset_host";
    public static final String ASSET_GROUP = "asset_group";
    public static final String ASSET_TAG = "asset_tag";
    public static final String ASSET_CREDENTIAL = "asset_credential";
    public static final String ASSET_DATABASE = "asset_database";
    public static final String MONITOR = "monitor";
    public static final String MONITOR_DETECT_TASK = "monitor_detect-task";
    public static final String MONITOR_METRIC = "monitor_metric";
    public static final String APP = "app";
    public static final String APP_DEFINITION = "app_definition";
    public static final String APP_VERSION = "app_version";
    public static final String APP_PACKAGE = "app_package";
    public static final String APP_CONFIG_TEMPLATE = "app_config-template";
    public static final String APP_SCRIPT_TEMPLATE = "app_script-template";
    public static final String DEPLOY = "deploy";
    public static final String DEPLOY_TASK = "deploy_task";
    public static final String TASK = "task";
    public static final String TASK_CENTER = "task_center";
    public static final String TASK_TRACKING = "task_tracking_[id]";
    public static final String TRAFFIC = "traffic";
    public static final String TRAFFIC_CONTROLLER = "traffic_controller";
    public static final String SYSTEM = "system";
    public static final String SYSTEM_USER = "system_user";
    public static final String SYSTEM_RBAC = "system_rbac";
  }

  public static final class Action {
    private Action() {
    }

    public static final String ASSET_HOST_MANAGE = "asset:host:manage";
    public static final String ASSET_GROUP_MANAGE = "asset:group:manage";
    public static final String ASSET_TAG_MANAGE = "asset:tag:manage";
    public static final String ASSET_CREDENTIAL_MANAGE = "asset:credential:manage";
    public static final String ASSET_DATABASE_MANAGE = "asset:database:manage";
    public static final String ASSET_DATABASE_CONNECTIVITY_CHECK = "asset:database:connectivity-check";
    public static final String MONITOR_DETECT_TASK_EXECUTE = "monitor:detect-task:execute";
    public static final String APP_DEFINITION_MANAGE = "app:definition:manage";
    public static final String APP_VERSION_MANAGE = "app:version:manage";
    public static final String APP_PACKAGE_MANAGE = "app:package:manage";
    public static final String APP_CONFIG_TEMPLATE_MANAGE = "app:config-template:manage";
    public static final String APP_SCRIPT_TEMPLATE_MANAGE = "app:script-template:manage";
    public static final String DEPLOY_TASK_CREATE = "deploy:task:create";
    public static final String DEPLOY_TASK_APPROVE = "deploy:task:approve";
    public static final String DEPLOY_TASK_EXECUTE = "deploy:task:execute";
    public static final String DEPLOY_TASK_CANCEL = "deploy:task:cancel";
    public static final String DEPLOY_TASK_RETRY = "deploy:task:retry";
    public static final String DEPLOY_TASK_ROLLBACK = "deploy:task:rollback";
    public static final String TRAFFIC_POLICY_PREVIEW = "traffic:policy:preview";
    public static final String TRAFFIC_POLICY_APPLY = "traffic:policy:apply";
    public static final String TRAFFIC_POLICY_ROLLBACK = "traffic:policy:rollback";
    public static final String SYSTEM_USER_MANAGE = "system:user:manage";
    public static final String SYSTEM_ROLE_MANAGE = "system:role:manage";
  }
}
```

- [ ] **Step 4: Create the framework permission service interface**

Create `EffectivePermissionService.java`:

```java
package com.img.envops.framework.security;

import java.util.Set;

public interface EffectivePermissionService {
  Set<String> findEffectivePermissionKeys(String username);
}
```

- [ ] **Step 5: Create `RbacMapper`**

Create `RbacMapper.java`:

```java
package com.img.envops.modules.system.infrastructure.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RbacMapper {

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      JOIN sys_user u ON u.id = ur.user_id
      WHERE u.user_name = #{userName}
        AND u.status = 'ACTIVE'
        AND r.enabled = TRUE
        AND p.enabled = TRUE
      ORDER BY p.permission_key
      """)
  List<String> findEffectivePermissionKeysByUserName(@Param("userName") String userName);

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
        AND r.enabled = TRUE
        AND p.enabled = TRUE
      ORDER BY p.permission_key
      """)
  List<String> findEffectivePermissionKeysByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      JOIN sys_user u ON u.id = ur.user_id
      WHERE u.user_name = #{userName}
        AND u.status = 'ACTIVE'
        AND r.enabled = TRUE
        AND p.enabled = TRUE
        AND p.permission_type = 'action'
      ORDER BY p.permission_key
      """)
  List<String> findEffectiveActionPermissionKeysByUserName(@Param("userName") String userName);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      ORDER BY id
      """)
  List<RoleRow> findRoles();

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      WHERE id = #{roleId}
      """)
  RoleRow findRoleById(@Param("roleId") Long roleId);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      WHERE role_key = #{roleKey}
      """)
  RoleRow findRoleByKey(@Param("roleKey") String roleKey);

  @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM sys_role")
  Long nextRoleId();

  @Insert("""
      INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at)
      VALUES (#{roleId}, #{roleKey}, #{roleName}, #{description}, #{enabled}, #{builtIn}, #{createdAt}, #{updatedAt})
      """)
  int insertRole(RoleEntity entity);

  @Update("""
      UPDATE sys_role
      SET role_name = #{roleName},
          description = #{description},
          enabled = #{enabled},
          updated_at = #{updatedAt}
      WHERE id = #{roleId}
      """)
  int updateRole(RoleEntity entity);

  @Select("""
      SELECT id AS permissionId,
             permission_key AS permissionKey,
             permission_name AS permissionName,
             permission_type AS permissionType,
             module_key AS moduleKey,
             parent_key AS parentKey,
             route_name AS routeName,
             action_key AS actionKey,
             sort_order AS sortOrder,
             enabled
      FROM sys_permission
      WHERE enabled = TRUE
      ORDER BY sort_order, id
      """)
  List<PermissionRow> findEnabledPermissions();

  @Select("""
      SELECT id AS permissionId,
             permission_key AS permissionKey,
             permission_name AS permissionName,
             permission_type AS permissionType,
             module_key AS moduleKey,
             parent_key AS parentKey,
             route_name AS routeName,
             action_key AS actionKey,
             sort_order AS sortOrder,
             enabled
      FROM sys_permission
      WHERE permission_key = #{permissionKey}
      """)
  PermissionRow findPermissionByKey(@Param("permissionKey") String permissionKey);

  @Select("""
      SELECT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      WHERE rp.role_id = #{roleId}
      ORDER BY p.sort_order, p.id
      """)
  List<String> findRolePermissionKeys(@Param("roleId") Long roleId);

  @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
  int deleteRolePermissions(@Param("roleId") Long roleId);

  @Insert("""
      INSERT INTO sys_role_permission (role_id, permission_id)
      SELECT #{roleId}, id
      FROM sys_permission
      WHERE permission_key = #{permissionKey}
      """)
  int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionKey") String permissionKey);

  @Select("""
      SELECT COUNT(*)
      FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id
      JOIN sys_role r ON r.id = ur.role_id
      WHERE u.status = 'ACTIVE'
        AND r.role_key = 'SUPER_ADMIN'
        AND r.enabled = TRUE
      """)
  int countActiveSuperAdminUsers();

  class RoleRow {
    private Long roleId;
    private String roleKey;
    private String roleName;
    private String description;
    private Boolean enabled;
    private Boolean builtIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  }

  class RoleEntity {
    private Long roleId;
    private String roleKey;
    private String roleName;
    private String description;
    private Boolean enabled;
    private Boolean builtIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  }

  class PermissionRow {
    private Long permissionId;
    private String permissionKey;
    private String permissionName;
    private String permissionType;
    private String moduleKey;
    private String parentKey;
    private String routeName;
    private String actionKey;
    private Integer sortOrder;
    private Boolean enabled;

    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getParentKey() { return parentKey; }
    public void setParentKey(String parentKey) { this.parentKey = parentKey; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
  }
}
```

- [ ] **Step 6: Create `RbacAuthorizationService`**

Create:

```java
package com.img.envops.modules.system.application.rbac;

import com.img.envops.framework.security.EffectivePermissionService;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RbacAuthorizationService implements EffectivePermissionService {
  private final RbacMapper rbacMapper;

  public RbacAuthorizationService(RbacMapper rbacMapper) {
    this.rbacMapper = rbacMapper;
  }

  @Override
  public Set<String> findEffectivePermissionKeys(String username) {
    if (!StringUtils.hasText(username)) {
      return Set.of();
    }

    return new LinkedHashSet<>(rbacMapper.findEffectivePermissionKeysByUserName(username.trim()));
  }

  public Set<String> findEffectiveActionPermissionKeys(String username) {
    if (!StringUtils.hasText(username)) {
      return Set.of();
    }

    return new LinkedHashSet<>(rbacMapper.findEffectiveActionPermissionKeysByUserName(username.trim()));
  }
}
```

- [ ] **Step 7: Update user info buttons**

Modify `AuthApplicationService`:

```java
private final RbacAuthorizationService rbacAuthorizationService;

public AuthApplicationService(UserAuthMapper userAuthMapper,
                              JwtTokenService jwtTokenService,
                              PasswordEncoder passwordEncoder,
                              RbacAuthorizationService rbacAuthorizationService) {
  this.userAuthMapper = userAuthMapper;
  this.jwtTokenService = jwtTokenService;
  this.passwordEncoder = passwordEncoder;
  this.rbacAuthorizationService = rbacAuthorizationService;
}
```

Remove `DEFAULT_BUTTON` and update `getUserInfo`:

```java
public UserInfo getUserInfo(String userName) {
  UserAuthRow user = requireUser(userName);
  List<String> roles = userAuthMapper.findEnabledRoleKeysByUserId(user.getUserId());
  List<String> buttons = rbacAuthorizationService.findEffectiveActionPermissionKeys(user.getUserName()).stream().toList();

  return new UserInfo(
      String.valueOf(user.getUserId()),
      user.getUserName(),
      roles,
      buttons);
}
```

- [ ] **Step 8: Add enabled role query**

Modify `UserAuthMapper`:

```java
@Select("""
    SELECT r.role_key
    FROM sys_role r
    JOIN sys_user_role ur ON ur.role_id = r.id
    WHERE ur.user_id = #{userId}
      AND r.enabled = TRUE
    ORDER BY r.id
    """)
List<String> findEnabledRoleKeysByUserId(@Param("userId") Long userId);
```

Update `UserDetailsLookupService.loadAuthorities` to call `findEnabledRoleKeysByUserId` instead of `findRoleKeysByUserId`.

- [ ] **Step 9: Run user-info test and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest#getUserInfoReturnsSeededUser test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-common/src/main/java/com/img/envops/common/security/PermissionKeys.java backend/envops-framework/src/main/java/com/img/envops/framework/security/EffectivePermissionService.java backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RbacMapper.java backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacAuthorizationService.java backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/AuthApplicationService.java backend/envops-system/src/main/java/com/img/envops/modules/system/application/auth/UserDetailsLookupService.java backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/UserAuthMapper.java backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java
git commit -m "feat: resolve effective rbac permissions"
```

---

### Task 3: Add central API authorization registry and Spring Security enforcement

**Files:**
- Create: `backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRule.java`
- Create: `backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRegistry.java`
- Create: `backend/envops-framework/src/main/java/com/img/envops/framework/security/EnvOpsApiAuthorizationManager.java`
- Modify: `backend/envops-framework/src/main/java/com/img/envops/framework/security/SecurityConfig.java`
- Create: `backend/envops-boot/src/test/java/com/img/envops/RbacApiAuthorizationTest.java`

- [ ] **Step 1: Write the failing API authorization tests**

Create `RbacApiAuthorizationTest.java`:

```java
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
```

- [ ] **Step 2: Run the failing authorization tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacApiAuthorizationTest test
```

Expected: FAIL because authenticated users can still call several module APIs without RBAC checks.

- [ ] **Step 3: Create `ApiAuthorizationRule`**

```java
package com.img.envops.framework.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public record ApiAuthorizationRule(HttpMethod method, String pattern, String menuPermission, String actionPermission) {
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  public boolean matches(HttpMethod requestMethod, String requestPath) {
    return method == requestMethod && PATH_MATCHER.match(pattern, requestPath);
  }

  public static ApiAuthorizationRule read(HttpMethod method, String pattern, String menuPermission) {
    return new ApiAuthorizationRule(method, pattern, menuPermission, null);
  }

  public static ApiAuthorizationRule action(HttpMethod method, String pattern, String menuPermission, String actionPermission) {
    return new ApiAuthorizationRule(method, pattern, menuPermission, actionPermission);
  }
}
```

- [ ] **Step 4: Create `ApiAuthorizationRegistry` with exact v0.0.8 mappings**

Create `ApiAuthorizationRegistry.java` with rules in this order:

```java
package com.img.envops.framework.security;

import com.img.envops.common.security.PermissionKeys;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ApiAuthorizationRegistry {
  private final List<ApiAuthorizationRule> rules = List.of(
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/auth/getUserInfo", null),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/routes/getUserRoutes", null),

      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/databases/*/connectivity-check", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_CONNECTIVITY_CHECK),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/databases/connectivity-check:selected", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_CONNECTIVITY_CHECK),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/databases/connectivity-check:page", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_CONNECTIVITY_CHECK),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/databases/connectivity-check:query", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_CONNECTIVITY_CHECK),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/databases", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/assets/databases/*", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/assets/databases/*", PermissionKeys.Menu.ASSET_DATABASE, PermissionKeys.Action.ASSET_DATABASE_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/assets/databases/**", PermissionKeys.Menu.ASSET_DATABASE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/hosts", PermissionKeys.Menu.ASSET_HOST, PermissionKeys.Action.ASSET_HOST_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/assets/hosts/**", PermissionKeys.Menu.ASSET_HOST),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/assets/credentials", PermissionKeys.Menu.ASSET_CREDENTIAL, PermissionKeys.Action.ASSET_CREDENTIAL_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/assets/credentials/**", PermissionKeys.Menu.ASSET_CREDENTIAL),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/assets/groups/**", PermissionKeys.Menu.ASSET_GROUP),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/assets/tags/**", PermissionKeys.Menu.ASSET_TAG),

      ApiAuthorizationRule.action(HttpMethod.POST, "/api/monitor/detect-tasks/*/execute", PermissionKeys.Menu.MONITOR_DETECT_TASK, PermissionKeys.Action.MONITOR_DETECT_TASK_EXECUTE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/monitor/detect-tasks", PermissionKeys.Menu.MONITOR_DETECT_TASK, PermissionKeys.Action.MONITOR_DETECT_TASK_EXECUTE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/monitor/detect-tasks/**", PermissionKeys.Menu.MONITOR_DETECT_TASK),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/monitor/hosts/*/facts/latest", PermissionKeys.Menu.MONITOR_METRIC),

      ApiAuthorizationRule.action(HttpMethod.POST, "/api/apps", PermissionKeys.Menu.APP_DEFINITION, PermissionKeys.Action.APP_DEFINITION_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/apps/*", PermissionKeys.Menu.APP_DEFINITION, PermissionKeys.Action.APP_DEFINITION_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/apps/*", PermissionKeys.Menu.APP_DEFINITION, PermissionKeys.Action.APP_DEFINITION_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/apps/*/versions", PermissionKeys.Menu.APP_VERSION),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/apps/*/versions", PermissionKeys.Menu.APP_VERSION, PermissionKeys.Action.APP_VERSION_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/apps/**", PermissionKeys.Menu.APP_DEFINITION),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/app-versions/*", PermissionKeys.Menu.APP_VERSION, PermissionKeys.Action.APP_VERSION_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/app-versions/*", PermissionKeys.Menu.APP_VERSION, PermissionKeys.Action.APP_VERSION_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/packages/upload", PermissionKeys.Menu.APP_PACKAGE, PermissionKeys.Action.APP_PACKAGE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/packages/*", PermissionKeys.Menu.APP_PACKAGE, PermissionKeys.Action.APP_PACKAGE_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/packages/**", PermissionKeys.Menu.APP_PACKAGE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/config-templates", PermissionKeys.Menu.APP_CONFIG_TEMPLATE, PermissionKeys.Action.APP_CONFIG_TEMPLATE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/config-templates/*", PermissionKeys.Menu.APP_CONFIG_TEMPLATE, PermissionKeys.Action.APP_CONFIG_TEMPLATE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/config-templates/*", PermissionKeys.Menu.APP_CONFIG_TEMPLATE, PermissionKeys.Action.APP_CONFIG_TEMPLATE_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/config-templates/**", PermissionKeys.Menu.APP_CONFIG_TEMPLATE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/script-templates", PermissionKeys.Menu.APP_SCRIPT_TEMPLATE, PermissionKeys.Action.APP_SCRIPT_TEMPLATE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/script-templates/*", PermissionKeys.Menu.APP_SCRIPT_TEMPLATE, PermissionKeys.Action.APP_SCRIPT_TEMPLATE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.DELETE, "/api/script-templates/*", PermissionKeys.Menu.APP_SCRIPT_TEMPLATE, PermissionKeys.Action.APP_SCRIPT_TEMPLATE_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/script-templates/**", PermissionKeys.Menu.APP_SCRIPT_TEMPLATE),

      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/approve", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_APPROVE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/reject", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_APPROVE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/execute", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_EXECUTE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/cancel", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_CANCEL),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/retry", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_RETRY),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks/*/rollback", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_ROLLBACK),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/deploy/tasks", PermissionKeys.Menu.DEPLOY_TASK, PermissionKeys.Action.DEPLOY_TASK_CREATE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/deploy/tasks/**", PermissionKeys.Menu.DEPLOY_TASK),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/deploy/executors", PermissionKeys.Menu.DEPLOY_TASK),

      ApiAuthorizationRule.read(HttpMethod.GET, "/api/task-center/tasks/**", PermissionKeys.Menu.TASK_CENTER),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/traffic/policies", PermissionKeys.Menu.TRAFFIC_CONTROLLER),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/traffic/plugins", PermissionKeys.Menu.TRAFFIC_CONTROLLER),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/traffic/policies/*/preview", PermissionKeys.Menu.TRAFFIC_CONTROLLER, PermissionKeys.Action.TRAFFIC_POLICY_PREVIEW),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/traffic/policies/*/apply", PermissionKeys.Menu.TRAFFIC_CONTROLLER, PermissionKeys.Action.TRAFFIC_POLICY_APPLY),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/traffic/policies/*/rollback", PermissionKeys.Menu.TRAFFIC_CONTROLLER, PermissionKeys.Action.TRAFFIC_POLICY_ROLLBACK),

      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/system/users/*/roles", PermissionKeys.Menu.SYSTEM_USER, PermissionKeys.Action.SYSTEM_USER_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/system/users/*/roles", PermissionKeys.Menu.SYSTEM_USER),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/system/users", PermissionKeys.Menu.SYSTEM_USER, PermissionKeys.Action.SYSTEM_USER_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/system/users/*", PermissionKeys.Menu.SYSTEM_USER, PermissionKeys.Action.SYSTEM_USER_MANAGE),
      ApiAuthorizationRule.read(HttpMethod.GET, "/api/system/users", PermissionKeys.Menu.SYSTEM_USER),
      ApiAuthorizationRule.action(HttpMethod.GET, "/api/system/rbac/**", PermissionKeys.Menu.SYSTEM_RBAC, PermissionKeys.Action.SYSTEM_ROLE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.POST, "/api/system/rbac/**", PermissionKeys.Menu.SYSTEM_RBAC, PermissionKeys.Action.SYSTEM_ROLE_MANAGE),
      ApiAuthorizationRule.action(HttpMethod.PUT, "/api/system/rbac/**", PermissionKeys.Menu.SYSTEM_RBAC, PermissionKeys.Action.SYSTEM_ROLE_MANAGE)
  );

  public Optional<ApiAuthorizationRule> findRule(HttpMethod method, String path) {
    return rules.stream().filter(rule -> rule.matches(method, path)).findFirst();
  }

  public List<ApiAuthorizationRule> rules() {
    return rules;
  }
}
```

- [ ] **Step 5: Create `EnvOpsApiAuthorizationManager`**

```java
package com.img.envops.framework.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Supplier;

@Component
public class EnvOpsApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
  private final ApiAuthorizationRegistry apiAuthorizationRegistry;
  private final EffectivePermissionService effectivePermissionService;

  public EnvOpsApiAuthorizationManager(ApiAuthorizationRegistry apiAuthorizationRegistry,
                                        EffectivePermissionService effectivePermissionService) {
    this.apiAuthorizationRegistry = apiAuthorizationRegistry;
    this.effectivePermissionService = effectivePermissionService;
  }

  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    Authentication currentAuthentication = authentication.get();
    if (currentAuthentication == null || !currentAuthentication.isAuthenticated()) {
      return new AuthorizationDecision(false);
    }

    HttpMethod method = HttpMethod.valueOf(context.getRequest().getMethod());
    String path = context.getRequest().getRequestURI();
    ApiAuthorizationRule rule = apiAuthorizationRegistry.findRule(method, path).orElse(null);

    if (rule == null) {
      return new AuthorizationDecision(true);
    }

    if (!StringUtils.hasText(rule.menuPermission()) && !StringUtils.hasText(rule.actionPermission())) {
      return new AuthorizationDecision(true);
    }

    Set<String> permissions = effectivePermissionService.findEffectivePermissionKeys(currentAuthentication.getName());
    boolean hasMenuPermission = !StringUtils.hasText(rule.menuPermission()) || permissions.contains(rule.menuPermission());
    boolean hasActionPermission = !StringUtils.hasText(rule.actionPermission()) || permissions.contains(rule.actionPermission());

    return new AuthorizationDecision(hasMenuPermission && hasActionPermission);
  }
}
```

- [ ] **Step 6: Wire central manager into `SecurityConfig`**

Update method signature:

```java
public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                               JwtTokenService jwtTokenService,
                                               UserDetailsService userDetailsService,
                                               ObjectMapper objectMapper,
                                               EnvOpsApiAuthorizationManager envOpsApiAuthorizationManager) throws Exception {
```

Replace the hard-coded `requestMatchers(...).hasRole("SUPER_ADMIN")` block and fallback `/api/**` authenticated rule with:

```java
.requestMatchers("/api/**").access(envOpsApiAuthorizationManager)
```

Keep all public matchers above it unchanged.

- [ ] **Step 7: Run authorization tests and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacApiAuthorizationTest test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRule.java backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRegistry.java backend/envops-framework/src/main/java/com/img/envops/framework/security/EnvOpsApiAuthorizationManager.java backend/envops-framework/src/main/java/com/img/envops/framework/security/SecurityConfig.java backend/envops-boot/src/test/java/com/img/envops/RbacApiAuthorizationTest.java
git commit -m "feat: enforce rbac api permissions"
```

---

### Task 4: Add RBAC management backend APIs

**Files:**
- Create: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacApplicationService.java`
- Create: `backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/rbac/RbacController.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RbacMapper.java`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java`

- [ ] **Step 1: Add failing RBAC controller API tests**

Append to `RbacControllerTest`:

```java
@Autowired
private ObjectMapper objectMapper;

@Autowired
private org.springframework.test.web.servlet.MockMvc mockMvc;

@Test
void superAdminCanReadRolesAndPermissionTree() throws Exception {
  String token = login("envops-admin", "EnvOps@123");

  mockMvc.perform(get("/api/system/rbac/roles").header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data[0].roleKey").value("SUPER_ADMIN"))
      .andExpect(jsonPath("$.data[0].builtIn").value(true));

  mockMvc.perform(get("/api/system/rbac/permissions").header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data[?(@.moduleKey == 'system')]").exists());
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
```

Add login helper if missing:

```java
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
```

- [ ] **Step 2: Run failing RBAC controller tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacControllerTest test
```

Expected: FAIL because `/api/system/rbac/**` does not exist.

- [ ] **Step 3: Add mapper method for safe permission-key validation**

Add to `RbacMapper`:

```java
@Select({
    "<script>",
    "SELECT permission_key",
    "FROM sys_permission",
    "WHERE enabled = TRUE",
    "AND permission_key IN",
    "<foreach collection='permissionKeys' item='permissionKey' open='(' separator=',' close=')'>",
    "#{permissionKey}",
    "</foreach>",
    "</script>"
})
List<String> findEnabledPermissionKeysByKeys(@Param("permissionKeys") List<String> permissionKeys);
```

The service must use bound `#{permissionKey}` values through MyBatis `<foreach>`, not string-built SQL. The required behavior is to reject every missing or disabled key before inserting role-permission rows.

- [ ] **Step 4: Create `RbacApplicationService`**

Create service with these records and methods:

```java
package com.img.envops.modules.system.application.rbac;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.PermissionRow;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.RoleEntity;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.RoleRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RbacApplicationService {
  private final RbacMapper rbacMapper;

  public RbacApplicationService(RbacMapper rbacMapper) {
    this.rbacMapper = rbacMapper;
  }

  public List<RoleRecord> getRoles() {
    return rbacMapper.findRoles().stream().map(this::toRoleRecord).toList();
  }

  @Transactional
  public RoleRecord createRole(CreateRoleCommand command) {
    String roleKey = normalizeRoleKey(command == null ? null : command.roleKey());
    String roleName = normalizeRequiredText(command == null ? null : command.roleName(), "roleName");
    String description = normalizeOptionalText(command.description());
    boolean enabled = command.enabled() == null || command.enabled();

    if (rbacMapper.findRoleByKey(roleKey) != null) {
      throw new ConflictException("roleKey already exists: " + roleKey);
    }

    LocalDateTime now = LocalDateTime.now();
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(rbacMapper.nextRoleId());
    entity.setRoleKey(roleKey);
    entity.setRoleName(roleName);
    entity.setDescription(description);
    entity.setEnabled(enabled);
    entity.setBuiltIn(false);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    rbacMapper.insertRole(entity);
    return toRoleRecord(rbacMapper.findRoleById(entity.getRoleId()));
  }

  @Transactional
  public RoleRecord updateRole(Long roleId, UpdateRoleCommand command) {
    RoleRow existing = requireRole(roleId);
    String roleName = normalizeRequiredText(command == null ? null : command.roleName(), "roleName");
    String description = normalizeOptionalText(command.description());
    boolean enabled = command.enabled() == null || command.enabled();

    RoleEntity entity = new RoleEntity();
    entity.setRoleId(existing.getRoleId());
    entity.setRoleKey(existing.getRoleKey());
    entity.setRoleName(roleName);
    entity.setDescription(description);
    entity.setEnabled(enabled);
    entity.setBuiltIn(existing.getBuiltIn());
    entity.setCreatedAt(existing.getCreatedAt());
    entity.setUpdatedAt(LocalDateTime.now());
    rbacMapper.updateRole(entity);
    ensureSuperAdminStillExists();
    return toRoleRecord(rbacMapper.findRoleById(roleId));
  }

  public List<PermissionModule> getPermissionTree() {
    Map<String, PermissionModuleBuilder> modules = new LinkedHashMap<>();
    Map<String, PermissionNode> menus = new LinkedHashMap<>();

    for (PermissionRow permission : rbacMapper.findEnabledPermissions()) {
      PermissionModuleBuilder module = modules.computeIfAbsent(
          permission.getModuleKey(),
          key -> new PermissionModuleBuilder(key, buildModuleName(key), new ArrayList<>()));
      PermissionNode node = toPermissionNode(permission, new ArrayList<>());

      if ("menu".equals(permission.getPermissionType())) {
        menus.put(permission.getPermissionKey(), node);
        module.permissions().add(node);
      } else if (StringUtils.hasText(permission.getParentKey()) && menus.containsKey(permission.getParentKey())) {
        menus.get(permission.getParentKey()).children().add(node);
      } else {
        module.permissions().add(node);
      }
    }

    return modules.values().stream().map(PermissionModuleBuilder::build).toList();
  }

  public RolePermissions getRolePermissions(Long roleId) {
    RoleRow role = requireRole(roleId);
    return new RolePermissions(role.getRoleId(), role.getRoleKey(), rbacMapper.findRolePermissionKeys(roleId));
  }

  @Transactional
  public RolePermissions replaceRolePermissions(Long roleId, ReplaceRolePermissionsCommand command) {
    RoleRow role = requireRole(roleId);
    LinkedHashSet<String> permissionKeys = normalizePermissionKeys(command == null ? null : command.permissionKeys());
    validatePermissionKeys(permissionKeys);

    if ("SUPER_ADMIN".equals(role.getRoleKey())
        && (!permissionKeys.contains("system_rbac") || !permissionKeys.contains("system:role:manage"))) {
      throw new IllegalArgumentException("SUPER_ADMIN must keep system RBAC management permissions");
    }

    rbacMapper.deleteRolePermissions(roleId);
    permissionKeys.forEach(permissionKey -> rbacMapper.insertRolePermission(roleId, permissionKey));
    ensureSuperAdminStillExists();
    return getRolePermissions(roleId);
  }

  private RoleRow requireRole(Long roleId) {
    if (roleId == null) {
      throw new IllegalArgumentException("roleId is required");
    }
    RoleRow role = rbacMapper.findRoleById(roleId);
    if (role == null) {
      throw new NotFoundException("role not found: " + roleId);
    }
    return role;
  }

  private void validatePermissionKeys(LinkedHashSet<String> permissionKeys) {
    if (permissionKeys.isEmpty()) {
      return;
    }

    Set<String> enabledKeys = new LinkedHashSet<>(rbacMapper.findEnabledPermissionKeysByKeys(new ArrayList<>(permissionKeys)));
    for (String permissionKey : permissionKeys) {
      if (!enabledKeys.contains(permissionKey)) {
        throw new IllegalArgumentException("permissionKey is invalid: " + permissionKey);
      }
    }
  }

  private void ensureSuperAdminStillExists() {
    if (rbacMapper.countActiveSuperAdminUsers() <= 0) {
      throw new ConflictException("at least one active SUPER_ADMIN user is required");
    }
  }

  private String normalizeRoleKey(String roleKey) {
    String normalized = normalizeRequiredText(roleKey, "roleKey").trim().toUpperCase(Locale.ROOT).replace('-', '_');
    if (!normalized.matches("^[A-Z][A-Z0-9_]{2,63}$")) {
      throw new IllegalArgumentException("roleKey must be uppercase underscore style");
    }
    return normalized;
  }

  private LinkedHashSet<String> normalizePermissionKeys(List<String> permissionKeys) {
    if (permissionKeys == null) {
      throw new IllegalArgumentException("permissionKeys is required");
    }
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    permissionKeys.stream().filter(StringUtils::hasText).map(String::trim).forEach(normalized::add);
    return normalized;
  }

  private String normalizeRequiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  private String normalizeOptionalText(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String buildModuleName(String moduleKey) {
    return switch (moduleKey) {
      case "asset" -> "Asset";
      case "monitor" -> "Monitor";
      case "app" -> "App";
      case "deploy" -> "Deploy";
      case "task" -> "Task Center";
      case "traffic" -> "Traffic";
      case "system" -> "System";
      default -> moduleKey;
    };
  }

  private RoleRecord toRoleRecord(RoleRow row) {
    return new RoleRecord(
        row.getRoleId(),
        row.getRoleKey(),
        row.getRoleName(),
        row.getDescription(),
        Boolean.TRUE.equals(row.getEnabled()),
        Boolean.TRUE.equals(row.getBuiltIn()),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  private PermissionNode toPermissionNode(PermissionRow row, List<PermissionNode> children) {
    return new PermissionNode(
        row.getPermissionId(),
        row.getPermissionKey(),
        row.getPermissionName(),
        row.getPermissionType(),
        row.getModuleKey(),
        row.getParentKey(),
        row.getRouteName(),
        row.getActionKey(),
        row.getSortOrder(),
        Boolean.TRUE.equals(row.getEnabled()),
        children);
  }

  public record RoleRecord(Long id,
                           String roleKey,
                           String roleName,
                           String description,
                           Boolean enabled,
                           Boolean builtIn,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
  }

  public record CreateRoleCommand(String roleKey, String roleName, String description, Boolean enabled) {
  }

  public record UpdateRoleCommand(String roleName, String description, Boolean enabled) {
  }

  public record PermissionModule(String moduleKey, String moduleName, List<PermissionNode> permissions) {
  }

  public record PermissionNode(Long id,
                               String permissionKey,
                               String permissionName,
                               String permissionType,
                               String moduleKey,
                               String parentKey,
                               String routeName,
                               String actionKey,
                               Integer sortOrder,
                               Boolean enabled,
                               List<PermissionNode> children) {
  }

  public record RolePermissions(Long roleId, String roleKey, List<String> permissionKeys) {
  }

  public record ReplaceRolePermissionsCommand(List<String> permissionKeys) {
  }

  private record PermissionModuleBuilder(String moduleKey, String moduleName, List<PermissionNode> permissions) {
    private PermissionModule build() {
      return new PermissionModule(moduleKey, moduleName, permissions);
    }
  }
}
```

- [ ] **Step 5: Create `RbacController`**

```java
package com.img.envops.modules.system.interfaces.rbac;

import com.img.envops.common.response.R;
import com.img.envops.modules.system.application.rbac.RbacApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/rbac")
public class RbacController {
  private final RbacApplicationService rbacApplicationService;

  public RbacController(RbacApplicationService rbacApplicationService) {
    this.rbacApplicationService = rbacApplicationService;
  }

  @GetMapping("/roles")
  public R<List<RbacApplicationService.RoleRecord>> getRoles() {
    return R.ok(rbacApplicationService.getRoles());
  }

  @PostMapping("/roles")
  public R<RbacApplicationService.RoleRecord> createRole(@RequestBody(required = false) CreateRoleRequest request) {
    return R.ok(rbacApplicationService.createRole(new RbacApplicationService.CreateRoleCommand(
        request == null ? null : request.roleKey(),
        request == null ? null : request.roleName(),
        request == null ? null : request.description(),
        request == null ? null : request.enabled())));
  }

  @PutMapping("/roles/{id}")
  public R<RbacApplicationService.RoleRecord> updateRole(@PathVariable Long id,
                                                         @RequestBody(required = false) UpdateRoleRequest request) {
    return R.ok(rbacApplicationService.updateRole(id, new RbacApplicationService.UpdateRoleCommand(
        request == null ? null : request.roleName(),
        request == null ? null : request.description(),
        request == null ? null : request.enabled())));
  }

  @GetMapping("/permissions")
  public R<List<RbacApplicationService.PermissionModule>> getPermissions() {
    return R.ok(rbacApplicationService.getPermissionTree());
  }

  @GetMapping("/roles/{id}/permissions")
  public R<RbacApplicationService.RolePermissions> getRolePermissions(@PathVariable Long id) {
    return R.ok(rbacApplicationService.getRolePermissions(id));
  }

  @PutMapping("/roles/{id}/permissions")
  public R<RbacApplicationService.RolePermissions> replaceRolePermissions(@PathVariable Long id,
                                                                          @RequestBody(required = false) ReplaceRolePermissionsRequest request) {
    return R.ok(rbacApplicationService.replaceRolePermissions(id, new RbacApplicationService.ReplaceRolePermissionsCommand(
        request == null ? null : request.permissionKeys())));
  }

  public record CreateRoleRequest(String roleKey, String roleName, String description, Boolean enabled) {
  }

  public record UpdateRoleRequest(String roleName, String description, Boolean enabled) {
  }

  public record ReplaceRolePermissionsRequest(List<String> permissionKeys) {
  }
}
```

- [ ] **Step 6: Run RBAC controller tests and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacControllerTest test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-system/src/main/java/com/img/envops/modules/system/application/rbac/RbacApplicationService.java backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/rbac/RbacController.java backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RbacMapper.java backend/envops-boot/src/test/java/com/img/envops/RbacControllerTest.java
git commit -m "feat: add rbac management api"
```

---

### Task 5: Move route filtering and user-role assignment to RBAC

**Files:**
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RouteMenuMapper.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/route/RouteApplicationService.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/user/UserController.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/user/UserApplicationService.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/UserAuthMapper.java`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/UserControllerTest.java`

- [ ] **Step 1: Add failing route filtering and user-role tests**

In `AuthRouteControllerTest`, add:

```java
@Test
void getUserRoutesFiltersByMenuPermissions() throws Exception {
  seedPermissionOnlyUser(90L, "task-only-user", "TaskOnly@123", "TASK_ONLY", List.of("home", "task", "task_center", "task_tracking_[id]"));
  String token = login("task-only-user", "TaskOnly@123");

  mockMvc.perform(get("/api/routes/getUserRoutes").header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data[?(@.name == 'task')]").exists())
      .andExpect(jsonPath("$.data[?(@.name == 'system')]").doesNotExist())
      .andExpect(jsonPath("$.data[?(@.name == 'asset')]").doesNotExist());
}
```

Add helper:

```java
private void seedPermissionOnlyUser(Long userId, String userName, String password, String roleKey, List<String> permissionKeys) {
  long roleId = 9000L + userId;
  jdbcTemplate.update(
      "INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
      userId,
      userName,
      passwordEncoder.encode(password),
      "138" + String.format("%08d", userId % 100000000L),
      "qa",
      "PASSWORD",
      "ACTIVE");
  jdbcTemplate.update(
      "INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at) VALUES (?, ?, ?, ?, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      roleId,
      roleKey,
      roleKey,
      "Test route role");
  jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
  for (String permissionKey : permissionKeys) {
    jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission WHERE permission_key = ?", roleId, permissionKey);
  }
}
```

In `UserControllerTest`, add:

```java
@Test
void superAdminCanReadAndReplaceUserRoles() throws Exception {
  String accessToken = login("envops-admin", "EnvOps@123");

  mockMvc.perform(get("/api/system/users/21/roles").header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.userId").value(21))
      .andExpect(jsonPath("$.data.roleKeys", containsInAnyOrder("TRAFFIC_OWNER")));

  mockMvc.perform(put("/api/system/users/21/roles")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "roleIds": [5]
              }
              """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.roleKeys", containsInAnyOrder("OBSERVER")));
}

@Test
void replacingLastSuperAdminRoleIsRejected() throws Exception {
  String accessToken = login("envops-admin", "EnvOps@123");
  jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = 20 AND role_id = 1");

  mockMvc.perform(put("/api/system/users/1/roles")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "roleIds": [5]
              }
              """))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("409"));
}
```

- [ ] **Step 2: Run failing integration tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest#getUserRoutesFiltersByMenuPermissions,UserControllerTest#superAdminCanReadAndReplaceUserRoles,UserControllerTest#replacingLastSuperAdminRoleIsRejected test
```

Expected: FAIL because routes still use `required_role` and user-role endpoints do not exist.

- [ ] **Step 3: Update `RouteMenuMapper.findUserRoutesByUserId`**

Replace the current `required_role` query with:

```java
@Select("""
    SELECT DISTINCT m.id,
           m.parent_id AS parentId,
           m.route_name AS routeName,
           m.route_path AS routePath,
           m.component,
           m.title,
           m.icon,
           m.route_order AS routeOrder,
           m.home_flag AS homeFlag,
           m.hide_in_menu AS hideInMenu,
           m.active_menu AS activeMenu
    FROM sys_menu_route m
    JOIN sys_permission p ON p.route_name = m.route_name
    JOIN sys_role_permission rp ON rp.permission_id = p.id
    JOIN sys_role r ON r.id = rp.role_id
    JOIN sys_user_role ur ON ur.role_id = r.id
    WHERE m.route_type = 'USER'
      AND ur.user_id = #{userId}
      AND p.permission_type = 'menu'
      AND p.enabled = TRUE
      AND r.enabled = TRUE
    ORDER BY m.route_order, m.id
    """)
List<RouteRow> findUserRoutesByUserId(@Param("userId") Long userId);
```

Keep `findConstantRoutes` unchanged.

- [ ] **Step 4: Add user-role mapper methods**

Add to `UserAuthMapper`:

```java
@Select("""
    SELECT r.id AS roleId,
           r.role_key AS roleKey,
           r.role_name AS roleName,
           r.enabled AS enabled,
           r.built_in AS builtIn
    FROM sys_role r
    JOIN sys_user_role ur ON ur.role_id = r.id
    WHERE ur.user_id = #{userId}
    ORDER BY r.id
    """)
List<RoleRow> findRolesByUserId(@Param("userId") Long userId);

@Select("""
    SELECT id AS roleId,
           role_key AS roleKey,
           role_name AS roleName,
           enabled AS enabled,
           built_in AS builtIn
    FROM sys_role
    WHERE enabled = TRUE
    ORDER BY id
    """)
List<RoleRow> findEnabledRoles();
```

Extend `RoleRow` with `enabled` and `builtIn` getters/setters.

- [ ] **Step 5: Add user-role service methods**

In `UserApplicationService`, add records:

```java
public record UserRoleAssignment(Long userId,
                                 List<UserRoleRecord> roles,
                                 List<Long> roleIds,
                                 List<String> roleKeys) {
}

public record UserRoleRecord(Long id, String roleKey, String roleName, Boolean enabled, Boolean builtIn) {
}

public record ReplaceUserRolesCommand(List<Long> roleIds) {
}
```

Add methods:

```java
public UserRoleAssignment getUserRoles(Long userId) {
  requireUser(userId);
  List<UserRoleRecord> roles = userAuthMapper.findRolesByUserId(userId).stream().map(this::toUserRoleRecord).toList();
  return new UserRoleAssignment(
      userId,
      roles,
      roles.stream().map(UserRoleRecord::id).toList(),
      roles.stream().map(UserRoleRecord::roleKey).toList());
}

@Transactional
public UserRoleAssignment replaceUserRoles(Long userId, ReplaceUserRolesCommand command) {
  requireUser(userId);
  if (command == null || command.roleIds() == null || command.roleIds().isEmpty()) {
    throw new IllegalArgumentException("roleIds must not be empty");
  }

  Map<Long, RoleRow> enabledRoles = userAuthMapper.findEnabledRoles().stream()
      .collect(java.util.stream.Collectors.toMap(RoleRow::getRoleId, role -> role, (left, right) -> left, LinkedHashMap::new));

  List<RoleBinding> roles = command.roleIds().stream()
      .distinct()
      .map(roleId -> {
        RoleRow role = enabledRoles.get(roleId);
        if (role == null) {
          throw new IllegalArgumentException("roleId is invalid: " + roleId);
        }
        return new RoleBinding(role.getRoleId(), role.getRoleKey());
      })
      .toList();

  replaceUserRoles(userId, roles);
  ensureActiveSuperAdminExists();
  return getUserRoles(userId);
}

private UserRoleRecord toUserRoleRecord(RoleRow row) {
  return new UserRoleRecord(row.getRoleId(), row.getRoleKey(), row.getRoleName(), row.getEnabled(), row.getBuiltIn());
}

private void ensureActiveSuperAdminExists() {
  Integer count = userAuthMapper.countActiveEnabledSuperAdminUsers();
  if (count == null || count <= 0) {
    throw new ConflictException("at least one active SUPER_ADMIN user is required");
  }
}
```

Add mapper method:

```java
@Select("""
    SELECT COUNT(*)
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.id
    JOIN sys_role r ON r.id = ur.role_id
    WHERE u.status = 'ACTIVE'
      AND r.role_key = 'SUPER_ADMIN'
      AND r.enabled = TRUE
    """)
Integer countActiveEnabledSuperAdminUsers();
```

Call `ensureActiveSuperAdminExists()` after `updateUser` replaces roles and after status changes.

- [ ] **Step 6: Add user-role controller endpoints**

In `UserController`, add:

```java
@GetMapping("/{id}/roles")
public R<UserApplicationService.UserRoleAssignment> getUserRoles(@PathVariable Long id) {
  return R.ok(userApplicationService.getUserRoles(id));
}

@PutMapping("/{id}/roles")
public R<UserApplicationService.UserRoleAssignment> replaceUserRoles(@PathVariable Long id,
                                                                     @RequestBody(required = false) ReplaceSystemUserRolesRequest request) {
  return R.ok(userApplicationService.replaceUserRoles(id, new UserApplicationService.ReplaceUserRolesCommand(
      request == null ? null : request.roleIds())));
}

public record ReplaceSystemUserRolesRequest(List<Long> roleIds) {
}
```

- [ ] **Step 7: Run tests and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest,UserControllerTest test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RouteMenuMapper.java backend/envops-system/src/main/java/com/img/envops/modules/system/application/route/RouteApplicationService.java backend/envops-system/src/main/java/com/img/envops/modules/system/interfaces/user/UserController.java backend/envops-system/src/main/java/com/img/envops/modules/system/application/user/UserApplicationService.java backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/UserAuthMapper.java backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java backend/envops-boot/src/test/java/com/img/envops/UserControllerTest.java
git commit -m "feat: align routes and user roles with rbac"
```

---

### Task 6: Add backend registry coverage tests

**Files:**
- Create: `backend/envops-boot/src/test/java/com/img/envops/RbacRegistryCoverageTest.java`
- Modify: `backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRegistry.java`

- [ ] **Step 1: Write coverage test**

Create:

```java
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

import java.util.Map;
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
```

- [ ] **Step 2: Run coverage test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=RbacRegistryCoverageTest test
```

Expected: FAIL if any controller endpoint lacks a matching registry rule.

- [ ] **Step 3: Add missing rules**

For each uncovered endpoint, add a specific `ApiAuthorizationRule` in `ApiAuthorizationRegistry`. Follow this rule:

```java
ApiAuthorizationRule.read(HttpMethod.GET, "/api/example/**", PermissionKeys.Menu.SOME_MENU)
ApiAuthorizationRule.action(HttpMethod.POST, "/api/example/*/run", PermissionKeys.Menu.SOME_MENU, PermissionKeys.Action.SOME_ACTION)
```

Do not add broad catch-all module rules before action endpoints.

- [ ] **Step 4: Run backend security suite and commit**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest,UserControllerTest,RbacControllerTest,RbacApiAuthorizationTest,RbacRegistryCoverageTest test
```

Expected: PASS.

Commit:

```bash
git add backend/envops-boot/src/test/java/com/img/envops/RbacRegistryCoverageTest.java backend/envops-framework/src/main/java/com/img/envops/framework/security/ApiAuthorizationRegistry.java
git commit -m "test: cover rbac registry mappings"
```

---

### Task 7: Add frontend RBAC API contracts, route, and locales

**Files:**
- Create: `frontend/src/typings/api/system-rbac.d.ts`
- Create: `frontend/src/service/api/system-rbac.ts`
- Modify: `frontend/src/service/api/index.ts`
- Modify: `frontend/src/typings/api/system-user.d.ts`
- Modify: `frontend/src/service/api/system-user.ts`
- Modify: `frontend/src/router/elegant/routes.ts`
- Modify: `frontend/src/router/elegant/imports.ts`
- Modify: `frontend/src/router/elegant/transform.ts`
- Modify: `frontend/src/typings/elegant-router.d.ts`
- Modify: `frontend/src/locales/langs/zh-cn.ts`
- Modify: `frontend/src/locales/langs/en-us.ts`
- Modify: `frontend/src/typings/app.d.ts`
- Create: `frontend/src/views/system/rbac-contract.spec.ts`
- Modify: `frontend/src/views/system/user-contract.spec.ts`
- Modify: `frontend/src/store/modules/__tests__/route-envops.spec.ts`

- [ ] **Step 1: Write failing frontend contract tests**

Create `frontend/src/views/system/rbac-contract.spec.ts`:

```ts
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = process.cwd();
const apiSource = readFileSync(resolve(root, 'src/service/api/system-rbac.ts'), 'utf8');
const apiIndexSource = readFileSync(resolve(root, 'src/service/api/index.ts'), 'utf8');
const typingSource = readFileSync(resolve(root, 'src/typings/api/system-rbac.d.ts'), 'utf8');
const routesSource = readFileSync(resolve(root, 'src/router/elegant/routes.ts'), 'utf8');
const zhSource = readFileSync(resolve(root, 'src/locales/langs/zh-cn.ts'), 'utf8');
const enSource = readFileSync(resolve(root, 'src/locales/langs/en-us.ts'), 'utf8');

describe('system rbac frontend contract', () => {
  it('declares rbac api methods and endpoints', () => {
    expect(apiSource).toContain('fetchGetSystemRbacRoles');
    expect(apiSource).toContain('/api/system/rbac/roles');
    expect(apiSource).toContain('fetchGetSystemRbacPermissions');
    expect(apiSource).toContain('/api/system/rbac/permissions');
    expect(apiSource).toContain('fetchGetSystemRbacRolePermissions');
    expect(apiSource).toContain('fetchUpdateSystemRbacRolePermissions');
    expect(apiIndexSource).toContain("export * from './system-rbac'");
  });

  it('declares rbac typings', () => {
    expect(typingSource).toContain('namespace SystemRbac');
    expect(typingSource).toContain('interface RoleRecord');
    expect(typingSource).toContain('interface PermissionNode');
    expect(typingSource).toContain('interface UpdateRolePermissionsParams');
  });

  it('registers system rbac route and locales', () => {
    expect(routesSource).toContain("name: 'system_rbac'");
    expect(routesSource).toContain("path: '/system/rbac'");
    expect(routesSource).toContain("component: 'view.system_rbac'");
    expect(zhSource).toContain("system_rbac: '权限管理'");
    expect(enSource).toContain("system_rbac: 'Permission Management'");
    expect(zhSource).toContain('systemRbac');
    expect(enSource).toContain('systemRbac');
  });
});
```

Extend `frontend/src/views/system/user-contract.spec.ts` with source checks:

```ts
expect(systemUserApiSource).toContain('fetchGetSystemUserRoles');
expect(systemUserApiSource).toContain('fetchUpdateSystemUserRoles');
expect(systemUserTypingSource).toContain('UpdateSystemUserRolesParams');
expect(systemUserTypingSource).toContain('roleIds: number[]');
```

- [ ] **Step 2: Run failing frontend contract tests**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts
```

Expected: FAIL because RBAC files, route, and locales are not present.

- [ ] **Step 3: Add RBAC typings**

Create `frontend/src/typings/api/system-rbac.d.ts`:

```ts
declare namespace Api {
  namespace SystemRbac {
    type PermissionType = 'menu' | 'action';

    interface RoleRecord {
      id: number;
      roleKey: string;
      roleName: string;
      description: string | null;
      enabled: boolean;
      builtIn: boolean;
      createdAt: string | null;
      updatedAt: string | null;
      [key: string]: unknown;
    }

    interface CreateRoleParams {
      roleKey: string;
      roleName: string;
      description?: string | null;
      enabled: boolean;
    }

    interface UpdateRoleParams {
      roleName: string;
      description?: string | null;
      enabled: boolean;
    }

    interface PermissionNode {
      id: number;
      permissionKey: string;
      permissionName: string;
      permissionType: PermissionType;
      moduleKey: string;
      parentKey: string | null;
      routeName: string | null;
      actionKey: string | null;
      sortOrder: number;
      enabled: boolean;
      children?: PermissionNode[];
      [key: string]: unknown;
    }

    interface PermissionModule {
      moduleKey: string;
      moduleName: string;
      permissions: PermissionNode[];
      [key: string]: unknown;
    }

    interface RolePermissionsResponse {
      roleId: number;
      roleKey: string;
      permissionKeys: string[];
      [key: string]: unknown;
    }

    interface UpdateRolePermissionsParams {
      permissionKeys: string[];
    }

    type PermissionTreeResponse = PermissionModule[];
    type RoleListResponse = RoleRecord[];
  }
}
```

Modify `frontend/src/typings/api/system-user.d.ts`:

```ts
interface UserRoleRecord {
  id: number;
  roleKey: string;
  roleName: string;
  enabled: boolean;
  builtIn: boolean;
  [key: string]: unknown;
}

interface UserRoleAssignmentResponse {
  userId: number;
  roles: UserRoleRecord[];
  roleIds: number[];
  roleKeys: string[];
  [key: string]: unknown;
}

interface UpdateSystemUserRolesParams {
  roleIds: number[];
}
```

- [ ] **Step 4: Add RBAC services**

Create `frontend/src/service/api/system-rbac.ts`:

```ts
import { request } from '../request';

/** get RBAC roles */
export function fetchGetSystemRbacRoles() {
  return request<Api.SystemRbac.RoleListResponse>({ url: '/api/system/rbac/roles' });
}

/** create RBAC role */
export function fetchCreateSystemRbacRole(data: Api.SystemRbac.CreateRoleParams) {
  return request<Api.SystemRbac.RoleRecord>({
    url: '/api/system/rbac/roles',
    method: 'post',
    data
  });
}

/** update RBAC role */
export function fetchUpdateSystemRbacRole(id: number, data: Api.SystemRbac.UpdateRoleParams) {
  return request<Api.SystemRbac.RoleRecord>({
    url: `/api/system/rbac/roles/${id}`,
    method: 'put',
    data
  });
}

/** get fixed permission tree */
export function fetchGetSystemRbacPermissions() {
  return request<Api.SystemRbac.PermissionTreeResponse>({ url: '/api/system/rbac/permissions' });
}

/** get role permissions */
export function fetchGetSystemRbacRolePermissions(id: number) {
  return request<Api.SystemRbac.RolePermissionsResponse>({ url: `/api/system/rbac/roles/${id}/permissions` });
}

/** replace role permissions */
export function fetchUpdateSystemRbacRolePermissions(id: number, data: Api.SystemRbac.UpdateRolePermissionsParams) {
  return request<Api.SystemRbac.RolePermissionsResponse>({
    url: `/api/system/rbac/roles/${id}/permissions`,
    method: 'put',
    data
  });
}
```

Add to `frontend/src/service/api/index.ts`:

```ts
export * from './system-rbac';
```

Add to `frontend/src/service/api/system-user.ts`:

```ts
/** get system user roles */
export function fetchGetSystemUserRoles(id: number) {
  return request<Api.SystemUser.UserRoleAssignmentResponse>({ url: `/api/system/users/${id}/roles` });
}

/** replace system user roles */
export function fetchUpdateSystemUserRoles(id: number, data: Api.SystemUser.UpdateSystemUserRolesParams) {
  return request<Api.SystemUser.UserRoleAssignmentResponse>({
    url: `/api/system/users/${id}/roles`,
    method: 'put',
    data
  });
}
```

- [ ] **Step 5: Register `system_rbac` route artifacts**

Add a System child route in `frontend/src/router/elegant/routes.ts`:

```ts
{
  name: 'system_rbac',
  path: '/system/rbac',
  component: 'view.system_rbac',
  meta: {
    title: 'system_rbac',
    i18nKey: 'route.system_rbac'
  }
}
```

Add view import in `frontend/src/router/elegant/imports.ts`:

```ts
system_rbac: () => import("@/views/system/rbac/index.vue"),
```

Add route map in `frontend/src/router/elegant/transform.ts`:

```ts
"system_rbac": "/system/rbac",
```

Add to `frontend/src/typings/elegant-router.d.ts` route map and last-level union:

```ts
"system_rbac": "/system/rbac";
```

```ts
| "system_rbac"
```

- [ ] **Step 6: Add locale keys**

Add route locale:

```ts
system_rbac: '权限管理'
```

```ts
system_rbac: 'Permission Management'
```

Add page locale blocks under `page.envops`:

```ts
systemRbac: {
  hero: {
    title: '权限管理',
    description: '维护 EnvOps RBAC 的角色和固定菜单/操作权限。'
  },
  actions: {
    createRole: '创建角色',
    refresh: '刷新',
    saveRole: '保存角色',
    savePermissions: '保存权限'
  },
  roleList: {
    title: '角色列表',
    searchPlaceholder: '搜索角色名称或标识',
    builtIn: '内置',
    enabled: '启用',
    disabled: '禁用',
    empty: '暂无角色'
  },
  detail: {
    title: '角色详情',
    roleKey: '角色标识',
    roleName: '角色名称',
    description: '描述',
    enabled: '启用',
    builtInHint: '当前版本不支持删除内置角色。'
  },
  permissions: {
    title: '权限树',
    menu: '菜单',
    action: '操作',
    empty: '暂无权限',
    menuRequired: '请先选择菜单权限，再分配其操作权限。'
  },
  messages: {
    loadFailed: '加载权限数据失败',
    createSuccess: '角色已创建',
    updateSuccess: '角色已更新',
    permissionSaveSuccess: '角色权限已保存',
    missingRoleManagePermission: '缺少权限：system:role:manage'
  }
}
```

English equivalent:

```ts
systemRbac: {
  hero: {
    title: 'Permission Management',
    description: 'Manage roles and fixed menu/action permissions for EnvOps RBAC.'
  },
  actions: {
    createRole: 'Create Role',
    refresh: 'Refresh',
    saveRole: 'Save Role',
    savePermissions: 'Save Permissions'
  },
  roleList: {
    title: 'Roles',
    searchPlaceholder: 'Search role name or key',
    builtIn: 'Built-in',
    enabled: 'Enabled',
    disabled: 'Disabled',
    empty: 'No roles found'
  },
  detail: {
    title: 'Role Details',
    roleKey: 'Role Key',
    roleName: 'Role Name',
    description: 'Description',
    enabled: 'Enabled',
    builtInHint: 'Built-in roles cannot be deleted in this version.'
  },
  permissions: {
    title: 'Permission Tree',
    menu: 'Menu',
    action: 'Action',
    empty: 'No permissions available',
    menuRequired: 'Select the menu permission before assigning its actions.'
  },
  messages: {
    loadFailed: 'Failed to load RBAC data',
    createSuccess: 'Role created',
    updateSuccess: 'Role updated',
    permissionSaveSuccess: 'Role permissions saved',
    missingRoleManagePermission: 'Missing permission: system:role:manage'
  }
}
```

Add common permission locale:

```ts
permission: {
  missingAction: '缺少权限：{permission}'
}
```

```ts
permission: {
  missingAction: 'Missing permission: {permission}'
}
```

Update `frontend/src/typings/app.d.ts` schema to include these keys.

- [ ] **Step 7: Update route tests**

Add `system_rbac` to `frontend/src/store/modules/__tests__/route-envops.spec.ts` mocked System route children and expected visible menu children.

- [ ] **Step 8: Run frontend contracts and commit**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts
pnpm --dir frontend typecheck
```

Expected: PASS.

Commit:

```bash
git add frontend/src/typings/api/system-rbac.d.ts frontend/src/service/api/system-rbac.ts frontend/src/service/api/index.ts frontend/src/typings/api/system-user.d.ts frontend/src/service/api/system-user.ts frontend/src/router/elegant/routes.ts frontend/src/router/elegant/imports.ts frontend/src/router/elegant/transform.ts frontend/src/typings/elegant-router.d.ts frontend/src/locales/langs/zh-cn.ts frontend/src/locales/langs/en-us.ts frontend/src/typings/app.d.ts frontend/src/views/system/rbac-contract.spec.ts frontend/src/views/system/user-contract.spec.ts frontend/src/store/modules/__tests__/route-envops.spec.ts
git commit -m "feat: add rbac frontend contract"
```

---

### Task 8: Build role-first Permission Management page

**Files:**
- Create: `frontend/src/views/system/rbac/index.vue`
- Modify: `frontend/src/views/system/rbac-contract.spec.ts`

- [ ] **Step 1: Add failing page contract assertions**

Append to `rbac-contract.spec.ts`:

```ts
const pageSource = readFileSync(resolve(root, 'src/views/system/rbac/index.vue'), 'utf8');

it('implements role-first permission management behavior', () => {
  expect(pageSource).toContain("name: 'SystemRbacPage'");
  expect(pageSource).toContain('system:role:manage');
  expect(pageSource).toContain('fetchGetSystemRbacRoles');
  expect(pageSource).toContain('fetchGetSystemRbacPermissions');
  expect(pageSource).toContain('fetchUpdateSystemRbacRolePermissions');
  expect(pageSource).toContain('collectActionKeys');
  expect(pageSource).toContain('isActionDisabled');
  expect(pageSource).toContain('handleSavePermissions');
});
```

- [ ] **Step 2: Run failing page contract**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts
```

Expected: FAIL because the page does not exist or lacks required behavior.

- [ ] **Step 3: Create `index.vue` page skeleton**

Create `frontend/src/views/system/rbac/index.vue` with script:

```vue
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchCreateSystemRbacRole,
  fetchGetSystemRbacPermissions,
  fetchGetSystemRbacRolePermissions,
  fetchGetSystemRbacRoles,
  fetchUpdateSystemRbacRole,
  fetchUpdateSystemRbacRolePermissions
} from '@/service/api';
import { useAuth } from '@/hooks/business/auth';

defineOptions({
  name: 'SystemRbacPage'
});

type RoleFormModel = {
  roleKey: string;
  roleName: string;
  description: string;
  enabled: boolean;
};

const ROLE_MANAGE_PERMISSION = 'system:role:manage';

const { t } = useI18n();
const { hasAuth } = useAuth();

const roles = ref<Api.SystemRbac.RoleRecord[]>([]);
const permissionModules = ref<Api.SystemRbac.PermissionModule[]>([]);
const selectedRoleId = ref<number | null>(null);
const assignedPermissionKeys = ref<string[]>([]);
const keyword = ref('');
const loading = ref(false);
const savingRole = ref(false);
const savingPermissions = ref(false);
const roleForm = reactive<RoleFormModel>(createDefaultRoleForm());

const canManageRole = computed(() => hasAuth(ROLE_MANAGE_PERMISSION));

const filteredRoles = computed(() => {
  const value = keyword.value.trim().toLowerCase();

  if (!value) {
    return roles.value;
  }

  return roles.value.filter(role => {
    return role.roleKey.toLowerCase().includes(value) || role.roleName.toLowerCase().includes(value);
  });
});

const selectedRole = computed(() => roles.value.find(role => role.id === selectedRoleId.value) || null);

function createDefaultRoleForm(): RoleFormModel {
  return {
    roleKey: '',
    roleName: '',
    description: '',
    enabled: true
  };
}

function resetRoleForm() {
  Object.assign(roleForm, createDefaultRoleForm());
}

function fillRoleForm(role: Api.SystemRbac.RoleRecord) {
  Object.assign(roleForm, {
    roleKey: role.roleKey,
    roleName: role.roleName,
    description: role.description || '',
    enabled: role.enabled
  } satisfies RoleFormModel);
}

function collectActionKeys(permission: Api.SystemRbac.PermissionNode) {
  return (permission.children || [])
    .filter(child => child.permissionType === 'action')
    .map(child => child.permissionKey);
}

function isPermissionChecked(permissionKey: string) {
  return assignedPermissionKeys.value.includes(permissionKey);
}

function setPermissionChecked(permission: Api.SystemRbac.PermissionNode, checked: boolean) {
  const next = new Set(assignedPermissionKeys.value);

  if (checked) {
    next.add(permission.permissionKey);
  } else {
    next.delete(permission.permissionKey);

    if (permission.permissionType === 'menu') {
      collectActionKeys(permission).forEach(actionKey => next.delete(actionKey));
    }
  }

  assignedPermissionKeys.value = Array.from(next);
}

function isActionDisabled(permission: Api.SystemRbac.PermissionNode) {
  if (permission.permissionType !== 'action' || !permission.parentKey) {
    return false;
  }

  return !assignedPermissionKeys.value.includes(permission.parentKey);
}

async function loadRoles() {
  const response = await fetchGetSystemRbacRoles();

  if (!response.error) {
    roles.value = response.data;

    if (selectedRoleId.value === null && response.data.length > 0) {
      await selectRole(response.data[0]);
    }
  }
}

async function loadPermissions() {
  const response = await fetchGetSystemRbacPermissions();

  if (!response.error) {
    permissionModules.value = response.data;
  }
}

async function selectRole(role: Api.SystemRbac.RoleRecord) {
  selectedRoleId.value = role.id;
  fillRoleForm(role);

  const response = await fetchGetSystemRbacRolePermissions(role.id);

  if (!response.error) {
    assignedPermissionKeys.value = response.data.permissionKeys;
  }
}

function handleCreateRole() {
  selectedRoleId.value = null;
  assignedPermissionKeys.value = [];
  resetRoleForm();
}

async function handleSaveRole() {
  if (!canManageRole.value || !roleForm.roleName.trim()) {
    return;
  }

  savingRole.value = true;

  try {
    const response =
      selectedRoleId.value === null
        ? await fetchCreateSystemRbacRole({
            roleKey: roleForm.roleKey.trim(),
            roleName: roleForm.roleName.trim(),
            description: roleForm.description.trim() || null,
            enabled: roleForm.enabled
          })
        : await fetchUpdateSystemRbacRole(selectedRoleId.value, {
            roleName: roleForm.roleName.trim(),
            description: roleForm.description.trim() || null,
            enabled: roleForm.enabled
          });

    if (!response.error) {
      window.$message?.success(
        t(selectedRoleId.value === null ? 'page.envops.systemRbac.messages.createSuccess' : 'page.envops.systemRbac.messages.updateSuccess')
      );
      await loadRoles();
      await selectRole(response.data);
    }
  } finally {
    savingRole.value = false;
  }
}

async function handleSavePermissions() {
  if (!canManageRole.value || selectedRoleId.value === null) {
    return;
  }

  savingPermissions.value = true;

  try {
    const response = await fetchUpdateSystemRbacRolePermissions(selectedRoleId.value, {
      permissionKeys: assignedPermissionKeys.value
    });

    if (!response.error) {
      assignedPermissionKeys.value = response.data.permissionKeys;
      window.$message?.success(t('page.envops.systemRbac.messages.permissionSaveSuccess'));
    }
  } finally {
    savingPermissions.value = false;
  }
}

async function loadPageData() {
  loading.value = true;

  try {
    await Promise.all([loadPermissions(), loadRoles()]);
  } finally {
    loading.value = false;
  }
}

onMounted(loadPageData);
</script>
```

- [ ] **Step 4: Add template**

Add template below the script:

```vue
<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false">
      <NSpace justify="space-between" align="center">
        <div>
          <h2 class="m-0 text-20px font-semibold">{{ t('page.envops.systemRbac.hero.title') }}</h2>
          <p class="m-t-8px text-#666">{{ t('page.envops.systemRbac.hero.description') }}</p>
        </div>
        <NSpace>
          <NButton @click="loadPageData">{{ t('page.envops.systemRbac.actions.refresh') }}</NButton>
          <NButton type="primary" :disabled="!canManageRole" @click="handleCreateRole">
            {{ t('page.envops.systemRbac.actions.createRole') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NSpin :show="loading">
      <div class="grid grid-cols-[320px_1fr] gap-16px">
        <NCard :title="t('page.envops.systemRbac.roleList.title')" :bordered="false">
          <NInput v-model:value="keyword" clearable :placeholder="t('page.envops.systemRbac.roleList.searchPlaceholder')" />
          <NEmpty v-if="filteredRoles.length === 0" class="m-t-16px" :description="t('page.envops.systemRbac.roleList.empty')" />
          <NSpace v-else class="m-t-16px" vertical>
            <NButton
              v-for="role in filteredRoles"
              :key="role.id"
              block
              :type="role.id === selectedRoleId ? 'primary' : 'default'"
              @click="selectRole(role)"
            >
              <div class="w-full flex justify-between">
                <span>{{ role.roleName }}</span>
                <NTag size="small" :type="role.enabled ? 'success' : 'default'">
                  {{ role.enabled ? t('page.envops.systemRbac.roleList.enabled') : t('page.envops.systemRbac.roleList.disabled') }}
                </NTag>
              </div>
            </NButton>
          </NSpace>
        </NCard>

        <NSpace vertical :size="16">
          <NCard :title="t('page.envops.systemRbac.detail.title')" :bordered="false">
            <NForm label-placement="top">
              <NGrid :cols="2" :x-gap="16">
                <NFormItem :label="t('page.envops.systemRbac.detail.roleKey')">
                  <NInput v-model:value="roleForm.roleKey" :disabled="selectedRoleId !== null || !canManageRole" />
                </NFormItem>
                <NFormItem :label="t('page.envops.systemRbac.detail.roleName')">
                  <NInput v-model:value="roleForm.roleName" :disabled="!canManageRole" />
                </NFormItem>
              </NGrid>
              <NFormItem :label="t('page.envops.systemRbac.detail.description')">
                <NInput v-model:value="roleForm.description" type="textarea" :disabled="!canManageRole" />
              </NFormItem>
              <NFormItem :label="t('page.envops.systemRbac.detail.enabled')">
                <NSwitch v-model:value="roleForm.enabled" :disabled="!canManageRole" />
              </NFormItem>
              <NAlert v-if="selectedRole?.builtIn" type="info" :show-icon="false">
                {{ t('page.envops.systemRbac.detail.builtInHint') }}
              </NAlert>
              <div class="m-t-16px flex justify-end">
                <NButton type="primary" :loading="savingRole" :disabled="!canManageRole" @click="handleSaveRole">
                  {{ t('page.envops.systemRbac.actions.saveRole') }}
                </NButton>
              </div>
            </NForm>
          </NCard>

          <NCard :title="t('page.envops.systemRbac.permissions.title')" :bordered="false">
            <NSpace vertical :size="16">
              <NCard v-for="module in permissionModules" :key="module.moduleKey" size="small" :title="module.moduleName">
                <NSpace vertical>
                  <div v-for="permission in module.permissions" :key="permission.permissionKey">
                    <NCheckbox
                      :checked="isPermissionChecked(permission.permissionKey)"
                      :disabled="!canManageRole"
                      @update:checked="checked => setPermissionChecked(permission, checked)"
                    >
                      {{ permission.permissionName }}
                    </NCheckbox>
                    <div v-if="permission.children?.length" class="m-l-24px m-t-8px flex flex-wrap gap-12px">
                      <NCheckbox
                        v-for="child in permission.children"
                        :key="child.permissionKey"
                        :checked="isPermissionChecked(child.permissionKey)"
                        :disabled="!canManageRole || isActionDisabled(child)"
                        @update:checked="checked => setPermissionChecked(child, checked)"
                      >
                        {{ child.permissionName }}
                      </NCheckbox>
                    </div>
                  </div>
                </NSpace>
              </NCard>
              <div class="flex justify-end">
                <NButton
                  type="primary"
                  :loading="savingPermissions"
                  :disabled="!canManageRole || selectedRoleId === null"
                  @click="handleSavePermissions"
                >
                  {{ t('page.envops.systemRbac.actions.savePermissions') }}
                </NButton>
              </div>
            </NSpace>
          </NCard>
        </NSpace>
      </div>
    </NSpin>
  </NSpace>
</template>
```

- [ ] **Step 5: Run page contract and commit**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts
pnpm --dir frontend typecheck
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/system/rbac/index.vue frontend/src/views/system/rbac-contract.spec.ts
git commit -m "feat: add rbac management page"
```

---

### Task 9: Add user-role assignment UI

**Files:**
- Modify: `frontend/src/views/system/user/index.vue`
- Modify: `frontend/src/views/system/user-contract.spec.ts`

- [ ] **Step 1: Add failing user-role UI contract assertions**

Append to `system-user-contract.spec.ts`:

```ts
expect(systemUserPage).toContain('fetchGetSystemRbacRoles');
expect(systemUserPage).toContain('fetchGetSystemUserRoles');
expect(systemUserPage).toContain('fetchUpdateSystemUserRoles');
expect(systemUserPage).toContain('system:user:manage');
expect(systemUserPage).toContain('handleOpenRoleAssignment');
expect(systemUserPage).toContain('handleSaveUserRoles');
```

- [ ] **Step 2: Run failing user contract**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/user-contract.spec.ts
```

Expected: FAIL because the user page still uses hard-coded role options and has no role assignment drawer.

- [ ] **Step 3: Import RBAC services and auth helper**

Modify imports in `system/user/index.vue`:

```ts
import {
  fetchCreateSystemUser,
  fetchGetSystemRbacRoles,
  fetchGetSystemUserRoles,
  fetchGetSystemUsers,
  fetchUpdateSystemUser,
  fetchUpdateSystemUserRoles
} from '@/service/api';
import { useAuth } from '@/hooks/business/auth';
```

Add after `const { t } = useI18n();`:

```ts
const USER_MANAGE_PERMISSION = 'system:user:manage';
const { hasAuth } = useAuth();
const canManageUser = computed(() => hasAuth(USER_MANAGE_PERMISSION));
```

- [ ] **Step 4: Replace hard-coded role options with backend roles**

Add state:

```ts
const assignableRoles = ref<Api.SystemRbac.RoleRecord[]>([]);
const roleDrawerVisible = ref(false);
const assigningUserId = ref<number | null>(null);
const selectedRoleIds = ref<number[]>([]);
const loadingUserRoles = ref(false);
const savingUserRoles = ref(false);
```

Replace `roleOptions` computed with:

```ts
const roleOptions = computed(() =>
  assignableRoles.value
    .filter(role => role.enabled)
    .map(role => ({
      label: `${role.roleName} (${role.roleKey})`,
      value: role.id
    }))
);
```

Add helper:

```ts
function getRoleKeysByIds(roleIds: number[]) {
  const roleIdSet = new Set(roleIds);
  return assignableRoles.value.filter(role => roleIdSet.has(role.id)).map(role => role.roleKey);
}
```

If existing create/update form still submits `roles: string[]`, use:

```ts
roles: getRoleKeysByIds(formModel.roles.map(Number))
```

If `formModel.roles` stays as strings for compatibility, keep old create/update payloads and use only the new role assignment drawer for role IDs. Do not remove existing create/update behavior unless backend command records are updated in the same task.

- [ ] **Step 5: Add role loading and assignment handlers**

```ts
async function loadAssignableRoles() {
  const response = await fetchGetSystemRbacRoles();

  if (!response.error) {
    assignableRoles.value = response.data.filter(role => role.enabled);
  }
}

async function handleOpenRoleAssignment(record: Api.SystemUser.SystemUserRecord) {
  if (!canManageUser.value) {
    return;
  }

  assigningUserId.value = record.id;
  roleDrawerVisible.value = true;
  loadingUserRoles.value = true;

  try {
    const response = await fetchGetSystemUserRoles(record.id);

    if (!response.error) {
      selectedRoleIds.value = response.data.roleIds;
    }
  } finally {
    loadingUserRoles.value = false;
  }
}

async function handleSaveUserRoles() {
  if (!canManageUser.value || assigningUserId.value === null) {
    return;
  }

  savingUserRoles.value = true;

  try {
    const response = await fetchUpdateSystemUserRoles(assigningUserId.value, {
      roleIds: selectedRoleIds.value
    });

    if (!response.error) {
      window.$message?.success(t('page.envops.systemUser.roleAssignment.saveSuccess'));
      roleDrawerVisible.value = false;
      assigningUserId.value = null;
      selectedRoleIds.value = [];
      await loadSystemUsers();
    }
  } finally {
    savingUserRoles.value = false;
  }
}
```

Update `onMounted`:

```ts
onMounted(() => {
  loadSystemUsers();
  loadAssignableRoles();
});
```

- [ ] **Step 6: Add role assignment action and drawer**

Add a row action button near edit button:

```vue
<NButton text type="primary" :disabled="!canManageUser" @click="handleOpenRoleAssignment(item)">
  {{ t('page.envops.systemUser.roleAssignment.title') }}
</NButton>
```

Add drawer near existing drawer:

```vue
<NDrawer v-model:show="roleDrawerVisible" :width="420">
  <NDrawerContent :title="t('page.envops.systemUser.roleAssignment.title')">
    <NSpin :show="loadingUserRoles">
      <NForm label-placement="top">
        <NFormItem :label="t('page.envops.systemUser.roleAssignment.roles')">
          <NSelect
            v-model:value="selectedRoleIds"
            multiple
            :options="roleOptions"
            :disabled="!canManageUser"
            :placeholder="t('page.envops.systemUser.roleAssignment.placeholder')"
          />
        </NFormItem>
      </NForm>
    </NSpin>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="roleDrawerVisible = false">{{ t('common.cancel') }}</NButton>
        <NButton type="primary" :loading="savingUserRoles" :disabled="!canManageUser" @click="handleSaveUserRoles">
          {{ t('page.envops.systemUser.roleAssignment.save') }}
        </NButton>
      </NSpace>
    </template>
  </NDrawerContent>
</NDrawer>
```

- [ ] **Step 7: Run user contract and commit**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/system/user-contract.spec.ts
pnpm --dir frontend typecheck
```

Expected: PASS.

Commit:

```bash
git add frontend/src/views/system/user/index.vue frontend/src/views/system/user-contract.spec.ts
git commit -m "feat: add user role assignment ui"
```

---

### Task 10: Add frontend action permission gating

**Files:**
- Modify: `frontend/src/hooks/business/auth.ts`
- Modify high-risk action pages listed in File Structure
- Create: `frontend/src/views/asset/asset-rbac-contract.spec.ts`
- Create: `frontend/src/views/app/app-rbac-contract.spec.ts`
- Create: `frontend/src/views/deploy/deploy-rbac-contract.spec.ts`
- Create: `frontend/src/views/monitor/monitor-rbac-contract.spec.ts`
- Modify: `frontend/src/views/traffic/traffic-contract.spec.ts`
- Modify: `frontend/src/views/asset/database-contract.spec.ts`

- [ ] **Step 1: Write failing action permission contract tests**

Create `frontend/src/views/deploy/deploy-rbac-contract.spec.ts`:

```ts
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const page = readFileSync(resolve(process.cwd(), 'src/views/deploy/task/index.vue'), 'utf8');

describe('deploy rbac contract', () => {
  it('uses action permissions for high-risk deploy actions', () => {
    expect(page).toContain('useAuth');
    expect(page).toContain('deploy:task:create');
    expect(page).toContain('deploy:task:approve');
    expect(page).toContain('deploy:task:execute');
    expect(page).toContain('deploy:task:cancel');
    expect(page).toContain('deploy:task:retry');
    expect(page).toContain('deploy:task:rollback');
  });
});
```

Create similar source-level tests for Asset, Monitor, App, and Traffic:

```ts
expect(page).toContain('asset:database:manage');
expect(page).toContain('asset:database:connectivity-check');
expect(page).toContain('monitor:detect-task:execute');
expect(page).toContain('app:package:manage');
expect(page).toContain('traffic:policy:apply');
```

- [ ] **Step 2: Run failing action contract tests**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/asset/asset-rbac-contract.spec.ts src/views/asset/database-contract.spec.ts src/views/app/app-rbac-contract.spec.ts src/views/deploy/deploy-rbac-contract.spec.ts src/views/monitor/monitor-rbac-contract.spec.ts src/views/traffic/traffic-contract.spec.ts
```

Expected: FAIL because pages do not yet use all action permission codes.

- [ ] **Step 3: Add `hasEveryAuth` without changing `hasAuth`**

Modify `frontend/src/hooks/business/auth.ts`:

```ts
function hasEveryAuth(codes: string[]) {
  if (!authStore.isLogin) {
    return false;
  }

  return codes.every(code => authStore.userInfo.buttons.includes(code));
}

return {
  hasAuth,
  hasEveryAuth
};
```

- [ ] **Step 4: Gate Asset actions**

Use this pattern in Asset pages:

```ts
import { useAuth } from '@/hooks/business/auth';

const { hasAuth } = useAuth();
const canManageDatabase = computed(() => hasAuth('asset:database:manage'));
const canCheckDatabaseConnectivity = computed(() => hasAuth('asset:database:connectivity-check'));
```

Guard handlers:

```ts
function handleOpenCreateDrawer() {
  if (!canManageDatabase.value) {
    return;
  }

  // keep existing create behavior
}
```

Disable buttons:

```vue
<NButton type="primary" :disabled="!canManageDatabase" @click="handleOpenCreateDrawer">
  {{ t('page.envops.assetDatabase.actions.create') }}
</NButton>
```

Apply equivalent constants:

- `asset:host:manage` in `asset/host/index.vue`
- `asset:credential:manage` in `asset/credential/index.vue`
- `asset:database:manage` and `asset:database:connectivity-check` in `asset/database/index.vue`

- [ ] **Step 5: Gate Monitor, App, Deploy, Traffic actions**

Add computed permissions to each page and combine with existing status checks.

Deploy example:

```ts
const canCreateDeployTask = computed(() => hasAuth('deploy:task:create'));
const canApproveDeployTask = computed(() => hasAuth('deploy:task:approve'));
const canExecuteDeployTask = computed(() => hasAuth('deploy:task:execute'));
const canCancelDeployTask = computed(() => hasAuth('deploy:task:cancel'));
const canRetryDeployTask = computed(() => hasAuth('deploy:task:retry'));
const canRollbackDeployTask = computed(() => hasAuth('deploy:task:rollback'));
```

Traffic example:

```ts
const canPreviewTrafficPolicy = computed(() => hasAuth('traffic:policy:preview'));
const canApplyTrafficPolicy = computed(() => hasAuth('traffic:policy:apply'));
const canRollbackTrafficPolicy = computed(() => hasAuth('traffic:policy:rollback'));
```

Template rule:

```vue
<NButton :disabled="!item.canApply || !canApplyTrafficPolicy" @click="handleApply(item)">
  {{ t('page.envops.traffic.actions.apply') }}
</NButton>
```

- [ ] **Step 6: Run frontend action tests and commit**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/asset/asset-rbac-contract.spec.ts src/views/asset/database-contract.spec.ts src/views/app/app-rbac-contract.spec.ts src/views/deploy/deploy-rbac-contract.spec.ts src/views/monitor/monitor-rbac-contract.spec.ts src/views/traffic/traffic-contract.spec.ts
pnpm --dir frontend typecheck
```

Expected: PASS.

Commit:

```bash
git add frontend/src/hooks/business/auth.ts frontend/src/views/asset frontend/src/views/app frontend/src/views/deploy frontend/src/views/monitor frontend/src/views/traffic
git commit -m "feat: gate frontend actions by rbac permissions"
```

---

### Task 11: Update documentation and release notes

**Files:**
- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Create: `release/0.0.8-release-notes.md`

- [ ] **Step 1: Write release notes**

Create `release/0.0.8-release-notes.md`:

```md
# EnvOps 0.0.8 Release Notes

## Summary

- EnvOps 新增 RBAC 权限管理第一版。
- 权限点由系统固定种子数据提供，不允许在 UI 中创建任意 API matcher。
- 系统管理新增权限管理页，可维护角色、角色启停和角色菜单/操作权限。
- 用户管理新增用户角色分配。
- 后端 API 授权与动态菜单权限统一到菜单 + 操作权限模型。

## Included scope

- 固定菜单权限和操作权限。
- 角色优先的权限管理 UI。
- 用户管理中的用户-角色绑定。
- Home、Asset、Monitor、App、Deploy、Task Center、Traffic、System 全模块覆盖。
- 菜单权限控制菜单可见和模块读 API。
- 操作权限控制创建、编辑、删除、执行、审批、应用、回滚、系统管理等高风险动作。
- 前端按钮禁用和提示作为用户体验，后端授权仍是安全边界。

## Not included

- 不提供 UI 创建任意 API matcher。
- 不建设组织架构、部门继承、审批流或审计中心。
- 不提供资源级归属权限。
- 不替换 JWT 登录模型。
- 不扩展 Task Center 统一重试、取消或编排。

## Validation

- `mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest,UserControllerTest,RbacControllerTest,RbacApiAuthorizationTest,RbacRegistryCoverageTest test`
- `mvn -f backend/pom.xml test`
- `pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
```

- [ ] **Step 2: Update README and docs**

Add this wording to README current baseline:

```md
- RBAC 权限管理提供固定权限点、角色维护、角色权限分配和用户角色分配；后端 API 授权与动态菜单权限统一使用菜单 + 操作权限模型。
```

Add this boundary wording to README and docs:

```md
RBAC 不在 v0.0.8 提供 UI 创建任意 API matcher、组织架构、部门继承、审批流、审计中心、资源级归属权限或 JWT 登录模型替换。
```

Add user manual steps:

```md
1. 进入系统管理 / 权限管理。
2. 在左侧选择或创建角色。
3. 在右侧维护角色基础信息。
4. 在权限树中勾选菜单权限和操作权限。
5. 保存角色权限。
6. 进入系统管理 / 用户管理。
7. 为用户分配已启用角色。
```

Add developer guide API list:

```md
- `GET /api/system/rbac/roles`
- `POST /api/system/rbac/roles`
- `PUT /api/system/rbac/roles/{id}`
- `GET /api/system/rbac/permissions`
- `GET /api/system/rbac/roles/{id}/permissions`
- `PUT /api/system/rbac/roles/{id}/permissions`
- `GET /api/system/users/{id}/roles`
- `PUT /api/system/users/{id}/roles`
```

- [ ] **Step 3: Verify docs mention scope and exclusions**

Run:

```bash
grep -R "任意 API matcher\|组织架构\|审批流\|审计中心\|资源级归属权限\|JWT 登录模型替换" -n README.md docs release/0.0.8-release-notes.md
```

Expected: output includes README, project docs, developer docs, user manual, and release notes.

- [ ] **Step 4: Commit docs**

```bash
git add README.md docs/envops-项目详细说明.md docs/envops-开发技术说明.md docs/envops-用户操作手册.md release/0.0.8-release-notes.md
git commit -m "docs: add v0.0.8 rbac release notes"
```

---

### Task 12: Final validation and browser smoke check

**Files:**
- All v0.0.8 backend, frontend, docs files.

- [ ] **Step 1: Run backend targeted validation**

```bash
mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest,UserControllerTest,RbacControllerTest,RbacApiAuthorizationTest,RbacRegistryCoverageTest test
```

Expected: PASS with all targeted RBAC tests green.

- [ ] **Step 2: Run backend full validation**

```bash
mvn -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 3: Run frontend targeted validation**

```bash
pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts src/views/asset/asset-rbac-contract.spec.ts src/views/asset/database-contract.spec.ts src/views/app/app-rbac-contract.spec.ts src/views/deploy/deploy-rbac-contract.spec.ts src/views/monitor/monitor-rbac-contract.spec.ts src/views/traffic/traffic-contract.spec.ts
```

Expected: PASS.

- [ ] **Step 4: Run frontend full validation**

```bash
pnpm --dir frontend test:unit
pnpm --dir frontend typecheck
pnpm --dir frontend build
```

Expected: PASS.

- [ ] **Step 5: Start backend for smoke testing**

```bash
ENVOPS_SECURITY_TOKEN_SECRET=test-only-envops-token-secret-12345 ENVOPS_CREDENTIAL_PROTECTION_SECRET=test-only-envops-credential-protection-secret-12345 ENVOPS_SERVER_PORT=18080 bash backend/scripts/run-envops-boot.sh
```

Expected: backend starts on `http://127.0.0.1:18080`.

- [ ] **Step 6: Start frontend for browser smoke testing**

```bash
pnpm --dir frontend dev
```

Expected: frontend starts on `http://localhost:9527`.

- [ ] **Step 7: Browser smoke test**

Use the app in a browser:

1. Login as `envops-admin / EnvOps@123`.
2. Open System / Permission Management.
3. Confirm roles are listed.
4. Create a role named `Smoke Viewer` with key `SMOKE_VIEWER`.
5. Assign `home`, `task`, `task_center`, and `task_tracking_[id]` permissions.
6. Save permissions.
7. Open System / User Management.
8. Assign `SMOKE_VIEWER` to a non-admin test user.
9. Login as that user.
10. Confirm only permitted menu entries appear.
11. Confirm high-risk action buttons are disabled or absent when permission is missing.

Expected: smoke path works. If browser automation is unavailable, state explicitly that browser UI smoke was not run and provide the backend/frontend validation evidence instead.

- [ ] **Step 8: Final status check and commit any validation-only fixes**

```bash
git status --short
```

Expected: no uncommitted changes after commits, unless validation generated ignored artifacts.

---

## Plan Self-Review

### Spec coverage

- Fixed permission points: Task 1 and Task 2.
- Role-first UI: Task 8.
- Central authorization registry: Task 3 and Task 6.
- Menu + action permission semantics: Task 1, Task 3, Task 10.
- User-role binding in User Management: Task 5 and Task 9.
- Full module coverage: Task 3, Task 6, Task 10.
- No arbitrary API matcher UI: Task 3 registry is code-defined; Task 8 UI only consumes permission tree.
- No organization hierarchy, approval flow, audit center, resource ownership, or JWT replacement: Scope Guardrails and docs task state exclusions.
- Backend authoritative security: Task 3 and Task 6.
- Frontend button permissions as UX only: Task 10 and docs task.
- Docs and release notes: Task 11.
- Validation and browser smoke: Task 12.

### Placeholder scan

No placeholder tokens or unspecified implementation slots remain. Every task lists exact files, exact commands, expected result, and concrete code or payload examples.

### Type and naming consistency

- Backend permission constants match SQL permission keys.
- Frontend action permission strings match backend `PermissionKeys.Action` values.
- Route name `system_rbac` is used consistently in SQL, generated route artifacts, locale keys, and frontend route tests.
- User-role payload uses `roleIds: number[]` consistently across backend controller records, frontend typings, frontend service, and user page.
- Role permission payload uses `permissionKeys: string[]` consistently across backend and frontend.
