# EnvOps v0.0.4 开发技术说明

## 1. 文档定位

本文档面向开发、联调、测试与维护人员，说明当前 `v0.0.4` 基线的工程版本字段、本地联调方式、验证命令，以及 Deploy / Task / Traffic 的技术边界。

## 2. 版本字段

当前 `v0.0.4` 基线要求：

- `backend/pom.xml` 包含 `<version>0.0.4-SNAPSHOT</version>`
- `frontend/package.json` 包含 `"version": "0.0.4"`

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

`v0.0.4` 统一使用以下命令作为工程闸口与本地验证入口：

- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

其中：

- 后端启动入口按 reactor-safe 脚本执行
- 前端 targeted unit gate 已有独立脚本，不再串跑无关 spec
- 完整 `pnpm --dir frontend test:unit` 仍作为全量单测基线

## 5. 模块口径与边界

### 5.1 Asset

`v0.0.4` 资产模块当前前后端只实现了 `/api/assets/hosts`、`/api/assets/credentials`、`/api/assets/groups`、`/api/assets/tags` 四类入口，对应页面也只有 Host、Credential、Group、Tag。当前没有 database / datasource / db instance 路由、接口、类型、表结构或契约测试，因此不能把资产模块表述成数据库资源管理。

### 5.2 Deploy

Deploy 创建契约当前只承诺真实执行参数，不再对外承诺 `deployDir`。开发和联调说明必须与当前页面、接口和 release materials 保持一致。

### 5.3 Task Center

Task Center 在 `v0.0.4` 中明确是 deploy-only 队列视图：

- 只服务于 Deploy 任务队列的查询、状态查看与详情跳转
- 当前不是跨域统一队列
- 相关文案应明确写成 deploy-only

### 5.4 Traffic

Traffic 在 `v0.0.4` 中必须明确写成 not-ready：

- 当前页面与接口用于表达 skeleton / not-ready 边界
- 页面应显示 not-ready warning
- 动作按钮禁用
- 当前不应把 `preview` / `apply` / `rollback` 写成用户已可执行的真实能力

## 6. 账号与安全基线

- 本地默认账号仍为 `envops-admin / EnvOps@123`
- 系统账号密码已改为哈希存储与校验
- 启动后端时必须显式提供 `ENVOPS_SECURITY_TOKEN_SECRET`、`ENVOPS_CREDENTIAL_PROTECTION_SECRET` 与 `ENVOPS_SERVER_PORT`

## 7. Deferred scope

以下内容继续明确排除在 `v0.0.4` 之外：

- Traffic 真实外部网关接通
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强
