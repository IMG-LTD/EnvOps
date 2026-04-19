# EnvOps / CMDB v0.0.3 项目详细说明

## 1. 文档说明

本文档用于整理当前仓库 `v0.0.3` 基线的项目资料，覆盖以下内容：

- 项目定位与版本范围
- 仓库结构与前后端代码说明
- 当前可用功能与页面落地情况
- 用户侧主要使用路径
- 开发、联调与本地启动方式
- 当前版本边界与已知注意事项

本文档以当前仓库中可直接验证的代码与配置为准，重点对应已经纳入版本基线的：

- `frontend/`
- `backend/`
- `release/`
- `docs/`

> 说明：本文档与仓库根 `README.md`、`release/0.0.3-release-notes.md` 已统一到 `v0.0.3` 口径，后续如调整发布边界，请同步维护三处文档。

---

## 2. 项目定位

EnvOps / CMDB 不是单点工具，而是围绕运维平台核心闭环建设的一套前后端项目：

`主机纳管 -> 检测采集 -> 应用建模 -> 版本与制品 -> 发布任务 -> 任务跟踪 -> 流量控制/回滚`

从当前 `v0.0.3` 代码可确认的目标包括：

1. 统一管理主机、分组、标签、凭据等资产资料。
2. 支撑检测任务的创建、执行和主机事实快照查询。
3. 支撑应用定义、版本、安装包、配置模板、脚本模板等应用建模能力。
4. 支撑发布任务的创建、审批、查询、详情、执行、重试、回滚、取消，以及任务中心聚合查询。
5. 支撑流量策略目录、插件目录和策略预览、应用、回滚动作。
6. 支撑系统用户列表、新建、编辑与角色绑定。
7. 为执行器目录与更完整的系统管理扩展保留后续接口。

---

## 3. 当前仓库结构

当前 `v0.0.3` 基线下，仓库主结构为：

```text
cmdb/
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # 项目文档
├── release/     # 发布说明与版本变更记录
├── README.md
└── LICENSE
```

当前版本中，`backend/` 与 `frontend/` 都已经是主工作区中的正式代码，`docs/` 与 `release/` 负责承接版本说明与交付口径。

---

## 4. 前端代码说明

## 4.1 前端技术栈

`frontend/` 当前采用：

- Vue 3
- TypeScript 6
- Vite 8
- Pinia 3
- Vue Router 5
- Naive UI
- UnoCSS
- vue-i18n
- Vitest

主要命令包括：

```bash
cd frontend
pnpm install
pnpm dev
pnpm build
pnpm test:unit
pnpm typecheck
pnpm lint
```

其中：

- `pnpm dev` 实际执行 `vite --mode test`
- 开发端口默认是 `9527`
- `preview` 端口默认是 `9725`

## 4.2 前端关键目录

`frontend/` 下与业务最相关的目录包括：

- `src/main.ts`：应用启动入口，负责加载 store、router、i18n、通知等初始化逻辑
- `src/router/`：路由生成、守卫、权限加载
- `src/store/modules/`：Pinia 状态模块，核心包括 `auth`、`route`、`tab`、`theme`
- `src/service/api/`：前端 API 封装
- `src/service/request/`：请求层封装、鉴权头注入、统一错误处理
- `src/views/`：具体页面实现
- `src/layouts/`：控制台布局与导航壳层
- `src/locales/`：中英文文案与页面文案

## 4.3 登录、鉴权与路由

前端当前已经对接真实后端契约：

- 登录接口：`POST /api/auth/login`
- 用户信息接口：`GET /api/auth/getUserInfo`
- 常量路由接口：`GET /api/routes/getConstantRoutes`
- 用户路由接口：`GET /api/routes/getUserRoutes`

当前 `.env` 中已启用：

- `VITE_AUTH_ROUTE_MODE=dynamic`
- `VITE_ROUTER_HISTORY_MODE=history`
- `VITE_HTTP_PROXY=Y`
- `VITE_SERVICE_SUCCESS_CODE=0000`

