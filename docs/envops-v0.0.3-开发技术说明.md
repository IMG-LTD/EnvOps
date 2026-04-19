# EnvOps / CMDB v0.0.3 开发技术说明

## 1. 文档定位

本文档面向开发、联调、测试和维护人员，重点说明当前 `v0.0.3` 代码基线下的：

- 仓库结构
- 前端技术基线
- 后端技术基线
- 前后端接口契约
- 本地运行与联调方式
- 当前版本边界与后续维护重点

本文档以主工作区中实际存在的 `frontend/` 和 `backend/` 为准，对应当前 `v0.0.3` 版本可运行代码，而不是历史整理口径。

---

## 2. 仓库代码基线

## 2.1 当前结构

当前 `v0.0.3` 仓库主结构如下：

```text
cmdb/
├── backend/
├── frontend/
├── docs/
├── release/
├── README.md
└── LICENSE
```

其中：

- `frontend/` 是正式前端工程
- `backend/` 是正式后端工程
- `docs/` 用于维护项目文档
- `release/` 用于保存发布说明与版本变更记录

## 2.2 与 `v0.0.2` 的关键差异

相较 `v0.0.2`，当前版本至少有以下几条需要同步到开发口径：

1. `asset/host` 已从只读列表补齐为“列表 + 纳管表单 + 指标详情跳转”页面
2. `monitor/detect-task` 已支持创建检测任务和手动执行任务
3. `deploy/task` 已支持任务创建、审批、详情、主机明细、日志与任务动作
4. `system/user` 已从静态展示补齐为真实列表、新建与编辑页面
5. `traffic/controller` 已从静态展示页补齐为“列表 + preview / apply / rollback”动作页，并对接真实后端接口

> 后续如果继续补链路，务必同步更新 `README.md`、`docs/` 和 `release/`，不要再让代码和文档分家。

---

## 3. 前端技术基线

## 3.1 技术栈与命令

`frontend/package.json` 显示当前前端主要采用：

- Vue 3.5
- TypeScript 6
- Vite 8
- Pinia 3
- Vue Router 5
- Naive UI
- UnoCSS
- vue-i18n
- Vitest

常用命令：

```bash
cd frontend
pnpm install
pnpm dev
pnpm build
pnpm build:test
pnpm test:unit
pnpm typecheck
pnpm lint
```

其中：

- `pnpm dev` = `vite --mode test`
- `pnpm build` = `vite build --mode prod`
- `pnpm test:unit` 使用 Vitest
- `pnpm typecheck` 使用 `vue-tsc --noEmit --skipLibCheck`

## 3.2 环境变量基线

`.env`、`.env.test`、`.env.prod` 当前的关键点如下：

- `VITE_AUTH_ROUTE_MODE=dynamic`
- `VITE_ROUTER_HISTORY_MODE=history`
- `VITE_HTTP_PROXY=Y`
- `VITE_SERVICE_SUCCESS_CODE=0000`
- `.env.test` 默认后端地址为 `http://127.0.0.1:8080`
- `.env.prod` 的 `VITE_SERVICE_BASE_URL` 仍为空

Vite 配置当前确认：

- dev 端口：`9527`
- preview 端口：`9725`
- `host: 0.0.0.0`
- dev 模式下支持代理

## 3.3 启动入口与应用装配

前端启动入口是 `frontend/src/main.ts`。当前启动顺序为：

1. 初始化 loading
2. 初始化 nprogress
3. 初始化离线图标
4. 初始化 dayjs
5. 创建 Vue App
6. 安装 Pinia Store
7. 安装 Router
8. 安装 i18n
9. 启用版本通知
10. 挂载应用

这意味着前端已经是完整控制台壳层，不是简单页面集合。

## 3.4 路由与权限加载

路由相关的关键点：

- 路由生成文件：`src/router/elegant/routes.ts`
- 动态路由接口：`/api/routes/getConstantRoutes`、`/api/routes/getUserRoutes`
- 路由模式：当前默认使用 dynamic route
- 登录后由 `auth` store 拉起用户信息与路由初始化

当前已生成的主要业务路由包括：

