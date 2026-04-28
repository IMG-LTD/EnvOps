DROP TABLE IF EXISTS deploy_task_log;
DROP TABLE IF EXISTS deploy_task_param;
DROP TABLE IF EXISTS deploy_task_host;
DROP TABLE IF EXISTS unified_task_center;
DROP TABLE IF EXISTS deploy_task;
DROP TABLE IF EXISTS app_version;
DROP TABLE IF EXISTS app_script_template;
DROP TABLE IF EXISTS app_config_template;
DROP TABLE IF EXISTS app_package;
DROP TABLE IF EXISTS app_definition;
DROP TABLE IF EXISTS monitor_host_fact;
DROP TABLE IF EXISTS monitor_detect_task;
DROP TABLE IF EXISTS asset_database;
DROP TABLE IF EXISTS asset_tag;
DROP TABLE IF EXISTS asset_group;
DROP TABLE IF EXISTS asset_credential;
DROP TABLE IF EXISTS asset_host;
DROP TABLE IF EXISTS traffic_policy;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu_route;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE asset_host (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    host_name VARCHAR(64) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    cluster_name VARCHAR(128) NOT NULL,
    owner_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_heartbeat TIMESTAMP NOT NULL
);

CREATE TABLE asset_credential (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    credential_type VARCHAR(32) NOT NULL,
    username VARCHAR(128),
    secret VARCHAR(512),
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE asset_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    host_count INT NOT NULL DEFAULT 0
);

CREATE TABLE asset_database (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    database_name VARCHAR(128) NOT NULL,
    database_type VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    host_id BIGINT NOT NULL,
    port INT NOT NULL,
    instance_name VARCHAR(128),
    credential_id BIGINT,
    owner_name VARCHAR(128) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    connectivity_status VARCHAR(32) NOT NULL,
    connection_username VARCHAR(128),
    connection_password VARCHAR(512),
    description VARCHAR(255),
    last_checked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_asset_database_env_host_port_name UNIQUE (environment, host_id, port, database_name),
    CONSTRAINT fk_asset_database_host FOREIGN KEY (host_id) REFERENCES asset_host (id),
    CONSTRAINT fk_asset_database_credential FOREIGN KEY (credential_id) REFERENCES asset_credential (id)
);

CREATE TABLE asset_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(32),
    description VARCHAR(255)
);

CREATE TABLE monitor_detect_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(128) NOT NULL,
    host_id BIGINT NOT NULL,
    target VARCHAR(128) NOT NULL,
    schedule VARCHAR(64) NOT NULL,
    last_run_at TIMESTAMP,
    last_result VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_monitor_detect_task_host FOREIGN KEY (host_id) REFERENCES asset_host (id)
);

CREATE TABLE monitor_host_fact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    host_id BIGINT NOT NULL,
    host_name VARCHAR(64) NOT NULL,
    os_name VARCHAR(64) NOT NULL,
    kernel_version VARCHAR(64) NOT NULL,
    cpu_cores INT NOT NULL,
    memory_mb INT NOT NULL,
    agent_version VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_monitor_host_fact_host FOREIGN KEY (host_id) REFERENCES asset_host (id)
);

CREATE TABLE app_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_code VARCHAR(64) NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    app_type VARCHAR(32) NOT NULL,
    runtime_type VARCHAR(32),
    deploy_mode VARCHAR(32),
    default_port INT,
    health_check_path VARCHAR(255),
    description VARCHAR(500),
    status TINYINT DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_definition_code_deleted UNIQUE (app_code, deleted)
);

CREATE TABLE app_package (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_name VARCHAR(255) NOT NULL,
    package_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_hash VARCHAR(128),
    storage_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_config_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_content CLOB NOT NULL,
    render_engine VARCHAR(32) NOT NULL DEFAULT 'PLAINTEXT',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_config_template_code_deleted UNIQUE (template_code, deleted)
);

CREATE TABLE app_script_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    script_type VARCHAR(32) NOT NULL,
    script_content CLOB NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_script_template_code_deleted UNIQUE (template_code, deleted)
);