`.env.test` 默认指向本地后端：

- `VITE_SERVICE_BASE_URL=http://127.0.0.1:8080`

`.env.prod` 中 `VITE_SERVICE_BASE_URL` 仍为空，说明生产接入地址仍需在部署前明确。

## 4.4 请求层说明

请求层统一做了以下事情：

- 读取环境变量中的服务地址
- 自动携带 `Authorization` 头
- 按统一返回码 `0000` 判断成功
- 统一处理后端错误信息
- 处理登录态失效和 401 场景

因此，前后端联调时的核心契约已经比较清晰：

```json
{
  "code": "0000",
  "msg": "success",
  "data": {}
}
```

## 4.5 当前页面与功能落地情况

从 `frontend/src/router/elegant/routes.ts` 和各页面实现可确认，当前已经落地的主要模块包括：

- 首页 `home`
- 资产中心 `asset`
- 检测中心 `monitor`
- 应用中心 `app`
- 安装发布 `deploy/task`
- 任务中心 `task/center`
- 流量控制 `traffic/controller`
- 系统管理 `system/user`

其中实际对接后端接口、具备业务数据交互的模块主要是：

1. **资产中心**
   - 主机列表查询
   - 主机纳管
   - 凭据列表查询与新增
   - 分组列表查询
   - 标签列表查询
   - 已有监控快照主机可直接跳转指标详情

2. **检测中心**
   - 检测任务列表查询
   - 检测任务创建
   - 检测任务手动执行
   - 主机最新事实快照查询

3. **应用中心**
   - 应用定义增删改查
   - 版本增删改查
   - 安装包上传与删除
   - 配置模板增删改查
   - 脚本模板增删改查

4. **安装发布 / 任务中心**
   - 发布任务列表
   - 发布任务创建
   - 发布任务审批通过 / 驳回
   - 发布任务详情
   - 主机明细
   - 执行日志
   - `execute` / `retry` / `rollback` / `cancel`
   - 任务中心聚合列表与跳转详情

5. **流量控制**
   - 流量策略列表
   - 插件目录能力映射
   - `preview` / `apply` / `rollback` 三个动作入口
   - 最近一次动作结果反馈

6. **系统管理**
   - 系统用户列表查询
   - 新建用户
   - 编辑用户
   - 角色、状态、登录方式维护

## 4.6 前端当前边界

当前仍应区分“已接真实接口的业务页”和“控制台壳层/演示页”：

- `asset`、`monitor`、`app`、`deploy/task`、`task/center`、`system/user` 已有真实接口对接
- `traffic/controller` 已接入真实动作接口，但插件执行结果当前仍为 skeleton 反馈，更适合联调与演示
- `home` 当前仍以平台概览展示为主
- 登录页中验证码登录仍是占位入口

---

## 5. 后端代码说明

## 5.1 后端技术栈

`backend/` 是一个 Maven 多模块 Spring Boot 项目，当前采用：

- Java 17
- Spring Boot 3.3.6
- Maven 多模块
- MyBatis
- Spring Security
- H2 内存数据库
- JUnit 5 / MockMvc

启动命令：

```bash
cd backend
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
mvn -pl envops-boot spring-boot:run
```

测试命令：

```bash
cd backend
mvn test
```

> 两个安全相关环境变量都要求至少 32 个字符，否则应用无法启动。

## 5.2 后端模块结构

`backend/pom.xml` 当前声明了以下模块：

- `envops-boot`
- `envops-common`
- `envops-framework`
- `envops-system`
- `envops-asset`
- `envops-monitor`
- `envops-app`
- `envops-deploy`
- `envops-exec`
- `envops-traffic`

职责可以概括为：

