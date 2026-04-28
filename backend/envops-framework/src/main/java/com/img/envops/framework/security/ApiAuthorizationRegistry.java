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
