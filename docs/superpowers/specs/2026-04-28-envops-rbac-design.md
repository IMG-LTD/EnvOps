# EnvOps v0.0.8 RBAC Design

## Context

EnvOps v0.0.7 completed the unified Task Center tracking page and made Task Center a cross-module tracking surface. The current platform now exposes meaningful operations across Asset, Monitor, App, Deploy, Task Center, Traffic, and System modules.

The current security model is uneven. Dynamic menu routes are filtered by role metadata, but most backend module APIs only require authentication. App and System user APIs have hard-coded `SUPER_ADMIN` checks, while Asset, Monitor, Deploy, Task Center, and Traffic APIs can be called by any authenticated user. v0.0.8 closes that gap by building the first complete RBAC management surface.

## Goal

Build EnvOps v0.0.8 RBAC as a fixed-permission, role-first authorization system that aligns backend API authorization, dynamic menu visibility, frontend action controls, and user-role management.

## Non-goals

- Do not let the UI create arbitrary API matchers.
- Do not build organization hierarchy, department inheritance, approval flow, or audit center.
- Do not introduce per-resource ownership rules such as “only owner can operate this Deploy task”.
- Do not replace JWT login or the existing user identity model.
- Do not make frontend button hiding a security boundary. Backend authorization remains authoritative.

## Approved scope decisions

- Permission points are fixed by system seed data.
- Roles are maintainable in UI.
- Role permissions are maintainable in UI.
- Permission granularity is menu plus action.
- The permission management UI is role-first.
- User-role binding is maintained from User Management.
- v0.0.8 covers all current modules: Home, Asset, Monitor, App, Deploy, Task Center, Traffic, and System.
- Menu permission is the prerequisite for module API access.
- Action permission controls write, execution, approval, rollback, delete, and other sensitive actions under an allowed menu.
- `OBSERVER` behavior is not hard-coded. Seed data can default to conservative permissions, but administrators can change it.

## Architecture

The design uses three layers.

1. **RBAC data model** stores fixed permissions, role-permission bindings, and user-role bindings.
2. **Central backend authorization registry** maps module APIs to menu permissions and action permissions.
3. **Frontend management and consumption** lets administrators maintain roles and role permissions, assign roles to users, and control buttons from returned action permission codes.

Controllers should not each implement custom authorization logic. Spring Security should delegate `/api/**` decisions to a central authorization component after public login and constant-route exceptions. The component resolves the current user’s effective permissions from enabled roles and enabled permission points, then checks the API registry.

Menu filtering and API authorization must come from the same permission model. `sys_menu_route.required_role` can remain temporarily for compatibility, but it is not the long-term source of truth for v0.0.8 route access.

## Data model

### `sys_permission`

Fixed permission point table, maintained by seed data and code review rather than arbitrary UI creation.

Recommended fields:

- `id`
- `permission_key`
- `permission_name`
- `permission_type`, values: `menu`, `action`
- `module_key`
- `parent_key`
- `route_name`
- `action_key`
- `sort_order`
- `enabled`
- `created_at`
- `updated_at`

Semantics:

- Menu permission points map to dynamic route names such as `asset_database`, `deploy_task`, `traffic_controller`, and `system_user`.
- Action permission points map to business actions such as create, update, delete, execute, approve, preview, apply, rollback, and manage.
- Action permissions are displayed under their owning menu or module.
- Disabled permissions are excluded from authorization and from editable role permission choices.

### `sys_role_permission`

Role-permission binding table.

Recommended fields:

- `role_id`
- `permission_id`
- `created_at`

Semantics:

- A role can own many menu and action permissions.
- Saving role permissions replaces the role’s complete permission set in one transaction.
- Bindings to disabled or missing permission points are rejected.

### `sys_role`

Existing role table becomes UI-managed.

Expected behavior:

- Role list supports search and enabled/disabled state.
- Role create and update are allowed for administrators with `system:role:manage`.
- Built-in `SUPER_ADMIN` can be viewed and edited for permissions only if the system remains safe, but it cannot be deleted.
- The system must preserve at least one enabled `SUPER_ADMIN` user to avoid lockout.
- Disabled roles do not contribute permissions.

### `sys_user_role`

Existing user-role binding remains the user-role source of truth.

Expected behavior:

- User Management owns user-role assignment.
- Only enabled roles are offered for assignment.
- Saving user roles replaces that user’s role set in one transaction.
- The system must not allow removing the last enabled `SUPER_ADMIN` user.

## Permission semantics

Authorization follows this order:

1. If the endpoint is public, allow it.
2. If the user is unauthenticated, return 401.
3. Resolve effective permissions from enabled roles and enabled permission points.
4. Match the request to the central API authorization registry.
5. If the matched rule has a menu permission and the user lacks it, return 403.
6. If the matched rule has an action permission and the user lacks it, return 403.
7. Otherwise allow the request.