- `envops-boot`：应用启动入口与基础装配
- `envops-common`：统一响应、异常、基础对象
- `envops-framework`：安全配置、Token 服务、全局基础设施
- `envops-system`：登录鉴权、用户信息、动态路由、系统用户管理
- `envops-asset`：主机、凭据、分组、标签
- `envops-monitor`：检测任务与事实快照
- `envops-app`：应用、版本、包、配置模板、脚本模板
- `envops-deploy`：发布任务、执行器目录、任务中心、日志与主机明细
- `envops-exec`：当前仅保留模块骨架
- `envops-traffic`：流量策略、插件目录与动作接口

## 5.3 启动方式与数据初始化

`envops-boot` 是实际启动入口。当前 `application.yml` 配置表明：

- 默认使用 H2 内存库
- 启动时自动加载 `schema.sql`
- 启动时自动加载 `data.sql`

这意味着本地启动后即可获得演示所需的基础数据。

## 5.4 统一响应与安全模型

后端统一响应对象为：

```json
{
  "code": "0000",
  "msg": "success",
  "data": {}
}
```

安全模型的关键点包括：

- 放行 `POST /api/auth/login`
- 放行 `GET /api/routes/getConstantRoutes`
- 其余 `/api/**` 默认需要鉴权
- `system/user`、应用、安装包、模板等接口要求 `SUPER_ADMIN`

Token 机制是自研的 HMAC 风格令牌，而不是标准第三方 JWT 组件。

## 5.5 当前后端接口分布

当前已可明确识别的接口分布如下。

### 系统与路由

- `POST /api/auth/login`
- `GET /api/auth/getUserInfo`
- `GET /api/routes/getConstantRoutes`
- `GET /api/routes/getUserRoutes`
- `GET /api/system/users`
- `POST /api/system/users`
- `PUT /api/system/users/{id}`

### 资产中心

- `GET /api/assets/hosts`
- `POST /api/assets/hosts`
- `GET /api/assets/credentials`
- `POST /api/assets/credentials`
- `GET /api/assets/groups`
- `GET /api/assets/tags`

### 检测中心

- `POST /api/monitor/detect-tasks`
- `GET /api/monitor/detect-tasks`
- `POST /api/monitor/detect-tasks/{id}/execute`
- `GET /api/monitor/hosts/{hostId}/facts/latest`

### 应用中心

- `GET /api/apps`
- `GET /api/apps/{id}`
- `POST /api/apps`
- `PUT /api/apps/{id}`
- `DELETE /api/apps/{id}`
- `GET /api/apps/{id}/versions`
- `POST /api/apps/{id}/versions`
- `PUT /api/app-versions/{id}`
- `DELETE /api/app-versions/{id}`
- `GET /api/packages`
- `POST /api/packages/upload`
- `DELETE /api/packages/{id}`
- `GET /api/config-templates`
- `POST /api/config-templates`
- `PUT /api/config-templates/{id}`
- `DELETE /api/config-templates/{id}`
- `GET /api/script-templates`
- `POST /api/script-templates`
- `PUT /api/script-templates/{id}`
- `DELETE /api/script-templates/{id}`

### 发布与任务中心

- `GET /api/deploy/tasks`
- `GET /api/deploy/tasks/{id}`
- `POST /api/deploy/tasks`
- `POST /api/deploy/tasks/{id}/approve`
- `POST /api/deploy/tasks/{id}/reject`
- `POST /api/deploy/tasks/{id}/execute`
- `POST /api/deploy/tasks/{id}/cancel`
- `POST /api/deploy/tasks/{id}/retry`
- `POST /api/deploy/tasks/{id}/rollback`
- `GET /api/deploy/tasks/{id}/hosts`
- `GET /api/deploy/tasks/{id}/logs`
- `GET /api/task-center/tasks`
- `GET /api/deploy/executors`

### 流量控制

- `GET /api/traffic/policies`
- `GET /api/traffic/plugins`
- `POST /api/traffic/policies/{id}/preview`
- `POST /api/traffic/policies/{id}/apply`
- `POST /api/traffic/policies/{id}/rollback`

## 5.6 Plugin 与执行器说明

当前版本需要明确区分两类扩展点。

### TrafficPlugin

`TrafficPlugin` 是流量策略动作接口，当前真实 contract 包括：

