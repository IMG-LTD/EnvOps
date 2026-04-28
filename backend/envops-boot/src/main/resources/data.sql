INSERT INTO asset_host (host_name, ip_address, environment, cluster_name, owner_name, status, last_heartbeat)
VALUES
    ('host-prd-01', '10.20.1.11', 'production', 'cn-shanghai-a', 'EnvOps', 'online', TIMESTAMP '2026-04-15 09:32:00'),
    ('host-prd-02', '10.20.1.12', 'production', 'cn-shanghai-a', 'Traffic Team', 'warning', TIMESTAMP '2026-04-15 09:27:00'),
    ('host-stg-01', '10.30.2.18', 'staging', 'cn-beijing-b', 'Release Team', 'online', TIMESTAMP '2026-04-15 09:31:00'),
    ('host-sbx-01', '10.40.8.6', 'sandbox', 'cn-hangzhou-c', 'QA Team', 'offline', TIMESTAMP '2026-04-15 08:54:00');

INSERT INTO asset_credential (name, credential_type, username, secret, description, created_at)
VALUES
    ('demo-fake-prod-root-password', 'ssh_password', 'root', 'protected:v1:ntn0obA2laEO3Jeahpora_mr9ZjsvqGYCM1N2Q53Kzg', '演示占位：FAKE_PROD_ROOT_PASSWORD_DO_NOT_USE', TIMESTAMP '2026-04-10 10:00:00'),
    ('demo-fake-staging-deploy-key', 'ssh_key', 'deploy', 'protected:v1:TQRbBwdAbZ8DojY7ab_y_-fQE01XXmULxSBt9766vik', '演示占位：FAKE_STAGING_DEPLOY_KEY_DO_NOT_USE', TIMESTAMP '2026-04-11 14:30:00');

INSERT INTO asset_group (name, description, host_count)
VALUES
    ('production-core', '核心生产主机分组', 2),
    ('staging-apps', '预发应用主机分组', 1),
    ('sandbox-lab', '沙箱实验主机分组', 1);

INSERT INTO asset_database (database_name, database_type, environment, host_id, port, instance_name, credential_id, owner_name, lifecycle_status, connectivity_status, connection_username, connection_password, description, last_checked_at, created_at, updated_at)
VALUES
    ('order_prod', 'mysql', 'production', 1, 3306, 'mysql-prd-a', 1, 'Platform DBA', 'managed', 'online', 'orders_app', 'sealed:v1:1gdCFwLAw-pS5GCsV1Cd1aq6RE6HgEtxF6JoONSKDzqYg4freLYh0WrA', '订单主库生产实例', TIMESTAMP '2026-04-18 09:10:00', TIMESTAMP '2026-04-14 10:00:00', TIMESTAMP '2026-04-18 09:10:00'),
    ('traffic_gate', 'postgresql', 'production', 2, 5432, 'pg-prd-b', NULL, 'Traffic DBA', 'managed', 'warning', NULL, NULL, '流量规则存储实例', TIMESTAMP '2026-04-18 08:20:00', TIMESTAMP '2026-04-14 11:00:00', TIMESTAMP '2026-04-18 08:20:00'),
    ('billing_archive', 'oracle', 'staging', 3, 1521, 'oracle-stg-a', 2, 'Finance DBA', 'managed', 'warning', 'archive_app', 'sealed:v1:YLWMmQshluZz6d9F9T3-kxkXOp_j6-lj2cnY5y5c1IyAn39L8wm6oGWXjQ', '归档计费库', TIMESTAMP '2026-04-18 07:40:00', TIMESTAMP '2026-04-14 11:40:00', TIMESTAMP '2026-04-18 07:40:00'),
    ('ops_metrics', 'sqlserver', 'production', 2, 1433, 'sqlserver-prd-a', NULL, 'Ops DBA', 'managed', 'online', NULL, NULL, '运维指标聚合实例', TIMESTAMP '2026-04-18 08:55:00', TIMESTAMP '2026-04-14 11:50:00', TIMESTAMP '2026-04-18 08:55:00'),
    ('event_bus', 'mongodb', 'staging', 3, 27017, 'mongo-stg-a', 2, 'Platform DBA', 'managed', 'unknown', NULL, NULL, '事件总线文档库', NULL, TIMESTAMP '2026-04-14 12:00:00', TIMESTAMP '2026-04-14 12:00:00'),
    ('session_hub', 'redis', 'sandbox', 4, 6379, 'redis-sbx-a', NULL, 'QA Team', 'disabled', 'unknown', 'sandbox_cache', 'sealed:v1:xdNUrMAViunULCU1YPzdAaQUJzYlXd_8lgZCU9aGM_XsU1lfCs0G_6uR9A', '沙箱会话缓存实例', NULL, TIMESTAMP '2026-04-14 12:10:00', TIMESTAMP '2026-04-14 12:10:00');