- `home`
- `asset/host`
- `asset/group`
- `asset/tag`
- `asset/credential`
- `monitor/detect-task`
- `monitor/metric`
- `app/definition`
- `app/version`
- `app/package`
- `app/config-template`
- `app/script-template`
- `deploy/task`
- `task/center`
- `traffic/controller`
- `system/user`

## 3.5 状态管理

Pinia 当前核心模块包括：

- `auth`：token、用户信息、登录态初始化、登出清理
- `route`：动态路由、菜单、缓存路由
- `tab`：标签页缓存与切换
- `app`：全局 UI 状态
- `theme`：主题与视觉配置

其中 `auth` store 的真实行为是：

1. 登录时调用 `/api/auth/login`
2. 将返回的 `token` 写入本地存储
3. 调用 `/api/auth/getUserInfo`
4. 初始化动态菜单与路由
5. 根据用户切换情况清理标签页状态

## 3.6 请求层契约

请求层位于 `frontend/src/service/request/index.ts`。当前已确认的能力包括：

- 统一 `baseURL`
- 自动附加 `Authorization`
- 按 `code === 0000` 判断成功
- 统一处理后端业务错误
- 处理 token 失效与 401
- 保留 `demoRequest` 作为其他服务示例请求实例

这要求后端统一返回：

```json
{
  "code": "0000",
  "msg": "success",
  "data": {}
}
```

## 3.7 前端 API 封装现状

当前 `src/service/api/` 已接入的接口可概括为：

### 认证与路由

- `fetchLogin`
- `fetchGetUserInfo`
- `fetchGetConstantRoutes`
- `fetchGetUserRoutes`

### 资产模块

- `fetchGetAssetHosts`
- `fetchCreateAssetHost`
- `fetchGetAssetCredentials`
- `fetchCreateAssetCredential`
- `fetchGetAssetGroups`
- `fetchGetAssetTags`

### 检测模块

- `fetchGetMonitorDetectTasks`
- `fetchPostCreateMonitorDetectTask`
- `fetchPostExecuteMonitorDetectTask`
- `fetchGetMonitorHostFactsLatest`

### 应用模块

- 应用定义增删改查
- 版本增删改查
- 安装包上传/删除/列表
- 配置模板增删改查
- 脚本模板增删改查

### 任务与发布模块

- `fetchGetDeployTasks`
- `fetchGetDeployTask`
- `fetchPostCreateDeployTask`
- `fetchPostApproveDeployTask`
- `fetchPostRejectDeployTask`
- `fetchPostExecuteDeployTask`
- `fetchPostRetryDeployTask`
- `fetchPostRollbackDeployTask`
- `fetchPostCancelDeployTask`
- `fetchGetDeployTaskHosts`
- `fetchGetDeployTaskLogs`
- `fetchGetTaskCenterTasks`

### 流量模块

- `fetchGetTrafficPolicies`
- `fetchGetTrafficPlugins`
- `fetchPostPreviewTrafficPolicy`
- `fetchPostApplyTrafficPolicy`
- `fetchPostRollbackTrafficPolicy`

### 系统管理模块

- `fetchGetSystemUsers`
- `fetchCreateSystemUser`
- `fetchUpdateSystemUser`

## 3.8 页面落地情况评估

### 已接真实接口的页面

1. 资产中心
   - `asset/host`：列表、主机纳管、指标详情跳转
   - `asset/credential`：列表与新增
   - `asset/group`：列表
   - `asset/tag`：列表

2. 检测中心
   - `monitor/detect-task`：列表、创建、执行
   - `monitor/metric`：按主机查询事实快照

3. 应用中心
   - `app/definition`：增删改查
   - `app/version`：增删改查
   - `app/package`：上传、列表、删除
   - `app/config-template`：增删改查
   - `app/script-template`：增删改查

4. 发布与任务中心
   - `deploy/task`：列表、创建、审批、详情、主机明细、日志、执行/重试/回滚/取消
   - `task/center`：聚合列表、筛选、分页、跳转详情

5. 流量控制
   - `traffic/controller`：列表、摘要卡片、preview / apply / rollback 动作、最新动作反馈

6. 系统管理
   - `system/user`：列表、摘要、新建、编辑

