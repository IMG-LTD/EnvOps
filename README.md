# EnvOps

EnvOps 是一个面向资产管理、检测采集、应用建模、发布任务与流量控制场景的运维平台仓库。当前文档基于仓库当前基线，重点说明工程闸口、账号安全、本地联调默认配置，以及数据库真实连通性、Deploy / Task / Traffic 的对外口径。

## 当前基线概览

当前基线明确纳入说明与交付范围的内容包括：

- 后端 release gate 与本地启动入口统一收敛到 reactor-safe 脚本。
- 前端 targeted Vitest runner 与完整单测基线已恢复，可分别用于 release gate 与全量验证。
- 系统账号密码已改为哈希存储与校验，本地默认账号仍保留 `envops-admin / EnvOps@123`。
- 资产中心当前覆盖主机、凭据、分组、标签与数据库资源五类入口，其中数据库资源支持登记、筛选、编辑、删除、主机/凭据关联，以及真实数据库连通性检测。
- Deploy 创建表单对外只承诺真实执行参数，不再承诺 `deployDir`。
- Task Center 保留 v0.0.6 统一列表和轻量详情抽屉，并新增统一任务完整追踪页；统一纳入 Deploy、数据库连通性检测、Traffic 动作三类任务，状态口径为 `pending`、`running`、`success`、`failed`。
- Deploy 历史任务允许在完整追踪页降级展示；数据库连通性检测与 Traffic 动作只保证新增任务可完整追踪；数据库批量检测按一批一条记录。
- Traffic 页面与接口当前已收敛为最小真实切流 MVP，只覆盖 REST 插件、`weighted_routing` 策略，以及 `preview` / `apply` / `rollback` 真闭环。

## 仓库结构

```text
.
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # 说明文档与设计材料
├── release/     # 发布说明与核对清单
├── README.md    # 仓库首页
└── LICENSE
```

## 文档入口

当前文档入口包括：

- [项目详细说明](docs/envops-项目详细说明.md)
- [开发技术说明](docs/envops-开发技术说明.md)
- [用户操作手册](docs/envops-用户操作手册.md)
- [Release Notes](release/0.0.7-release-notes.md)

## 运行环境建议

- JDK 17
- Maven 3
- Node.js >= 20.19.0
- pnpm >= 10.5.0

## Quick Start

### 1. 启动后端

```bash
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
ENVOPS_SERVER_PORT=18080 \
bash backend/scripts/run-envops-boot.sh
```

说明：

- 默认后端地址为 `http://127.0.0.1:18080`
- 默认体验账号为 `envops-admin / EnvOps@123`
- 后端启动时会自动加载 `schema.sql` 与 `data.sql`

### 2. 启动前端

```bash
pnpm --dir frontend install
pnpm --dir frontend dev
```

说明：

- 默认前端地址为 `http://localhost:9527`
- 前端 test 模式默认代理到 `http://127.0.0.1:18080`

## 当前对外口径

1. **资产 / 检测 / 应用 / Deploy**
   - 保持当前版本的主要联调与演示能力。
   - 资产中心当前提供 Host、Credential、Group、Tag、Database 五类能力。
   - Host 与 Credential 支持创建和列表，Group 与 Tag 提供只读列表，Database 支持列表、筛选、创建、编辑、删除、主机/凭据关联，以及真实数据库连通性检测。
   - 当前数据库资源支持的主流类型为 `mysql`、`postgresql`、`oracle`、`sqlserver`、`mongodb`、`redis`。
   - 页面支持单个检测、批量检测已选、检测当前页、检测全部筛选结果。检测通过真实数据库连接与认证执行，成功回写 `online`，失败回写 `offline`，`warning` 仍保留为人工标记。
   - 数据库编辑表单支持维护 `connectionUsername` 与 `connectionPassword`。响应不回显密码，编辑时密码留空表示沿用已保存密文。
   - 当前只支持基础直连，不支持 SSL、连接串、MongoDB 副本集、Redis Sentinel / Cluster 等高级连接能力，也不包含数据库专用凭据体系或版本治理。
   - Deploy 创建契约聚焦真实执行参数，不再对外承诺 `deployDir`。

2. **统一任务中心**
   - 当前提供最小真实统一任务中心，保留 v0.0.6 统一列表和轻量详情抽屉。
   - 统一展示 Deploy、数据库连通性检测、Traffic 动作三类任务。
   - 统一状态口径为 `pending`、`running`、`success`、`failed`。
   - 支持任务类型、状态、开始时间范围、关键词筛选。
   - Task Center 在统一列表和轻量详情抽屉之外，新增统一任务完整追踪页，提供基础信息、状态时间线、日志摘要和原模块入口。
   - 用户可先通过轻量详情抽屉快速判断任务，再按需进入完整追踪页；更深领域信息继续跳转原模块日志或详情入口。
   - Deploy 历史任务允许降级展示；数据库连通性检测与 Traffic 动作只保证新任务完整追踪；数据库批量检测按一批一条记录。
   - Task Center 不在 v0.0.7 承担统一任务内重试、统一任务内取消、多任务编排或统一完整日志平台职责，也不补录数据库与 Traffic 全历史任务或引入新执行引擎抽象。

3. **Traffic**
   - 当前页面与接口已提供最小真实切流 MVP。
   - 当前只支持 `REST` 插件和 `weighted_routing` 策略。
   - `preview` / `apply` / `rollback` 都会真实调用外部 REST 流量服务；只有外部调用成功，才会更新策略状态与 `rollbackToken`。
   - 页面会明确展示支持范围、最近一次动作结果，以及不支持行的禁用原因。
   - 当前不扩展多插件、多策略矩阵、审批体系重构、批量切流、高级编排与灰度报表。

## 验证命令

推荐至少执行以下命令验证当前基线：

- `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
- `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectionSecretProtectorTest,DatabaseConnectivityServiceTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest test`
- `mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test`
- `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/views/task/shared/query.spec.ts src/views/asset/database-contract.spec.ts src/views/asset/database-connectivity.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

## Deferred scope

以下内容明确继续留在当前基线之外：

- Traffic 的多插件适配、多策略矩阵、审批体系重构、批量切流、高级编排与灰度报表
- Task Center 的统一任务内重试、统一任务内取消、多任务编排、统一完整日志平台、数据库与 Traffic 全历史补录，以及新执行引擎抽象
- Deploy 大规模主机检索与更深执行器增强

## License

- 仓库级 `LICENSE` 适用于当前公开的 EnvOps 仓库基线
- `frontend/` 的依赖与第三方许可证仍以各自上游许可条款为准
