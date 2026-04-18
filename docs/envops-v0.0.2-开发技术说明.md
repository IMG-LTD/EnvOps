# EnvOps / CMDB v0.0.2 开发技术说明

## 1. 文档定位

本文档面向开发、联调、测试和维护人员，重点说明当前 `v0.0.2` 代码基线下的：

- 仓库结构
- 前端技术基线
- 后端技术基线
- 前后端接口契约
- 本地运行与联调方式
- 当前版本边界与后续维护重点

本文档以主工作区中实际存在的 `frontend/` 和 `backend/` 为准，不再沿用此前“前端目录为 `soybean-admin-main/`、后端尚未进入主工作区”的旧口径。

---

## 2. 仓库代码基线

## 2.1 当前结构

当前 `v0.0.2` 仓库主结构如下：

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
- `release/` 用于保存发布清单与发布说明草稿

## 2.2 与旧基线的关键差异

相较此前整理过的旧文档口径，当前有三点必须统一：

1. 前端目录已经是 `frontend/`，不是 `soybean-admin-main/`
2. 后端已经在主工作区内，`backend/` 是正式源码，不再只是参考实现
3. 登录与路由接口契约已经统一为 `/api/auth/*` 和 `/api/routes/*`

> 当前 `README.md`、`release/` 和 `docs/` 已统一到 `v0.0.2` 口径，后续如调整发布范围，应同步更新三处文档。

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

这意味着前端不是简单页面集合，而是已经具备完整控制台壳层能力的标准后台项目。

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
- `fetchGetAssetCredentials`
- `fetchCreateAssetCredential`
- `fetchGetAssetGroups`
- `fetchGetAssetTags`

### 检测模块

- `fetchGetMonitorDetectTasks`
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
- `fetchPostExecuteDeployTask`
- `fetchPostRetryDeployTask`
- `fetchPostRollbackDeployTask`
- `fetchPostCancelDeployTask`
- `fetchGetDeployTaskHosts`
- `fetchGetDeployTaskLogs`
- `fetchGetTaskCenterTasks`

## 3.8 页面落地情况评估

### 已接真实接口的页面

1. 资产中心
   - `asset/host`：列表与刷新
   - `asset/credential`：列表与新增
   - `asset/group`：列表
   - `asset/tag`：列表

2. 检测中心
   - `monitor/detect-task`：检测任务列表
   - `monitor/metric`：按主机查询事实快照

3. 应用中心
   - `app/definition`：增删改查
   - `app/version`：增删改查
   - `app/package`：上传、列表、删除
   - `app/config-template`：增删改查
   - `app/script-template`：增删改查

4. 发布与任务中心
   - `deploy/task`：列表、详情、主机明细、日志、执行/重试/回滚/取消
   - `task/center`：聚合列表、筛选、分页、跳转详情

### 当前仍偏壳层/演示的页面

- `home`：展示平台摘要与静态指标，不直接请求后端
- `system/user`：静态数据演示
- `traffic/controller`：静态数据演示
- 登录页中的验证码登录：仍是占位逻辑

### 前端当前缺口

- 没有新建发布任务页
- 没有发布任务审批页
- 没有真实的用户管理、流量控制操作页

---

## 4. 后端技术基线

## 4.1 技术栈与运行方式

`backend/` 是 Spring Boot 3.3.6 的 Maven 多模块工程，采用：

- Java 17
- MyBatis Spring Boot 3.0.3
- Spring Security
- H2 内存数据库
- JUnit 5 / MockMvc

后端当前并不是微服务拆分，而是“多模块单体”结构。

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
- `system`：登录、用户信息、动态路由
- `asset`：主机、凭据、分组、标签
- `monitor`：检测任务、主机快照
- `app`：应用与版本建模、安装包、模板
- `deploy`：发布任务、执行器目录、任务中心
- `exec`：预留模块，当前基本为空
- `traffic`：流量目录与插件目录

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
- 应用、安装包、模板相关接口需要 `SUPER_ADMIN`

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

### 资产模块

