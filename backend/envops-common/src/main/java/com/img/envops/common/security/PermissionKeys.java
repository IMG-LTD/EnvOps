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
