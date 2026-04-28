# EnvOps 开发技术说明

## 1. 文档定位

本文档面向开发、联调、测试与维护人员，说明当前基线的工程版本字段、本地联调方式、验证命令，以及 Deploy / Task / Traffic / RBAC 的技术边界，其中 Task Center 在统一列表和轻量详情抽屉之外新增统一任务完整追踪页，Traffic 已收敛到最小真实能力口径，RBAC 新增第一版固定权限点和角色授权模型。

## 2. 版本字段

当前基线的工程版本字段与发布材料口径如下：

- `backend/pom.xml` 当前包含 `<version>0.0.4-SNAPSHOT</version>`
- `frontend/package.json` 当前包含 `"version": "0.0.4"`
- 当前功能说明与发布材料按 `release/0.0.8-release-notes.md` 的 RBAC 权限管理口径同步，并保留 `release/0.0.7-release-notes.md` 的统一任务追踪口径

## 3. 本地联调方式

### 3.1 启动后端

```bash
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
ENVOPS_SERVER_PORT=18080 \
bash backend/scripts/run-envops-boot.sh
```

### 3.2 启动前端

```bash
pnpm --dir frontend install
pnpm --dir frontend dev
```

### 3.3 默认联调关系

- 后端地址：`http://127.0.0.1:18080`
- 前端地址：`http://localhost:9527`
- 前端 test 模式代理后端：`http://127.0.0.1:18080`
- 默认账号：`envops-admin / EnvOps@123`

## 4. 工程闸口与验证命令

当前基线统一使用以下命令作为工程闸口与本地验证入口：

- `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
- `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectionSecretProtectorTest,DatabaseConnectivityServiceTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AuthRouteControllerTest,UserControllerTest,RbacControllerTest,RbacApiAuthorizationTest,RbacRegistryCoverageTest test`
- `mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test`
- `mvn -f backend/pom.xml test`
- `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/views/task/shared/query.spec.ts src/views/asset/database-contract.spec.ts src/views/asset/database-connectivity.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts`
- `pnpm --dir frontend exec vitest run src/views/system/rbac-contract.spec.ts src/views/system/user-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

其中：

- 后端启动入口按 reactor-safe 脚本执行
- 前端 targeted unit gate 已有独立脚本，不再串跑无关 spec
- 完整 `pnpm --dir frontend test:unit` 仍作为全量单测基线

## 5. 模块口径与边界

### 5.1 Asset

当前基线的资产模块前后端已实现 `/api/assets/hosts`、`/api/assets/credentials`、`/api/assets/groups`、`/api/assets/tags`、`/api/assets/databases` 五类入口，其中数据库资源已具备页面、路由、类型、表结构和契约测试。

数据库资源模块当前真实技术边界如下：

- 数据类型枚举：`mysql`、`postgresql`、`oracle`、`sqlserver`、`mongodb`、`redis`
- 纳管状态枚举：`managed`、`disabled`
- 连通性状态枚举：`unknown`、`online`、`warning`、`offline`
- 前端页面支持列表、筛选、创建、编辑、删除，以及主机/凭据依赖加载
- 当前支持页面发起单个检测、批量检测已选、检测当前页、检测全部筛选结果
- 后端接口覆盖 `/api/assets/databases/{id}/connectivity-check`、`/api/assets/databases/connectivity-check:selected`、`/api/assets/databases/connectivity-check:page`、`/api/assets/databases/connectivity-check:query`
- 检测通过真实数据库连接和认证执行，成功写 `online`，失败写 `offline`
- `warning` 仍为人工维护状态，不由检测流程自动写入
- 数据库连接密码以可逆密文存储在 `asset_database.connection_password`，响应不回显密码，编辑时密码留空表示沿用已保存密文
- 当前只支持基础直连，不支持 SSL、连接串、副本集、Sentinel、Cluster 等高级能力
- 当前不包含数据库版本发现、数据库专用凭据模型或数据库专用生命周期工作流

### 5.2 Deploy

Deploy 创建契约当前只承诺真实执行参数，不再对外承诺 `deployDir`。开发和联调说明必须与当前页面、接口和 release materials 保持一致。

### 5.3 Task Center

Task Center 当前在统一列表和轻量详情抽屉之外新增统一任务完整追踪页：