- `/api/assets/hosts`
- `/api/assets/credentials`
- `/api/assets/groups`
- `/api/assets/tags`

### 检测模块

- `/api/monitor/detect-tasks`
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

## 4.9 测试资源与种子数据

当前本地演示数据来自 `envops-boot/src/main/resources/data.sql`，已包含：

- 默认账号：`envops-admin`
- 默认密码：`EnvOps@123`
- 角色：`SUPER_ADMIN`
- 4 台主机
- 2 个检测任务
- 2 个应用定义
- 2 个安装包
- 2 个配置模板
- 2 个脚本模板
- 2 个应用版本
- 1 个示例发布任务 `DT202604151600000001`

测试主要集中在 `envops-boot` 聚合层执行。

## 4.10 后端当前边界

- `envops-exec` 目前基本只有 `pom.xml`，没有实质业务代码
- `envops-traffic` 当前主要提供策略和插件目录接口
- `deploy` 模块已具备较完整的任务链路，但是否进入生产化仍需配合真实执行器与真实存储方案

---

## 5. 前后端契约说明

## 5.1 已经对齐的契约

当前 `v0.0.2` 下，以下问题已经完成对齐：

1. 登录接口前缀统一为 `/api/auth/*`
2. 路由接口前缀统一为 `/api/routes/*`
3. 登录返回结构统一为仅返回 `token`
4. 统一成功码为 `0000`
5. 前端统一通过 `Authorization: Bearer <token>` 携带登录态

这意味着此前旧文档里提到的“前端使用 `/route/getUserRoutes`、后端使用 `/api/routes/getUserRoutes`”这类差异，当前已经不再成立。

## 5.2 当前仍存在的产品层缺口

虽然接口契约已经基本统一，但产品层仍有缺口：

- 后端已提供 `POST /api/deploy/tasks`、`approve`、`reject`，前端暂未提供对应页面
- 后端已有 `/api/traffic/policies`、`/api/traffic/plugins`，前端当前页面仍是静态展示
- 后端没有成型的系统用户管理接口，前端 `system/user` 因此仍是静态页

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
cd backend && mvn test
cd frontend && pnpm test:unit
cd frontend && pnpm typecheck
```

如果只是核对文档所描述的能力，建议至少验证：

1. 后端能正常启动
2. 前端能正常登录
3. 动态菜单能正常加载
4. `deploy/task` 的列表和详情可正常打开

---

## 7. 维护建议

## 7.1 持续维护 README 与发布文档口径

当前 `README.md` 已完成版本统一，后续应继续保持 README、`release/` 和 `docs/` 三处说明同步，避免再次出现版本口径漂移。

## 7.2 区分“真实业务页”和“控制台壳层”

后续维护时，建议明确分层：

- 壳层页：`home`、`system/user`、`traffic/controller`
- 真实业务页：`asset`、`monitor`、`app`、`deploy/task`、`task/center`

这样可以避免把尚未接入真实接口的演示页误判为完整业务能力。

## 7.3 优先补齐发布链路缺口

从平台价值看，当前最值得继续补齐的是：

1. 新建发布任务页
2. 发布任务审批页
3. 流量控制真实操作页
4. 系统管理真实用户接口与页面
5. 执行器模块的真实实现

## 7.4 安全与生产化建议

如果后续继续推进：

- 登录密码应改为安全存储和校验
- H2 仅适合作为本地演示或测试基线，应替换为正式数据库
- 生产环境需补齐前端 `VITE_SERVICE_BASE_URL`
- 需进一步明确部署、日志、配置管理与密钥管理方案

---

## 8. 结论

当前 `v0.0.2` 已经不是“前端模板 + 后端设计文档”的状态，而是：

- 前端已有正式工程、动态路由、真实请求层和多模块业务页
- 后端已有正式多模块源码、可运行入口、统一响应、安全配置和种子数据
- 资产、检测、应用、发布任务、任务中心构成了当前最完整的联调主链路

下一阶段的重点，不再是确认有没有后端，而是继续补齐剩余产品能力与生产化能力。