Menu permission is a prerequisite. If a user lacks `deploy_task`, they cannot call Deploy task APIs even if they somehow have `deploy:task:execute`.

Read APIs generally require menu permission. Mutating or sensitive APIs require menu permission plus action permission.

`SUPER_ADMIN` seed role owns all enabled permissions. This should be represented through seeded role-permission rows, not a hidden bypass, so the UI reflects the real model.

## Backend API design

New RBAC management APIs live under `/api/system/rbac`.

### Roles

- `GET /api/system/rbac/roles`
  - Returns role list with id, key, name, description, enabled state, built-in marker, and timestamps.
- `POST /api/system/rbac/roles`
  - Creates a role.
- `PUT /api/system/rbac/roles/{id}`
  - Updates role name, description, and enabled state.
- `GET /api/system/rbac/roles/{id}/permissions`
  - Returns assigned menu and action permission keys for a role.
- `PUT /api/system/rbac/roles/{id}/permissions`
  - Replaces assigned permissions for a role.

### Permissions

- `GET /api/system/rbac/permissions`
  - Returns the fixed permission tree grouped by module and menu.
  - Includes menu permissions and child action permissions.
  - Excludes disabled permission points by default.

### User-role assignment

User-role assignment belongs to User Management:

- `GET /api/system/users/{id}/roles`
  - Returns user role ids/keys.
- `PUT /api/system/users/{id}/roles`
  - Replaces user role assignments.

### Auth and route APIs

Existing auth and route APIs remain, with updated payloads and filtering:

- `POST /api/auth/login`, `POST /api/auth/sendCode`, and `POST /api/auth/codeLogin` stay public.
- `GET /api/routes/getConstantRoutes` stays public.
- `GET /api/auth/getUserInfo` requires login and returns roles plus action permission codes.
- `GET /api/routes/getUserRoutes` requires login and returns only routes backed by menu permissions.

## Central API authorization registry

The registry maps stable endpoint groups to menu and action permissions. It is code-defined in v0.0.8, not edited through UI.

Required first-version coverage:

### Home

- Home dashboard read APIs require `home` menu permission when such APIs exist.

### Asset

- `GET /api/assets/hosts/**` requires `asset_host`.
- Mutating host APIs require `asset:host:manage`.
- `GET /api/assets/groups/**` requires `asset_group`.
- Mutating group APIs require `asset:group:manage` if mutating group APIs exist.
- `GET /api/assets/tags/**` requires `asset_tag`.
- Mutating tag APIs require `asset:tag:manage` if mutating tag APIs exist.
- `GET /api/assets/credentials/**` requires `asset_credential`.
- Mutating credential APIs require `asset:credential:manage`.
- `GET /api/assets/databases/**` requires `asset_database`.
- Database create/update/delete require `asset:database:manage`.
- Database connectivity checks require `asset:database:connectivity-check`.

### Monitor

- `GET /api/monitor/detect-tasks/**` requires `monitor_detect-task`.
- Creating or executing detect tasks requires `monitor:detect-task:execute`.
- `GET /api/monitor/hosts/*/facts/latest` requires `monitor_metric`.

### App

- App definition APIs require `app_definition`; mutating APIs require `app:definition:manage`.
- App version APIs require `app_version`; mutating APIs require `app:version:manage`.
- Package APIs require `app_package`; upload/delete require `app:package:manage`.
- Config template APIs require `app_config-template`; mutating APIs require `app:config-template:manage`.
- Script template APIs require `app_script-template`; mutating APIs require `app:script-template:manage`.

### Deploy

- Deploy task read APIs require `deploy_task`.
- Creating deploy tasks requires `deploy:task:create`.
- Approve/reject require `deploy:task:approve`.
- Execute requires `deploy:task:execute`.
- Cancel requires `deploy:task:cancel`.
- Retry requires `deploy:task:retry`.
- Rollback requires `deploy:task:rollback`.
- Deploy executor read APIs require `deploy_task`.

### Task Center

- `GET /api/task-center/tasks/**` requires `task_center`.
- The tracking page route uses `task_tracking_[id]`; tracking API access requires `task_center`.
- v0.0.8 does not add Task Center retry, cancel, orchestration, or full log platform behavior.

### Traffic

- Traffic policy and plugin read APIs require `traffic_controller`.
- Preview requires `traffic:policy:preview`.
- Apply requires `traffic:policy:apply`.
- Rollback requires `traffic:policy:rollback`.

### System

- System user APIs require `system_user` for read and `system:user:manage` for mutation.
- RBAC management APIs require a new `system_rbac` menu permission and `system:role:manage` action permission.

### Fallback

A matched business API must have an explicit registry rule. During v0.0.8 implementation, all existing module APIs must either be registered or deliberately documented as public/authenticated-only with tests.

Unknown `/api/**` endpoints remain authenticated by default in the first version to avoid breaking future development, but release validation must include a registry coverage check for current module controllers.

## Frontend design

### Permission Management page

Add a System menu entry for Permission Management with route name `system_rbac`.