### 当前仍偏壳层/演示的页面

- `home`：展示平台摘要与静态指标，不直接请求后端
- 登录页中的验证码登录：仍是占位逻辑

### 前端当前缺口

- 流量页面虽接入真实接口，但插件执行结果仍是 skeleton 结果
- 系统用户页当前聚焦创建与编辑，还未覆盖更完整的用户生命周期动作
- 更生产化的发布执行与编排能力仍未补齐

---

## 4. 后端技术基线

## 4.1 技术栈与运行方式

`backend/` 是 Spring Boot 3.3.6 的 Maven 多模块工程，采用：

- Java 17
- MyBatis Spring Boot 3.0.3
- Spring Security
- H2 内存数据库
- JUnit 5 / MockMvc

后端当前不是微服务拆分，而是多模块单体结构。

## 4.2 模块结构

`backend/pom.xml` 当前声明：

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

模块职责建议按以下方式理解：

- `boot`：启动、装配、Mapper 扫描
- `common`：统一响应 `R<T>`、通用异常
- `framework`：安全、Token、全局基础设施
- `system`：登录、用户信息、动态路由、系统用户管理
- `asset`：主机、凭据、分组、标签
- `monitor`：检测任务、主机快照
- `app`：应用与版本建模、安装包、模板
- `deploy`：发布任务、执行器目录、任务中心
- `exec`：预留模块，当前基本为空
- `traffic`：流量策略、插件目录和动作接口

## 4.3 启动与数据初始化

后端启动入口是 `envops-boot/src/main/java/com/img/envops/EnvOpsApplication.java`。

`application.yml` 当前说明：

- 数据源为 `jdbc:h2:mem:envops`
- 启动时自动加载 `schema.sql`
- 启动时自动加载 `data.sql`
- 读取两个环境变量：
  - `ENVOPS_SECURITY_TOKEN_SECRET`
  - `ENVOPS_CREDENTIAL_PROTECTION_SECRET`

这两个密钥都要求长度不少于 32，否则会在启动阶段直接抛出异常。

## 4.4 统一响应与接口风格

统一响应对象 `R<T>` 的输出格式为：

```json
{
  "code": "0000",
  "msg": "success",
  "data": {}
}
```

前端 `.env` 中的 `VITE_SERVICE_SUCCESS_CODE=0000` 与之保持一致。

## 4.5 安全配置

`SecurityConfig` 当前确认：

- 放行 `POST /api/auth/login`
- 放行 `GET /api/routes/getConstantRoutes`
- `/api/**` 其余接口默认需要认证
- 应用、安装包、模板、系统用户管理相关接口需要 `SUPER_ADMIN`

这意味着本地演示账号需要具备管理员角色，当前种子数据中的 `envops-admin` 已满足该条件。

## 4.6 Token 与鉴权模型

当前 Token 服务不是直接使用标准 JWT 库，而是自研的 HMAC SHA256 风格令牌：

- `createAccessToken`
- `createRefreshToken`
- `resolveAccessToken`
- `extractUsernameFromAccessToken`

虽然 Token 服务支持 refresh token 的内部生成能力，但当前登录接口实际仅返回：

```json
{
  "token": "..."
}
```

前端当前的类型定义也已经同步为仅包含 `token`。

## 4.7 认证实现注意点

`AuthApplicationService` 当前登录逻辑的关键点：

- 校验 `userName`、`password` 必填
- 从数据库按用户名查找用户
- 当前直接比较明文密码
- 登录成功后返回 access token

这套实现适合本地演示和联调，但如果后续进入更正式环境，应优先改造密码加密与安全策略。

## 4.8 控制器与接口分布

### 系统模块

- `/api/auth/login`
- `/api/auth/getUserInfo`
- `/api/routes/getConstantRoutes`
- `/api/routes/getUserRoutes`
- `/api/system/users`
- `/api/system/users/{id}`

### 资产模块

- `/api/assets/hosts`
- `/api/assets/credentials`
- `/api/assets/groups`
- `/api/assets/tags`

### 检测模块

- `/api/monitor/detect-tasks`
- `/api/monitor/detect-tasks/{id}/execute`
- `/api/monitor/hosts/{hostId}/facts/latest`