INSERT INTO asset_tag (name, color, description)
VALUES
    ('linux', '#18a058', 'Linux 系统主机'),
    ('database', '#2080f0', '数据库相关主机'),
    ('needs-maintenance', '#f0a020', '待安排维护窗口');

INSERT INTO monitor_detect_task (task_name, host_id, target, schedule, last_run_at, last_result, created_at)
VALUES
    ('node-baseline-check', 1, 'host-prd-01', 'every_10m', TIMESTAMP '2026-04-15 09:30:00', 'success', TIMESTAMP '2026-04-15 09:00:00'),
    ('nginx-config-diff', 2, 'host-prd-02', 'every_1h', TIMESTAMP '2026-04-15 09:00:00', 'warning', TIMESTAMP '2026-04-15 08:00:00');

INSERT INTO monitor_host_fact (host_id, host_name, os_name, kernel_version, cpu_cores, memory_mb, agent_version, collected_at)
VALUES
    (1, 'host-prd-01', 'Ubuntu 22.04.4 LTS', '5.15.0-105-generic', 8, 16384, '1.0.0', TIMESTAMP '2026-04-15 09:32:00'),
    (1, 'host-prd-01', 'Ubuntu 22.04.4 LTS', '5.15.0-106-generic', 8, 16384, '1.0.1', TIMESTAMP '2026-04-16 08:45:00'),
    (2, 'host-prd-02', 'Alibaba Cloud Linux 3', '5.10.134-18.al8', 16, 32768, '1.0.0', TIMESTAMP '2026-04-16 08:30:00');

INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at)
VALUES
    (1, 'envops-admin', '$2a$10$sWJS6yVY5qaMhiYxDaI6VeCpsw4iCkM4BAnZVUGXZ3ubl2oY/WMGG', '13800138000', 'platform', 'PASSWORD', 'ACTIVE', TIMESTAMP '2026-04-18 09:15:00'),
    (20, 'release-admin', '$2a$10$OUBSEAaQp/gYQtWjZHO0lu56HJVAuTFRPNzqHBa4DcIHl/msxU.Wq', '13900139000', 'release', 'PASSWORD_OTP', 'ACTIVE', TIMESTAMP '2026-04-18 08:45:00'),
    (21, 'traffic-owner', '$2a$10$PPuMaDMld2QRRagKXK7NMOZTqMZBFSbLXZbR8pmGhJCp.OH2D0lf2', '13700137000', 'traffic', 'SSO', 'REVIEW', TIMESTAMP '2026-04-17 19:20:00'),
    (22, 'qa-observer', '$2a$10$iXuMZ4iC1HB7Wdz4mF9lde.QeVz1WR5u0HpumRPn6oEu8s68oHV1u', '13600136000', 'qa', 'SSO', 'DISABLED', TIMESTAMP '2026-04-16 18:10:00');

INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at)
VALUES
    (1, 'SUPER_ADMIN', 'Super Admin', 'Built-in full platform administrator', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'PLATFORM_ADMIN', 'Platform Admin', 'Platform operator for assets and monitor data', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'RELEASE_MANAGER', 'Release Manager', 'Release operator for app and deploy flows', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'TRAFFIC_OWNER', 'Traffic Owner', 'Traffic policy operator', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'OBSERVER', 'Observer', 'Conservative read-only observer', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (user_id, role_id)
VALUES
    (1, 1),
    (1, 2),
    (20, 1),
    (20, 3),
    (21, 4),
    (22, 5);

INSERT INTO app_definition (id, app_code, app_name, app_type, runtime_type, deploy_mode, default_port, health_check_path, description, status, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (1001, 'order-service', '订单服务', 'JAVA', 'SPRING_BOOT', 'SYSTEMD', 8080, '/actuator/health', '核心订单业务服务', 1, 0, 'seed', 'seed', TIMESTAMP '2026-04-12 10:00:00', TIMESTAMP '2026-04-12 10:00:00'),
    (1002, 'gateway-nginx', '网关 Nginx', 'NGINX', 'NGINX', 'PROCESS', 80, '/status', '统一入口网关', 1, 0, 'seed', 'seed', TIMESTAMP '2026-04-12 11:00:00', TIMESTAMP '2026-04-12 11:00:00');

INSERT INTO app_package (id, package_name, package_type, file_path, file_size, file_hash, storage_type, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (1101, 'order-service-1.0.0.jar', 'JAR', 'packages/order-service/1.0.0/order-service-1.0.0.jar', 73400320, 'sha256:order-service-1.0.0', 'LOCAL', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 12:00:00', TIMESTAMP '2026-04-12 12:00:00'),
    (1102, 'gateway-nginx-2026.04.tar', 'TAR', 'packages/gateway-nginx/2026.04/gateway-nginx-2026.04.tar', 12582912, 'sha256:gateway-nginx-2026.04', 'LOCAL', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 12:30:00', TIMESTAMP '2026-04-12 12:30:00');

INSERT INTO app_config_template (id, template_code, template_name, template_content, render_engine, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (1201, 'order-service-prod', '订单服务生产配置', 'server.port={{port}}\nspring.profiles.active=prod', 'PLAINTEXT', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 13:00:00', TIMESTAMP '2026-04-12 13:00:00'),
    (1202, 'gateway-nginx-default', '网关默认配置', 'server {\n  listen {{port}};\n}', 'PLAINTEXT', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 13:30:00', TIMESTAMP '2026-04-12 13:30:00');

INSERT INTO app_script_template (id, template_code, template_name, script_type, script_content, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (1301, 'systemd-start-java', 'Java 应用启动脚本', 'BASH', '#!/usr/bin/env bash\njava -jar {{package}}', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 14:00:00', TIMESTAMP '2026-04-12 14:00:00'),
    (1302, 'reload-nginx', 'Nginx 重载脚本', 'BASH', '#!/usr/bin/env bash\nnginx -s reload', 0, 'seed', 'seed', TIMESTAMP '2026-04-12 14:30:00', TIMESTAMP '2026-04-12 14:30:00');

INSERT INTO app_version (id, app_id, version_no, package_id, config_template_id, script_template_id, changelog, status, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (1401, 1001, '1.0.0', 1101, 1201, 1301, '初始稳定版本', 1, 0, 'seed', 'seed', TIMESTAMP '2026-04-12 15:00:00', TIMESTAMP '2026-04-12 15:00:00'),
    (1402, 1002, '2026.04', 1102, 1202, 1302, '网关月度配置基线', 1, 0, 'seed', 'seed', TIMESTAMP '2026-04-12 15:30:00', TIMESTAMP '2026-04-12 15:30:00');

INSERT INTO deploy_task (id, task_no, task_name, task_type, app_id, version_id, status, batch_strategy, batch_size, target_count, success_count, fail_count, operator_name, started_at, finished_at, deleted, created_by, updated_by, created_at, updated_at)
VALUES
    (2001, 'DT202604151600000001', 'seed-order-service-install', 'INSTALL', 1001, 1401, 'SUCCESS', 'ALL', 0, 1, 1, 0, 'envops-admin', TIMESTAMP '2026-04-15 16:00:00', TIMESTAMP '2026-04-15 16:05:00', 0, 'seed', 'seed', TIMESTAMP '2026-04-15 16:00:00', TIMESTAMP '2026-04-15 16:05:00');

INSERT INTO deploy_task_host (id, task_id, host_id, status, current_step, started_at, finished_at, error_msg)
VALUES
    (2101, 2001, 1, 'SUCCESS', 'COMPLETED', TIMESTAMP '2026-04-15 16:00:30', TIMESTAMP '2026-04-15 16:04:30', NULL);

INSERT INTO deploy_task_log (id, task_id, task_host_id, log_level, log_content, created_at)
VALUES
    (2201, 2001, 2101, 'INFO', 'Seed task completed', TIMESTAMP '2026-04-15 16:05:00');

INSERT INTO deploy_task_param (id, task_id, param_key, param_value, secret_flag)
VALUES
    (2301, 2001, 'sshUser', 'deploy', 0),
    (2302, 2001, 'sshPort', '22', 0),
    (2303, 2001, 'privateKeyPath', '/opt/envops/keys/demo-release.pem', 1),
    (2304, 2001, 'remoteBaseDir', '/opt/envops/releases', 0);

INSERT INTO traffic_policy (id, app, strategy, scope, traffic_ratio, owner, status, plugin_type, rollback_token, created_at, updated_at)
VALUES
    (3001, 'checkout-gateway', 'weighted_routing', 'prod / cn-beijing-b', '10%', 'platform-team', 'REVIEW', 'REST', NULL, TIMESTAMP '2026-04-22 10:00:00', TIMESTAMP '2026-04-22 10:00:00'),
    (3002, 'billing-admin', 'weighted_routing', 'staging / all', '20%', 'release-team', 'PREVIEW', 'REST', 'rb-apply-3002', TIMESTAMP '2026-04-22 10:05:00', TIMESTAMP '2026-04-22 10:05:00'),
    (3003, 'ops-worker', 'header_canary', 'prod / cn-shanghai-a', '5%', 'traffic-team', 'REVIEW', 'NGINX', NULL, TIMESTAMP '2026-04-22 10:10:00', TIMESTAMP '2026-04-22 10:10:00');

INSERT INTO unified_task_center (
    id, task_type, task_name, status, triggered_by, started_at, finished_at,
    summary, detail_preview, source_id, source_route, module_name, error_summary,
    created_at, updated_at
) VALUES
    (
        9001, 'deploy', 'Deploy order-service to production', 'success', 'envops-admin',
        TIMESTAMP '2026-04-15 16:00:00', TIMESTAMP '2026-04-15 16:05:00',
        '发布 order-service 到 production，1 台主机，已完成',
        '{"app":"order-service","environment":"production","targetCount":1,"successCount":1,"failCount":0,"rawStatus":"SUCCESS","sourceRoute":"/deploy/task?taskId=2001"}',
        2001, '/deploy/task?taskId=2001', 'deploy', NULL,
        TIMESTAMP '2026-04-15 16:00:00', TIMESTAMP '2026-04-15 16:05:00'
    ),
    (
        9002, 'database_connectivity', '批量数据库连通性检测', 'failed', 'envops-admin',
        TIMESTAMP '2026-04-21 09:00:00', TIMESTAMP '2026-04-21 09:02:00',
        '批量检测 20 条，成功 16，失败 3，跳过 1',
        '{"mode":"batch","summary":"批量检测 20 条，成功 16，失败 3，跳过 1","total":20,"success":16,"failed":3,"skipped":1,"sourceRoute":"/asset/database","errorSummary":"3 databases failed authentication"}',
        NULL, '/asset/database', 'asset', '3 databases failed authentication',
        TIMESTAMP '2026-04-21 09:00:00', TIMESTAMP '2026-04-21 09:02:00'
    ),
    (
        9003, 'traffic_action', 'Traffic Apply', 'failed', 'envops-admin',
        TIMESTAMP '2026-04-22 08:30:00', TIMESTAMP '2026-04-22 08:31:00',
        'Apply checkout-gateway，策略 weighted_routing，插件 REST',
        '{"action":"apply","app":"checkout-gateway","strategy":"weighted_routing","plugin":"REST","rollbackTokenAvailable":false,"sourceRoute":"/traffic/controller","errorSummary":"rollbackToken is required from traffic rest service"}',
        3001, '/traffic/controller', 'traffic', 'rollbackToken is required from traffic rest service',
        TIMESTAMP '2026-04-22 08:30:00', TIMESTAMP '2026-04-22 08:31:00'
    );

INSERT INTO sys_menu_route (id, parent_id, route_name, route_path, component, title, icon, route_order, route_type, required_role, home_flag, hide_in_menu, active_menu)
VALUES
    (100, NULL, 'login', '/login', 'layout.blank$view.login', '登录', 'mdi:login', 1, 'CONSTANT', NULL, FALSE, FALSE, NULL),
    (101, NULL, '403', '/403', 'layout.blank$view.403', '403', 'mdi:alert-circle', 2, 'CONSTANT', NULL, FALSE, FALSE, NULL),
    (200, NULL, 'home', '/home', 'layout.base$view.home', '首页', 'mdi:monitor-dashboard', 1, 'USER', 'SUPER_ADMIN', TRUE, FALSE, NULL),
    (210, NULL, 'asset', '/asset', 'layout.base', '资产中心', 'mdi:server-network', 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (211, 210, 'asset_host', '/asset/host', 'view.asset_host', '主机管理', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (212, 210, 'asset_group', '/asset/group', 'view.asset_group', '分组管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (213, 210, 'asset_tag', '/asset/tag', 'view.asset_tag', '标签管理', NULL, 3, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (214, 210, 'asset_credential', '/asset/credential', 'view.asset_credential', '凭据管理', NULL, 4, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (215, 210, 'asset_database', '/asset/database', 'view.asset_database', '数据库资源', NULL, 5, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (220, NULL, 'monitor', '/monitor', 'layout.base', '检测中心', 'mdi:radar', 3, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (221, 220, 'monitor_detect-task', '/monitor/detect-task', 'view.monitor_detect-task', '即时检测', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (222, 220, 'monitor_metric', '/monitor/metric', 'view.monitor_metric', '指标快照', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (230, NULL, 'app', '/app', 'layout.base', '应用中心', 'mdi:application-braces-outline', 4, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (231, 230, 'app_definition', '/app/definition', 'view.app_definition', '应用定义', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (232, 230, 'app_version', '/app/version', 'view.app_version', '版本管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (233, 230, 'app_package', '/app/package', 'view.app_package', '安装包管理', NULL, 3, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (234, 230, 'app_config-template', '/app/config-template', 'view.app_config-template', '配置模板', NULL, 4, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (235, 230, 'app_script-template', '/app/script-template', 'view.app_script-template', '脚本模板', NULL, 5, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (240, NULL, 'deploy', '/deploy', 'layout.base', '安装发布', 'mdi:rocket-launch-outline', 5, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (241, 240, 'deploy_task', '/deploy/task', 'view.deploy_task', '发布任务', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (250, NULL, 'task', '/task', 'layout.base', '任务中心', 'mdi:clipboard-text-clock-outline', 6, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (251, 250, 'task_center', '/task/center', 'view.task_center', '全部任务', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (252, 250, 'task_tracking_[id]', '/task/tracking/:id', 'view.task_tracking_[id]', '任务追踪', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, TRUE, 'task_center'),
    (260, NULL, 'traffic', '/traffic', 'layout.base', '流量控制', 'mdi:source-branch', 7, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (261, 260, 'traffic_controller', '/traffic/controller', 'view.traffic_controller', '流量规则', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (270, NULL, 'system', '/system', 'layout.base', '系统管理', 'mdi:account-cog-outline', 8, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (271, 270, 'system_user', '/system/user', 'view.system_user', '用户管理', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL),
    (272, 270, 'system_rbac', '/system/rbac', 'view.system_rbac', '权限管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, FALSE, NULL);

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