- `pluginType()`
- `pluginName()`
- `pluginStatus()`
- `supportsPreview()`
- `supportsApply()`
- `supportsRollback()`
- `preview(...)`
- `apply(...)`
- `rollback(...)`

当前内置实现有：

- `NginxTrafficPlugin`
- `RestTrafficPlugin`

两者都已经注册到 Spring 容器，但 `pluginStatus` 都是 `NOT_IMPLEMENTED`。这意味着：

- `supports*` 用来表达动作能力位
- `pluginStatus` 用来表达实现就绪度
- 当前它们能返回契约化结果，能驱动页面和接口联调
- 当前它们还不是已接通真实网关或外部流量系统的生产插件

### RemoteExecutor

`RemoteExecutor` 是发布任务侧远程执行接口，当前 contract 包括：

- `executorType()`
- `executorName()`
- `executorStatus()`
- `exec(...)`
- `upload(...)`
- `detect(...)`

当前 `SshRemoteExecutor` 状态是 `READY`，其中：

- `exec` 和 `upload` 已走 SSH 适配执行
- `detect` 当前仍返回适配器级检测反馈

这两类扩展点的边界必须分清：

- `TrafficPlugin` 负责流量动作
- `RemoteExecutor` 负责发布执行

它们不是一个体系，也不应该在文档中混写成同一类 plugin。

## 5.7 当前后端边界

后端虽然已经进入主工作区，但仍要注意：

- `envops-exec` 当前只有模块骨架，尚未形成独立业务实现
- `envops-traffic` 已具备真实策略状态变更和动作接口，但插件结果当前仍为 skeleton 响应
- 登录逻辑当前仍是明文密码比对，更适合作为本地演示和联调基线，而不是最终生产安全方案
- 系统用户管理当前支持列表、新建、编辑，但还未覆盖删除、禁用审计、密码策略等更完整后台能力

---

## 6. 默认数据与体验信息

当前 `data.sql` 已提供本地体验所需的种子数据，包括：

- 默认账号：`envops-admin`
- 默认密码：`EnvOps@123`
- 默认角色：`SUPER_ADMIN`
- 4 个系统用户样例
- 4 台主机演示数据
- 2 个检测任务
- 2 个应用
- 2 个安装包
- 2 个配置模板
- 2 个脚本模板
- 2 个应用版本
- 1 个示例发布任务：`DT202604151600000001`
- 3 条流量策略示例数据

因此，本地启动后可以直接体验：

- 登录
- 动态菜单加载
- 主机纳管与凭据新增
- 检测任务创建、执行和主机快照查看
- 应用、版本、包、模板维护
- 发布任务创建、审批、详情、主机明细、执行日志查看
- 系统用户创建与编辑
- 流量策略预览、应用、回滚

---

## 7. 用户使用说明概览

站在最终使用者角度，当前版本可以按以下路径理解：

1. 登录系统
2. 在资产中心纳管主机、查看凭据和资产基础数据
3. 在检测中心创建并执行检测任务，查看主机快照
4. 在应用中心维护应用、版本、安装包和模板
5. 在安装发布中创建任务、进行审批、查看详情、主机明细和日志，并对可执行任务进行执行、重试、回滚、取消
6. 在任务中心统一查看任务状态与处理风险
7. 在系统管理中新增或编辑系统用户
8. 在流量控制中查看策略并执行 `preview` / `apply` / `rollback`

更完整的操作步骤见 `docs/envops-v0.0.3-用户操作手册.md`。

---

## 8. 典型业务操作流程

## 8.1 登录与菜单加载流程

1. 访问前端地址 `http://localhost:9527`
2. 输入 `envops-admin / EnvOps@123`
3. 前端调用 `/api/auth/login`
4. 成功后保存 token
5. 前端调用 `/api/auth/getUserInfo`
6. 前端调用 `/api/routes/getConstantRoutes` 与 `/api/routes/getUserRoutes`
7. 动态生成菜单并进入控制台

## 8.2 主机纳管与检测联动流程

