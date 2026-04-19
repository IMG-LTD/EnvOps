# EnvOps v0.0.3

EnvOps 是一个面向资产管理、检测采集、应用建模、发布任务与流量控制场景的运维平台仓库。当前 `v0.0.3` 基线已经补齐资产主机纳管、检测任务创建与执行、发布任务创建与审批、系统用户创建与编辑、流量策略预览/应用/回滚等关键链路，并同步提供面向用户和开发者的版本化说明文档。

## 当前版本概览

当前版本可以明确对外说明的内容包括：

- 前后端分离的基础运行形态已经成型
- 动态路由登录流程已接入真实后端接口
- 资产中心已支持主机列表、主机纳管、凭据新增、分组与标签浏览
- 检测中心已支持检测任务列表、检测任务创建、手动执行和主机最新事实快照查询
- 应用中心已支持应用定义、版本、安装包、配置模板、脚本模板的增删改查
- `deploy/task` 页面已支持列表、创建、审批通过/驳回、详情、主机明细、执行日志，以及 `execute` / `retry` / `rollback` / `cancel`
- `traffic/controller` 页面已支持策略列表、预览、应用、回滚，并对接真实后端动作接口
- `system/user` 页面已支持真实列表、新建用户、编辑用户、角色绑定、状态与登录方式维护
- 已补充 `v0.0.3` 项目说明、开发说明、用户手册与版本变更记录

## 仓库结构

```text
.
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # v0.0.2 / v0.0.3 项目文档
├── release/     # 发布说明与版本变更记录
├── README.md    # 仓库首页
└── LICENSE
```

## 核心能力

当前版本已具备的主要能力包括：

1. **资产中心**
   - 主机列表查询
   - 主机纳管
   - 凭据列表查询与新增
   - 分组与标签浏览

2. **检测中心**
   - 检测任务列表查询
   - 检测任务创建
   - 检测任务手动执行
   - 主机最新事实快照查询

3. **应用中心**
   - 应用定义增删改查
   - 版本增删改查
   - 安装包上传与删除
   - 配置模板与脚本模板增删改查

4. **安装发布与任务中心**
   - 发布任务列表与详情
   - 发布任务创建
   - 发布任务审批通过/驳回
   - 主机明细与执行日志
   - 任务执行、重试、回滚、取消
   - 任务中心聚合查询

5. **流量控制与插件目录**
   - 流量策略列表
   - 流量插件目录
   - 策略预览、应用、回滚
   - 最新动作结果反馈
   - 当前 NGINX / REST plugin 仍为 skeleton adapter

6. **系统管理**
   - 系统用户列表查询
   - 新建用户
   - 编辑用户
   - 角色、状态、登录方式维护

## Plugin 与执行器说明

当前版本里有两类容易混淆的扩展点：

1. **TrafficPlugin**
   - 作用：处理流量策略相关动作
   - 能力边界：`preview` / `apply` / `rollback`
   - 当前现状：`NGINX` 与 `REST` plugin 都已出现在目录里，但 `pluginStatus` 仍是 `NOT_IMPLEMENTED`
   - 解释：`supportsPreview` / `supportsApply` / `supportsRollback` 表示接口能力位，`pluginStatus` 才表示实现就绪度

2. **RemoteExecutor**
   - 作用：处理发布任务侧的远程执行
   - 能力边界：`exec` / `upload` / `detect`
   - 当前现状：`SshRemoteExecutor` 状态是 `READY`，`exec` / `upload` 已走 SSH 适配，`detect` 仍返回适配器级结果

一句话区分就是：**TrafficPlugin 负责流量动作，RemoteExecutor 负责发布执行，它们不是同一个扩展点。**

## 文档入口

当前仓库已提供以下 `v0.0.3` 文档：

- [项目详细说明](docs/envops-v0.0.3-项目详细说明.md)
- [开发技术说明](docs/envops-v0.0.3-开发技术说明.md)
- [用户操作手册](docs/envops-v0.0.3-用户操作手册.md)
- [版本变更记录](release/0.0.3-release-notes.md)

历史 `v0.0.2` 文档仍保留在 `docs/` 与 `release/` 下，便于对比版本演进。

