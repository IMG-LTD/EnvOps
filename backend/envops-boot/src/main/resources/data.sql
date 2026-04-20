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

INSERT INTO sys_role (id, role_key, role_name)
VALUES
    (1, 'SUPER_ADMIN', 'Super Admin'),
    (2, 'PLATFORM_ADMIN', 'Platform Admin'),
    (3, 'RELEASE_MANAGER', 'Release Manager'),
    (4, 'TRAFFIC_OWNER', 'Traffic Owner'),
    (5, 'OBSERVER', 'Observer');

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
    (3001, 'checkout-gateway', 'header_canary', 'prod / cn-shanghai-a', '20%', 'traffic-team', 'ENABLED', 'NGINX', 'traffic-rb-3001', TIMESTAMP '2026-04-15 17:00:00', TIMESTAMP '2026-04-15 17:00:00'),
    (3002, 'billing-admin', 'blue_green', 'staging / all', '100%', 'release-team', 'PREVIEW', 'REST', 'traffic-rb-3002', TIMESTAMP '2026-04-15 17:05:00', TIMESTAMP '2026-04-15 17:05:00'),
    (3003, 'ops-worker', 'weighted_routing', 'prod / cn-beijing-b', '10%', 'platform-team', 'REVIEW', 'NGINX', NULL, TIMESTAMP '2026-04-15 17:10:00', TIMESTAMP '2026-04-15 17:10:00');

INSERT INTO sys_menu_route (id, parent_id, route_name, route_path, component, title, icon, route_order, route_type, required_role, home_flag)
VALUES
    (100, NULL, 'login', '/login', 'layout.blank$view.login', '登录', 'mdi:login', 1, 'CONSTANT', NULL, FALSE),
    (101, NULL, '403', '/403', 'layout.blank$view.403', '403', 'mdi:alert-circle', 2, 'CONSTANT', NULL, FALSE),
    (200, NULL, 'home', '/home', 'layout.base$view.home', '首页', 'mdi:monitor-dashboard', 1, 'USER', 'SUPER_ADMIN', TRUE),
    (210, NULL, 'asset', '/asset', 'layout.base', '资产中心', 'mdi:server-network', 2, 'USER', 'SUPER_ADMIN', FALSE),
    (211, 210, 'asset_host', '/asset/host', 'view.asset_host', '主机管理', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (212, 210, 'asset_group', '/asset/group', 'view.asset_group', '分组管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE),
    (213, 210, 'asset_tag', '/asset/tag', 'view.asset_tag', '标签管理', NULL, 3, 'USER', 'SUPER_ADMIN', FALSE),
    (214, 210, 'asset_credential', '/asset/credential', 'view.asset_credential', '凭据管理', NULL, 4, 'USER', 'SUPER_ADMIN', FALSE),
    (220, NULL, 'monitor', '/monitor', 'layout.base', '检测中心', 'mdi:radar', 3, 'USER', 'SUPER_ADMIN', FALSE),
    (221, 220, 'monitor_detect-task', '/monitor/detect-task', 'view.monitor_detect-task', '即时检测', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (222, 220, 'monitor_metric', '/monitor/metric', 'view.monitor_metric', '指标快照', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE),
    (230, NULL, 'app', '/app', 'layout.base', '应用中心', 'mdi:application-braces-outline', 4, 'USER', 'SUPER_ADMIN', FALSE),
    (231, 230, 'app_definition', '/app/definition', 'view.app_definition', '应用定义', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (232, 230, 'app_version', '/app/version', 'view.app_version', '版本管理', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE),
    (233, 230, 'app_package', '/app/package', 'view.app_package', '安装包管理', NULL, 3, 'USER', 'SUPER_ADMIN', FALSE),
    (234, 230, 'app_config-template', '/app/config-template', 'view.app_config-template', '配置模板', NULL, 4, 'USER', 'SUPER_ADMIN', FALSE),
    (235, 230, 'app_script-template', '/app/script-template', 'view.app_script-template', '脚本模板', NULL, 5, 'USER', 'SUPER_ADMIN', FALSE),
    (240, NULL, 'deploy', '/deploy', 'layout.base', '安装发布', 'mdi:rocket-launch-outline', 5, 'USER', 'SUPER_ADMIN', FALSE),
    (241, 240, 'deploy_task', '/deploy/task', 'view.deploy_task', '发布任务', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (250, NULL, 'task', '/task', 'layout.base', '任务中心', 'mdi:clipboard-text-clock-outline', 6, 'USER', 'SUPER_ADMIN', FALSE),
    (251, 250, 'task_center', '/task/center', 'view.task_center', '全部任务', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (260, NULL, 'traffic', '/traffic', 'layout.base', '流量控制', 'mdi:source-branch', 7, 'USER', 'SUPER_ADMIN', FALSE),
    (261, 260, 'traffic_controller', '/traffic/controller', 'view.traffic_controller', '流量规则', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE),
    (270, NULL, 'system', '/system', 'layout.base', '系统管理', 'mdi:account-cog-outline', 8, 'USER', 'SUPER_ADMIN', FALSE),
    (271, 270, 'system_user', '/system/user', 'view.system_user', '用户管理', NULL, 1, 'USER', 'SUPER_ADMIN', FALSE);