- 统一纳入 `deploy`、`database_connectivity`、`traffic_action` 三类任务
- 统一状态口径：`pending`、`running`、`success`、`failed`
- 统一列表字段以任务类型、任务名、状态、发起人、开始时间、结束时间、摘要、源路由为主
- 前端保留“统一列表 + 轻量详情抽屉”，用户先用轻量详情抽屉快速判断任务，再进入完整追踪页
- 完整追踪接口为 `GET /api/task-center/tasks/{id}/tracking`
- 统一追踪由 envops-task 读侧 application service 组装，按任务类型拆分 Deploy、Database Connectivity、Traffic Action assembler。assembler 只组装追踪视图，不执行任务，不写业务状态
- 返回信息范围限定为 `basicInfo`、`timeline`、`logSummary`、`logRoute` / `detailPreview` / `sourceLinks`、`degraded`
- Deploy 历史任务允许降级展示；数据库连通性检测与 Traffic 动作只保证新任务完整追踪
- 数据库批量检测按一批一条记录，不拆成多条子任务投影
- 数据库与 Traffic 原模块入口当前分别回到 `/asset/database`、`/traffic/controller` 页面级入口；Deploy 原模块入口回到 `/deploy/task?taskId=...`
- 禁止在统一追踪响应中暴露或扩展 `retryable`、`cancelable`、`children` 等统一重试、取消、编排语义
- 当前范围不新增统一任务内重试、统一任务内取消、多任务编排、统一完整日志平台、数据库与 Traffic 全历史补录或新执行引擎抽象

### 5.4 Traffic

Traffic 当前技术边界已收敛为最小真实能力：

- 插件范围只覆盖 `REST`
- 策略范围只覆盖 `weighted_routing`
- 后端通过真实 REST 客户端调用外部流量服务执行 `preview`、`apply`、`rollback`
- 外部调用失败时返回错误，不回写成功状态；外部服务不可用时统一映射为 `502 Bad Gateway`
- `apply` 成功路径要求外部服务返回 `rollbackToken`，否则视为失败
- `rollback` 只在记录已有 `rollbackToken` 时可执行
- 页面只对 REST + weighted routing + 插件就绪的记录开放动作按钮，其余记录展示禁用原因
- 当前不纳入多插件适配、多策略矩阵、审批重构、批量切流、高级编排与灰度报表

### 5.5 RBAC 架构与 API

RBAC 权限管理采用固定权限点 + 角色绑定模型，当前技术口径如下：

- 权限点固定写入 `sys_permission`，由系统种子数据维护，不提供 UI 创建任意 API matcher
- 角色到权限的绑定写入 `sys_role_permission`，用户到角色的绑定写入 `sys_user_role`
- 用户有效权限来自已启用角色和已启用权限点的交集
- 后端通过集中 API 授权注册表维护接口与菜单/操作权限的映射，并以该注册表作为 API 授权的权威判断
- 动态路由按菜单权限过滤；菜单权限同时控制模块读 API 的访问
- 用户信息接口返回操作权限编码，前端据此做按钮禁用和提示等 UX gating；后端 API 授权仍是安全边界
- Home、Asset、Monitor、App、Deploy、Task Center、Traffic、System 全模块均纳入菜单 + 操作权限模型

RBAC 管理 API 包括：

- `GET /api/system/rbac/roles`
- `POST /api/system/rbac/roles`
- `PUT /api/system/rbac/roles/{id}`
- `GET /api/system/rbac/permissions`
- `GET /api/system/rbac/roles/{id}/permissions`
- `PUT /api/system/rbac/roles/{id}/permissions`
- `GET /api/system/users/{id}/roles`
- `PUT /api/system/users/{id}/roles`

RBAC 不在 v0.0.8 提供 UI 创建任意 API matcher、组织架构、部门继承、审批流、审计中心、资源级归属权限或 JWT 登录模型替换。

## 6. 账号与安全基线

- 本地默认账号仍为 `envops-admin / EnvOps@123`
- 系统账号密码已改为哈希存储与校验
- 启动后端时必须显式提供 `ENVOPS_SECURITY_TOKEN_SECRET`、`ENVOPS_CREDENTIAL_PROTECTION_SECRET` 与 `ENVOPS_SERVER_PORT`

## 7. Deferred scope

以下内容继续明确排除在当前基线之外：

- Traffic 的多插件适配、多策略矩阵、审批重构、批量切流、高级编排与灰度报表
- Task Center 的统一任务内重试、统一任务内取消、多任务编排、统一完整日志平台、数据库与 Traffic 全历史补录，以及新执行引擎抽象
- Deploy 大规模主机检索与更深执行器增强
- RBAC 不在 v0.0.8 提供 UI 创建任意 API matcher、组织架构、部门继承、审批流、审计中心、资源级归属权限或 JWT 登录模型替换