如果你想快速了解当前版本，建议先读：

1. 本 README
2. `docs/envops-v0.0.3-项目详细说明.md`
3. 再根据身份选择开发说明或用户手册

## 运行环境建议

- JDK 17
- Maven 3
- Node.js >= 20.19.0
- pnpm >= 10.5.0

## Quick Start

### 1. 启动后端

```bash
cd backend
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
mvn -pl envops-boot spring-boot:run
```

说明：

- 默认后端地址为 `http://127.0.0.1:8080`
- 两个安全相关环境变量都要求至少 32 个字符
- 后端启动时会自动加载 `schema.sql` 与 `data.sql`

### 2. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

说明：

- 默认前端地址为 `http://localhost:9527`
- 本地测试环境下，前端默认请求后端地址 `http://127.0.0.1:8080`
- `frontend/.env.prod` 中 `VITE_SERVICE_BASE_URL` 当前仍为空，生产部署前需明确 API 接入地址

### 3. 使用默认体验账号登录

- 用户名：`envops-admin`
- 密码：`EnvOps@123`

## 默认种子数据

本地启动后，可直接体验以下种子数据：

- 默认管理员账号：`envops-admin`
- 默认系统用户样例：4 个
- 示例发布任务：`DT202604151600000001`
- 流量策略示例：3 条
- 主机、检测任务、应用、安装包、模板、版本等基础演示数据

这些数据适用于本地体验与联调环境。如果用于更长期的共享环境，建议自行替换默认账号、密码和密钥。

## 测试与验证

推荐按以下命令验证当前版本：

- 后端测试：`cd backend && mvn test`
- 前端单测：`cd frontend && pnpm test:unit`
- 前端类型检查：`cd frontend && pnpm typecheck`
- 前端代码检查：`cd frontend && pnpm lint`

如果只想快速验证本轮补齐的关键链路，建议至少覆盖：

- `cd frontend && pnpm exec vitest run src/views/monitor/monitor-contract.spec.ts`
- `cd frontend && pnpm exec vitest run src/views/traffic/traffic-contract.spec.ts`
- `cd frontend && pnpm exec vitest run src/views/system/user-contract.spec.ts`
- `cd frontend && pnpm typecheck`
- `cd frontend && pnpm lint`
- `cd backend && mvn -pl envops-boot -am -Dtest=AssetControllerTest,TrafficControllerTest,UserControllerTest test`

当前版本至少可以验证以下路径：

1. 本地启动前后端
2. 使用默认账号登录
3. 打开 `asset/host` 页面新增一台主机，并验证列表刷新
4. 打开 `monitor/detect-task` 页面新增检测任务并执行
5. 打开 `deploy/task` 页面创建任务，按状态执行审批通过或驳回，再查看详情和日志
6. 打开 `system/user` 页面新增或编辑用户
7. 打开 `traffic/controller` 页面执行 `preview` / `apply` / `rollback`

## 当前限制

当前 `v0.0.3` 仍有以下边界：

- `home` 仍以平台概览页为主
- `envops-exec` 仍是模块骨架
- 流量插件当前返回的是 skeleton 结果，适合联调和契约验证，不是完整生产流量切换能力
- `supports*` 代表动作能力位，不代表真实外部系统已接通，插件是否真正可用要看 `pluginStatus`
- `SshRemoteExecutor` 当前的 `detect` 仍是适配器级返回，不是完整探测编排链路
- `system/user` 当前聚焦创建与编辑，还不是完整用户生命周期后台
- `frontend/.env.prod` 的 `VITE_SERVICE_BASE_URL` 仍待生产环境补齐

## Roadmap

后续建议优先推进：

- 推进流量插件从 skeleton 结果接入真实执行链路
- 推进执行器与生产化能力完善
- 继续补齐系统用户更完整的生命周期动作与审计能力
- 补齐更正式的安全存储、部署配置与环境治理能力
- 持续保持 `README.md`、`docs/` 与 `release/` 三处文档同步

## License

- 仓库级 `LICENSE` 适用于当前公开的 EnvOps 仓库基线
- `frontend/` 的依赖与第三方许可证仍以各自上游许可条款为准