CREATE TABLE app_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    package_id BIGINT,
    config_template_id BIGINT,
    script_template_id BIGINT,
    changelog VARCHAR(1000),
    status TINYINT DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_version UNIQUE (app_id, version_no, deleted),
    CONSTRAINT fk_app_version_definition FOREIGN KEY (app_id) REFERENCES app_definition (id),
    CONSTRAINT fk_app_version_package FOREIGN KEY (package_id) REFERENCES app_package (id),
    CONSTRAINT fk_app_version_config_template FOREIGN KEY (config_template_id) REFERENCES app_config_template (id),
    CONSTRAINT fk_app_version_script_template FOREIGN KEY (script_template_id) REFERENCES app_script_template (id)
);

CREATE TABLE deploy_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    task_name VARCHAR(128) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    app_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    origin_task_id BIGINT,
    status VARCHAR(32) NOT NULL,
    batch_strategy VARCHAR(32) NOT NULL,
    batch_size INT NOT NULL DEFAULT 0,
    target_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    operator_name VARCHAR(64) NOT NULL,
    approval_operator_name VARCHAR(64),
    approval_comment VARCHAR(255),
    approval_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_deploy_task_no UNIQUE (task_no),
    CONSTRAINT fk_deploy_task_app FOREIGN KEY (app_id) REFERENCES app_definition (id),
    CONSTRAINT fk_deploy_task_version FOREIGN KEY (version_id) REFERENCES app_version (id),
    CONSTRAINT fk_deploy_task_origin FOREIGN KEY (origin_task_id) REFERENCES deploy_task (id)
);

CREATE TABLE unified_task_center (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(64) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    triggered_by VARCHAR(128) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    summary VARCHAR(500) NOT NULL,
    detail_preview CLOB NOT NULL,
    tracking_timeline TEXT,
    tracking_log_summary TEXT,
    source_id BIGINT NULL,
    source_route VARCHAR(255) NOT NULL,
    log_route VARCHAR(512),
    module_name VARCHAR(64) NOT NULL,
    error_summary VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_unified_task_center_started_at ON unified_task_center (started_at);
CREATE INDEX idx_unified_task_center_type_status ON unified_task_center (task_type, status);
CREATE UNIQUE INDEX uk_unified_task_center_source_ref ON unified_task_center (task_type, source_id);

CREATE TABLE deploy_task_host (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_step VARCHAR(64),
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_msg VARCHAR(500),
    CONSTRAINT fk_deploy_task_host_task FOREIGN KEY (task_id) REFERENCES deploy_task (id),
    CONSTRAINT fk_deploy_task_host_host FOREIGN KEY (host_id) REFERENCES asset_host (id),
    CONSTRAINT uk_deploy_task_host UNIQUE (task_id, host_id)
);

CREATE TABLE deploy_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_host_id BIGINT,
    log_level VARCHAR(16) NOT NULL,
    log_content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deploy_task_log_task FOREIGN KEY (task_id) REFERENCES deploy_task (id),
    CONSTRAINT fk_deploy_task_log_task_host FOREIGN KEY (task_host_id) REFERENCES deploy_task_host (id)
);

CREATE TABLE deploy_task_param (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    param_key VARCHAR(128) NOT NULL,
    param_value VARCHAR(1000) NOT NULL,
    secret_flag TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_deploy_task_param_task FOREIGN KEY (task_id) REFERENCES deploy_task (id)
);

CREATE TABLE traffic_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app VARCHAR(128) NOT NULL,
    strategy VARCHAR(64) NOT NULL,
    scope VARCHAR(128) NOT NULL,
    traffic_ratio VARCHAR(32) NOT NULL,
    owner VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    plugin_type VARCHAR(32) NOT NULL,
    rollback_token VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    phone VARCHAR(16) NOT NULL UNIQUE,
    team_key VARCHAR(32) NOT NULL DEFAULT 'envops',
    login_type VARCHAR(32) NOT NULL DEFAULT 'PASSWORD',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP NULL
);

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

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_menu_route (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    route_name VARCHAR(64) NOT NULL,
    route_path VARCHAR(128) NOT NULL,
    component VARCHAR(128) NOT NULL,
    title VARCHAR(128) NOT NULL,
    icon VARCHAR(64),
    route_order INT NOT NULL,
    route_type VARCHAR(32) NOT NULL,
    required_role VARCHAR(64),
    hide_in_menu BOOLEAN DEFAULT FALSE,
    active_menu VARCHAR(128),
    home_flag BOOLEAN DEFAULT FALSE
);