Layout is role-first:

- Left panel: searchable role list.
- Right panel: selected role details and permission tree.

Role list behavior:

- Shows role name, role key, enabled/disabled state, and built-in marker.
- Supports creating a role.
- Selecting a role loads its details and permissions.

Role detail behavior:

- Edits role name, description, and enabled state.
- Shows role key as immutable after creation.
- Prevents deleting built-in roles in v0.0.8.

Permission tree behavior:

- Groups permissions by module.
- Shows menu permissions as parent nodes.
- Shows action permissions below the related menu/module.
- If a menu permission is unchecked, child action permissions are automatically unchecked and disabled until the menu permission is selected again.
- Saving submits the complete permission key set for the role.

### User Management enhancement

User Management gets role assignment controls:

- View a user’s assigned roles.
- Assign enabled roles.
- Save the full role set.
- Prevent changes that would remove the last enabled `SUPER_ADMIN` user.

### Runtime permission consumption

Frontend gets action permission codes from user info.

Expected behavior:

- Dynamic routes come from backend route filtering.
- High-risk buttons are disabled without action permission.
- Execute/delete/approve/apply/rollback buttons show a tooltip explaining the missing permission.
- Frontend permission checks are UX only. Backend remains authoritative.

## Seed role defaults

Seed data should create permissions for all current modules and bind them to initial roles.

Seed defaults:

- `SUPER_ADMIN`: all enabled menu and action permissions.
- `PLATFORM_ADMIN`: `home`; all Asset menu permissions; `asset:host:manage`; `asset:credential:manage`; `asset:database:manage`; `asset:database:connectivity-check`; all Monitor menu permissions; `monitor:detect-task:execute`; `task_center`.
- `RELEASE_MANAGER`: `home`; all App menu permissions as read-only; `deploy_task`; `deploy:task:create`; `deploy:task:approve`; `deploy:task:execute`; `deploy:task:cancel`; `deploy:task:retry`; `deploy:task:rollback`; `task_center`.
- `TRAFFIC_OWNER`: `home`; `traffic_controller`; `traffic:policy:preview`; `traffic:policy:apply`; `traffic:policy:rollback`; `task_center`.
- `OBSERVER`: `home`; `task_center`.

All defaults are represented as normal role-permission rows and can be changed by administrators after deployment.

## Error handling

- Unauthenticated requests return 401 with the existing JSON response shape.
- Authenticated requests missing menu or action permission return 403 with the existing JSON response shape.
- Missing role returns 404 with the existing JSON response shape.
- Disabled roles do not contribute permissions.
- Disabled permissions cannot be assigned and do not authorize requests.
- Saving role permissions with unknown keys fails.
- Deleting built-in roles is not supported in v0.0.8.
- Disabling or unassigning the last effective `SUPER_ADMIN` user is rejected.

## Testing plan

### Backend tests

Add focused tests for:

- Permission calculation with enabled roles.
- Disabled roles excluded from effective permissions.
- Disabled permissions excluded from effective permissions.
- Route filtering by menu permission.
- User info returning action permission codes.
- Role list, role create/update, permission tree, get role permissions, save role permissions.
- User-role get/save from User Management.
- Last-admin protection.

Add authorization tests by module:

- Unauthenticated request returns 401.
- Authenticated user without menu permission returns 403 for read API.
- Authenticated user with menu but without action permission returns 403 for write/action API.
- Authenticated user with menu and action permission succeeds.

Minimum module coverage:

- Asset credential/database read and mutation.
- Database connectivity check.
- Monitor detect task execute.
- App package/config/script mutation.
- Deploy create, approve, execute, rollback.
- Task Center list/detail/tracking read.
- Traffic preview, apply, rollback.
- System user management and RBAC management.

### Frontend tests

Add tests for:

- Permission Management role list rendering.
- Permission tree parent-child behavior.
- Saving role permissions payload.
- Built-in role restrictions in UI.
- User Management role assignment payload.
- Action button disabled or hidden state for missing permission.
- Dynamic route/menu absence for missing menu permission.

### Documentation and release materials

Update:

- `README.md`
- `docs/envops-项目详细说明.md`
- `docs/envops-开发技术说明.md`
- `docs/envops-用户操作手册.md`
- `release/0.0.8-release-notes.md`

Docs must state:

- v0.0.8 adds RBAC management.
- Permission points are fixed by system seed data.
- Administrators can maintain roles, role permissions, and user-role assignments.
- Backend API authorization is authoritative.
- UI button controls are convenience, not security boundaries.

## Implementation constraints

- Keep the first implementation small enough to ship in one version: fixed permission points, role-first UI, user-role assignment, and central backend authorization.
- Avoid action permission names that mirror raw HTTP paths. Use business action names.
- Prefer explicit authorization tests over relying on generic security config tests.
- Keep compatibility fields such as `sys_menu_route.required_role` only as migration support, not as the new source of truth.
