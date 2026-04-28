# EnvOps 项目详细说明

## 1. 文档说明

本文档用于说明当前仓库当前基线的项目定位、交付范围、本地联调方式与能力边界。当前阶段重点不是继续扩张功能承诺，而是同步工程闸口、账号安全、本地默认配置，以及 Database / Deploy / Task / Traffic / RBAC 的真实能力边界。

## 2. 当前基线定位

当前基线明确聚焦以下事项：

- 后端 release gate 与本地启动入口统一收敛到 reactor-safe 命令。
- 前端 targeted unit gate 已有独立脚本，同时保留完整单测基线。
- 系统账号密码已改为哈希存储与校验。
- Deploy 创建契约对外只承诺真实执行参数，不再承诺 `deployDir`。
- 资产中心当前覆盖主机、凭据、分组、标签、数据库资源五类资产，其中数据库范围已支持目录登记、关系维护与真实连通性检测，但仍不承诺版本治理或数据库专用生命周期。
- Task Center 当前保留统一列表和轻量详情抽屉，并新增统一任务完整追踪页，统一纳入 Deploy、数据库连通性检测、Traffic 动作三类任务。
- Deploy 历史任务允许在完整追踪页降级展示；数据库连通性检测与 Traffic 动作只保证新任务完整追踪；数据库批量检测按一批一条记录。
- Traffic 页面与接口当前已收敛为最小真实切流 MVP，只覆盖 REST 插件、`weighted_routing` 策略，以及 `preview` / `apply` / `rollback` 真闭环。
- RBAC 权限管理提供固定权限点、角色维护、角色权限分配和用户角色分配；后端 API 授权与动态菜单权限统一使用菜单 + 操作权限模型。

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

Task Center 当前在最小真实统一任务中心基础上提供两级查看流程：

- 统一纳入 `deploy`、`database_connectivity`、`traffic_action` 三类任务
- 统一状态口径为 `pending`、`running`、`success`、`failed`
- 保留统一任务列表和轻量详情抽屉
- 用户先在统一任务列表中打开轻量详情抽屉快速判断任务，再按需进入统一任务完整追踪页查看状态时间线、日志摘要和原模块入口
- 完整追踪页仅包含基础信息、状态时间线、日志摘要、原模块日志/详情入口
- Deploy 历史任务允许在完整追踪页降级展示
- 数据库连通性检测与 Traffic 动作只保证新任务完整追踪；数据库批量检测按一批一条记录
- Task Center 不承担统一任务内重试、统一任务内取消、多任务编排或统一完整日志平台职责，也不补录数据库与 Traffic 全历史任务或引入新执行引擎抽象

### 5.4 Traffic

Traffic 当前已收敛为最小真实切流 MVP，真实边界如下：

- 只支持一个插件通路：`REST`
- 只支持一个策略：`weighted_routing`
- `preview`、`apply`、`rollback` 三个动作都会真实调用外部 REST 流量服务
- 只有外部调用成功，EnvOps 才更新策略状态与 `rollbackToken`
- `apply` 要求外部服务返回可用的 `rollbackToken`，否则请求失败且不写入成功状态
- `rollback` 只对已有 `rollbackToken` 的记录开放
- 页面会展示范围 warning、最近一次动作结果，以及不支持记录的禁用原因
- 当前不扩展多插件、多策略矩阵、审批体系大改、批量切流、高级编排与灰度报表

### 5.5 RBAC 权限管理

RBAC 权限管理当前落地第一版，采用固定权限点、角色优先的权限维护方式：

- 权限点由系统固定种子数据提供，分为菜单权限和操作权限，不允许在 UI 中创建任意 API matcher
- 系统管理 / 权限管理提供角色优先的维护入口，可创建和编辑角色、启停角色，并维护角色菜单/操作权限
- 系统管理 / 用户管理新增用户-角色绑定，可为用户分配已启用角色
- Home、Asset、Monitor、App、Deploy、Task Center、Traffic、System 全模块均纳入菜单 + 操作权限模型
- 菜单权限控制菜单可见和模块读 API
- 操作权限控制创建、编辑、删除、执行、审批、应用、回滚、系统管理等高风险动作
- 后端 API 授权是权威判断和安全边界，前端按钮禁用和提示只作为用户体验
- RBAC 不在 v0.0.8 提供 UI 创建任意 API matcher、组织架构、部门继承、审批流、审计中心、资源级归属权限或 JWT 登录模型替换

## 6. 本地验证建议

建议按以下命令验证当前基线：

- `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
- `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectionSecretProtectorTest,DatabaseConnectivityServiceTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest test`
- `mvn -f backend/pom.xml -pl envops-boot -Dtest=AuthRouteControllerTest,UserControllerTest,RbacControllerTest,RbacApiAuthorizationTest,RbacRegistryCoverageTest test`
- `mvn -f backend/pom.xml test`
- `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

## 7. Deferred scope

以下内容明确继续保留在当前基线之外：

- Traffic 的多插件适配、多策略矩阵、审批体系重构、批量切流、高级编排与灰度报表
- Task Center 的统一任务内重试、统一任务内取消、多任务编排、统一完整日志平台、数据库与 Traffic 全历史补录，以及新执行引擎抽象
- Deploy 大规模主机检索与更深执行器增强
- RBAC 不在 v0.0.8 提供 UI 创建任意 API matcher、组织架构、部门继承、审批流、审计中心、资源级归属权限或 JWT 登录模型替换

## 8. 文档同步要求

当前项目口径需要与以下材料保持一致：

- `README.md`
- `release/0.0.8-release-notes.md`
- `release/0.0.7-release-notes.md`
