# EnvOps v0.0.4 项目详细说明

## 1. 文档说明

本文档用于说明当前仓库 `v0.0.4` 基线的项目定位、交付范围、本地联调方式与版本边界。当前版本重点不是继续扩张功能承诺，而是完成 release gate 修复，统一工程闸口、账号安全、本地默认配置，以及 Deploy / Task / Traffic 的对外口径。

## 2. 版本定位

`v0.0.4` 明确聚焦以下事项：

- 后端 release gate 与本地启动入口统一收敛到 reactor-safe 命令。
- 前端 targeted unit gate 已有独立脚本，同时保留完整单测基线。
- 系统账号密码已改为哈希存储与校验。
- Deploy 创建契约对外只承诺真实执行参数，不再承诺 `deployDir`。
- 资产中心当前只覆盖主机、凭据、分组、标签四类资产，不承诺数据库资源、数据库实例或数据库专用生命周期。
- Task Center 当前收敛为 deploy-only 队列视图。
- Traffic 页面与接口当前明确处于 skeleton / not-ready 边界，不再把占位能力包装成真实切流。

## 3. 仓库结构

```text
cmdb/
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # v0.0.2 / v0.0.3 / v0.0.4 版本化文档
├── release/     # 发布说明与核对清单
├── README.md
└── LICENSE
```

## 4. Quick Start

### 4.1 启动后端

```bash
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
ENVOPS_SERVER_PORT=18080 \
bash backend/scripts/run-envops-boot.sh
```

### 4.2 启动前端

```bash
pnpm --dir frontend install
pnpm --dir frontend dev
```

### 4.3 本地联调默认地址

- 后端：`http://127.0.0.1:18080`
- 前端：`http://localhost:9527`
- 前端 test 模式默认代理后端：`http://127.0.0.1:18080`

### 4.4 默认体验账号

- 用户名：`envops-admin`
- 密码：`EnvOps@123`
- 默认账号组合：`envops-admin / EnvOps@123`

## 5. 当前版本范围

### 5.1 资产中心

`v0.0.4` 的资产中心当前只覆盖四类资产：

- Host：提供列表与创建
- Credential：提供列表与创建
- Group：提供只读列表
- Tag：提供只读列表

当前前后端都没有数据库资源、数据库实例、数据库专用凭据、数据库连接串、数据库版本信息或数据库生命周期管理入口。`schema.sql` 中也只有 `asset_host`、`asset_credential`、`asset_group`、`asset_tag` 四类资产表，唯一出现 `database` 的地方只是示例标签文案，不代表数据库实例纳管。

### 5.2 Deploy

当前版本保留 Deploy 主链路的创建、审批、详情与执行相关能力，但对外契约只承诺真实执行参数，不再承诺 `deployDir`。所有对外说明都应以当前页面和接口中的真实参数为准。

### 5.3 Task Center

Task Center 在 `v0.0.4` 中明确是 deploy-only 队列视图：

- 用于查看 Deploy 任务列表、状态与摘要
- 支持从队列跳转到 Deploy 详情
- 不对外表述为跨域统一队列

### 5.4 Traffic

Traffic 在 `v0.0.4` 中必须按 not-ready 口径说明：

- 页面与接口用于明确 skeleton / not-ready 边界
- 页面应显示 not-ready warning
- 动作按钮保持禁用
- 当前不对外承诺 `preview` / `apply` / `rollback` 为可执行的真实切流能力

## 6. 本地验证建议

建议按以下命令验证 `v0.0.4` 基线：

- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

## 7. Deferred scope

以下内容明确继续保留在 `v0.0.4` 之外：

- Traffic 真实外部网关接通
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强

## 8. 文档同步要求

当前 `v0.0.4` 项目口径需要与以下材料保持一致：

- `README.md`
- `release/0.0.4-release-notes.md`
- `release/0.0.4-checklist.md`