1. 进入“资产中心 -> 主机管理”查看主机列表
2. 在左侧表单录入主机名、IP、环境、集群、归属团队和状态
3. 提交后刷新主机列表，确认主机已纳管
4. 进入“检测中心 -> 即时检测”新增检测任务并选择该主机
5. 执行检测任务，确认任务状态变化
6. 如主机已有监控快照，可在主机管理页面直接进入指标详情

## 8.3 应用建模流程

1. 进入“应用中心 -> 应用定义”新增应用或维护已有应用
2. 进入“版本管理”维护应用版本
3. 进入“安装包管理”上传安装包
4. 进入“配置模板”“脚本模板”维护模板内容
5. 完成版本与制品准备

## 8.4 发布任务流程

1. 进入“安装发布 -> 发布任务”查看任务列表
2. 点击“创建”打开抽屉，填写任务名称、应用、版本、环境、主机和执行参数
3. 提交后在列表中查看新任务
4. 对待审批任务执行“批准”或“驳回”
5. 打开任务详情，查看总览、主机明细、执行日志
6. 对满足条件的任务执行 `execute` / `retry` / `rollback` / `cancel`
7. 进入“任务中心 -> 全部任务”查看聚合视图并跳回详情

## 8.5 系统用户管理流程

1. 进入“系统管理 -> 用户管理”查看用户列表与摘要卡片
2. 点击“新增用户”打开抽屉
3. 填写用户名、密码、手机号、团队、登录方式、状态、角色
4. 提交后刷新列表，确认用户已创建
5. 点击“编辑”可修改用户资料、角色、状态和登录方式

## 8.6 流量控制动作流程

1. 进入“流量控制 -> 流量规则”查看策略列表
2. 根据策略当前状态和插件能力选择 `preview`、`apply` 或 `rollback`
3. 页面展示最近一次动作结果反馈
4. 刷新后确认策略状态和回滚令牌变化

> 当前流量 plugin 已具备页面和接口契约，但 `NGINX` / `REST` 实现仍是 skeleton adapter，不应解读为真实生产切流已接通。

---

## 9. 本地运行方式

## 9.1 启动后端

```bash
cd backend
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
mvn -pl envops-boot spring-boot:run
```

默认后端地址：

- `http://127.0.0.1:8080`

## 9.2 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

默认前端地址：

- `http://localhost:9527`

## 9.3 常用验证命令

```bash
cd backend && mvn test
cd frontend && pnpm test:unit
cd frontend && pnpm typecheck
cd frontend && pnpm lint
```

---

## 10. 当前版本已知注意事项

1. `frontend/.env.prod` 中生产 API 地址仍为空，生产部署前必须明确接入方式。
2. 前端首页当前仍以控制台展示和概览为主。
3. `envops-exec` 目前仍是模块骨架，执行器能力主要体现在 `envops-deploy` 的目录与扩展点中。
4. 流量插件当前返回 skeleton 结果，适合联调和契约验证，不应视为真实生产流量切换结果。
5. `supportsPreview`、`supportsApply`、`supportsRollback` 只能表示动作支持位，不能替代 `pluginStatus` 对实现完成度的表达。
6. `RemoteExecutor` 是发布执行适配器，不是流量 plugin，维护时不要混淆两类扩展点。

---

## 11. 结论

`v0.0.3` 相比 `v0.0.2` 的主要增量，是把几个此前偏展示或只到半链路的模块继续补齐到了可操作状态：

- `asset/host` 已支持主机纳管和指标详情跳转
- `monitor/detect-task` 已支持检测任务创建与执行
- `deploy/task` 已支持任务创建、审批与完整详情交互
- `system/user` 已支持真实用户创建与编辑
- `traffic/controller` 已支持预览、应用、回滚动作，并落到真实后端接口

如果需要分别查看用户使用说明和开发说明，请继续参考：

- `docs/envops-v0.0.3-用户操作手册.md`
- `docs/envops-v0.0.3-开发技术说明.md`