### 应用模块

- `/api/apps`
- `/api/apps/{id}`
- `/api/apps/{id}/versions`
- `/api/app-versions/{id}`
- `/api/packages`
- `/api/packages/upload`
- `/api/config-templates`
- `/api/script-templates`

### 发布模块

- `/api/deploy/tasks`
- `/api/deploy/tasks/{id}`
- `/api/deploy/tasks/{id}/approve`
- `/api/deploy/tasks/{id}/reject`
- `/api/deploy/tasks/{id}/execute`
- `/api/deploy/tasks/{id}/cancel`
- `/api/deploy/tasks/{id}/retry`
- `/api/deploy/tasks/{id}/rollback`
- `/api/deploy/tasks/{id}/hosts`
- `/api/deploy/tasks/{id}/logs`
- `/api/task-center/tasks`
- `/api/deploy/executors`

### 流量模块

- `/api/traffic/policies`
- `/api/traffic/plugins`
- `/api/traffic/policies/{id}/preview`
- `/api/traffic/policies/{id}/apply`
- `/api/traffic/policies/{id}/rollback`

## 4.9 Plugin 与执行器边界

这是当前版本最容易被文档写歪的地方。

### TrafficPlugin

`TrafficPlugin` 位于 `envops-traffic`，是流量动作适配接口。它提供：

- 插件标识：`pluginType`、`pluginName`
- 插件状态：`pluginStatus`
- 动作能力位：`supportsPreview`、`supportsApply`、`supportsRollback`
- 动作实现：`preview`、`apply`、`rollback`

当前已落地的实现：

- `NginxTrafficPlugin`
- `RestTrafficPlugin`

两者当前的共同点：

- 都会出现在插件目录接口中
- 都支持返回契约化结果
- `pluginStatus` 都是 `NOT_IMPLEMENTED`

所以这里要分清：

- `supports*` 表示动作是否允许在当前插件模型中暴露
- `pluginStatus` 表示这个插件是不是已经接好了真实外部系统

当前它们更像 skeleton adapter，不是生产 ready plugin。

### RemoteExecutor

`RemoteExecutor` 位于 `envops-deploy`，是发布任务的远程执行适配接口。它提供：

- 执行器标识：`executorType`、`executorName`
- 执行器状态：`executorStatus`
- 执行动作：`exec`、`upload`、`detect`

当前 `SshRemoteExecutor`：

- `executorStatus = READY`
- `exec`、`upload` 已走真实 SSH 进程执行适配
- `detect` 当前仍返回适配器级探测结果

结论很简单：

- `TrafficPlugin` 处理流量
- `RemoteExecutor` 处理发布执行

别把 deploy executor 写成 traffic plugin。那会把边界写烂。

## 4.10 测试资源与契约覆盖

后端聚合层当前可见的控制器测试包括：

- `AuthRouteControllerTest`
- `AssetControllerTest`
- `MonitorControllerTest`
- `AppControllerTest`
- `DeployTaskControllerTest`
- `DeployExecutorControllerTest`
- `TrafficControllerTest`
- `UserControllerTest`

前端侧还补了页面级契约测试，当前至少包括：

- `frontend/src/views/monitor/monitor-contract.spec.ts`
- `frontend/src/views/traffic/traffic-contract.spec.ts`
- `frontend/src/views/system/user-contract.spec.ts`

这些测试的价值不是替代真实 UI 体验，而是把页面是否真的连到了正确接口、是否保留关键动作入口，钉在代码层。

## 4.11 后端当前边界

- `envops-exec` 目前基本只有 `pom.xml`，没有实质业务代码
- `envops-traffic` 已支持真实状态变更，但插件返回结果仍是 skeleton 信息
- `deploy` 模块已具备较完整的任务链路，但要进入生产化仍需真实执行器与真实存储方案
- `system/user` 当前支持列表、新建、编辑，但还不是完整身份治理后台

---

## 5. 前后端契约说明

## 5.1 已经对齐的契约

当前 `v0.0.3` 下，以下问题已经完成对齐：

