# EnvOps 项目详细说明

## 1. 文档说明

本文档用于说明当前仓库当前基线的项目定位、交付范围、本地联调方式与能力边界。当前阶段重点不是继续扩张功能承诺，而是完成 release gate 修复，统一工程闸口、账号安全、本地默认配置，以及 Deploy / Task / Traffic 的对外口径。

## 2. 版本定位

当前基线明确聚焦以下事项：

- 后端 release gate 与本地启动入口统一收敛到 reactor-safe 命令。
- 前端 targeted unit gate 已有独立脚本，同时保留完整单测基线。
- 系统账号密码已改为哈希存储与校验。
- Deploy 创建契约对外只承诺真实执行参数，不再承诺 `deployDir`。
- 资产中心当前覆盖主机、凭据、分组、标签、数据库资源五类资产，其中数据库范围已支持目录登记、关系维护与真实连通性检测，但仍不承诺版本治理或数据库专用生命周期。
- Task Center 当前收敛为 deploy-only 队列视图。
- Traffic 页面与接口当前明确处于 skeleton / not-ready 边界，不再把占位能力包装成真实切流。

## 3. 仓库结构

```text
cmdb/
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # 说明文档与设计材料
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

当前基线的资产中心覆盖五类资产：

- Host：提供列表与创建
- Credential：提供列表与创建
- Group：提供只读列表
- Tag：提供只读列表
- Database：提供列表、筛选、创建、编辑、删除、主机/通用凭据关联，以及真实数据库连通性检测

数据库资源当前落地的是“资产目录 + 最小可用真实探测”能力，不是完整数据库平台能力。当前真实边界如下：

- 已支持的主流数据库类型：`mysql`、`postgresql`、`oracle`、`sqlserver`、`mongodb`、`redis`
- 已支持的状态字段：纳管状态 `managed / disabled`，连通性状态 `unknown / online / warning / offline`
- 已支持的字段：数据库名、类型、环境、所属主机、端口、实例名、关联凭据、归属团队、纳管状态、连通性状态、连接用户名、最近检查时间、说明
- 页面支持单个检测、批量检测已选、检测当前页、检测全部筛选结果
- 检测通过真实数据库连接与认证执行，成功回写 `online`，失败回写 `offline`，缺少连接用户名或密码的记录会跳过并返回原因
- `warning` 仍为人工维护状态，不由检测流程自动写入
- 响应不回显连接密码，编辑时密码留空表示沿用已保存密文
- 当前只支持基础直连，不支持 SSL、连接串、MongoDB 副本集、Redis Sentinel / Cluster 等高级连接能力
- 当前不包含数据库专用凭据体系、数据库版本采集、自动巡检任务或数据库专用生命周期编排

### 5.2 Deploy

当前版本保留 Deploy 主链路的创建、审批、详情与执行相关能力，但对外契约只承诺真实执行参数，不再承诺 `deployDir`。所有对外说明都应以当前页面和接口中的真实参数为准。

### 5.3 Task Center

Task Center 当前明确是 deploy-only 队列视图：

- 用于查看 Deploy 任务列表、状态与摘要
- 支持从队列跳转到 Deploy 详情
- 不对外表述为跨域统一队列

### 5.4 Traffic

Traffic 当前必须按 not-ready 口径说明：

- 页面与接口用于明确 skeleton / not-ready 边界
- 页面应显示 not-ready warning
- 动作按钮保持禁用
- 当前不对外承诺 `preview` / `apply` / `rollback` 为可执行的真实切流能力

## 6. 本地验证建议

建议按以下命令验证当前基线：

- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

## 7. Deferred scope

以下内容明确继续保留在当前基线之外：

- Traffic 真实外部网关接通
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强

## 8. 文档同步要求

当前项目口径需要与以下材料保持一致：

- `README.md`
- `release/0.0.4-release-notes.md`
- `release/0.0.4-checklist.md`