1. 登录接口前缀统一为 `/api/auth/*`
2. 路由接口前缀统一为 `/api/routes/*`
3. 登录返回结构统一为仅返回 `token`
4. 统一成功码为 `0000`
5. 前端统一通过 `Authorization: Bearer <token>` 携带登录态
6. `asset/host` 页面已经消费 `POST /api/assets/hosts`
7. `monitor/detect-task` 页面已经消费创建和执行接口
8. `deploy/task` 页面已经消费创建、审批和任务动作接口
9. `system/user` 页面已经消费列表、创建、更新接口
10. `traffic/controller` 页面已经消费 `preview`、`apply`、`rollback` 三个动作接口

## 5.2 当前仍存在的产品层缺口

虽然接口契约已经基本统一，但产品层仍有缺口：

- 流量插件当前仍是 skeleton 实现，真实外部系统接入没有完成
- 系统用户管理还缺更完整的生命周期动作与审计能力
- 更生产化的执行器编排、存储和安全方案还未完成

---

## 6. 本地开发与联调方式

## 6.1 后端启动

```bash
cd backend
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
mvn -pl envops-boot spring-boot:run
```

## 6.2 前端启动

```bash
cd frontend
pnpm install
pnpm dev
```

## 6.3 默认联调关系

- 前端：`http://localhost:9527`
- 后端：`http://127.0.0.1:8080`
- 前端 `.env.test` 默认直接对接本地后端

## 6.4 常用验证命令

```bash
cd frontend && pnpm lint
cd frontend && pnpm test:unit
cd frontend && pnpm typecheck
cd backend && mvn test
```

如果只是核对 `v0.0.3` 新补链路，建议优先验证：

```bash
cd frontend && pnpm exec vitest run src/views/monitor/monitor-contract.spec.ts
cd frontend && pnpm exec vitest run src/views/traffic/traffic-contract.spec.ts
cd frontend && pnpm exec vitest run src/views/system/user-contract.spec.ts
cd backend && mvn -pl envops-boot -am -Dtest=AssetControllerTest,TrafficControllerTest,UserControllerTest test
```

## 6.5 类型检查注意项

当前仓库脚本 `pnpm typecheck` 使用 `vue-tsc --noEmit --skipLibCheck`，这是更接近项目当前基线的检查方式。

如果直接运行裸命令 `vue-tsc --noEmit`，在当前环境下可能会命中第三方 `node_modules` 的环境类型噪音。这更像依赖声明问题，不等价于本次业务改动引入的前端类型回归。

---

## 7. 维护建议

## 7.1 持续维护 README 与发布文档口径

后续应继续保持 `README.md`、`release/` 和 `docs/` 三处说明同步，避免代码和对外口径再次漂移。

## 7.2 区分“真实业务页”和“控制台壳层”

后续维护时，建议明确分层：

- 壳层页：`home`
- 真实业务页：`asset`、`monitor`、`app`、`deploy/task`、`task/center`、`system/user`
- 联调级动作页：`traffic/controller`

这样可以避免把 still skeleton 的执行结果误判为已经生产化。

## 7.3 优先补齐剩余缺口

从平台价值看，当前最值得继续补齐的是：

1. 流量 plugin 真实接入
2. 系统用户更完整的生命周期动作
3. 执行器模块的真实实现与编排能力
4. 密码安全存储和更正式的安全策略
5. 生产环境配置、日志、部署治理能力

## 7.4 安全与生产化建议

如果后续继续推进：

- 登录密码应改为安全存储和校验
- H2 仅适合作为本地演示或测试基线，应替换为正式数据库
- 生产环境需补齐前端 `VITE_SERVICE_BASE_URL`
- 需进一步明确部署、日志、配置管理与密钥管理方案

---

## 8. 结论

当前 `v0.0.3` 已经不是“前端能看、后端能列”的阶段了，而是把几条关键链路继续往前推了一步：

- 资产页面能新增主机
- 检测页面能创建并执行任务
- 发布任务页面能创建并审批任务
- 系统用户页面能真实创建和编辑用户
- 流量页面能真正发起 `preview` / `apply` / `rollback`

下一阶段的重点，不是再补概念页，而是把剩余的 skeleton 执行链路和生产化边界做实。
