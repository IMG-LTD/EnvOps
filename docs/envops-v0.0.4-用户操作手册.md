# EnvOps v0.0.4 用户操作手册

## 1. 手册说明

本文档面向当前 `v0.0.4` 版本的本地体验者、联调人员与演示使用者。当前版本重点在于修正文档与发版口径，尤其是本地启动方式、Task Center 的范围收口，以及 Traffic 的 not-ready 边界说明。

## 2. 使用前准备

### 2.1 启动后端

```bash
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
ENVOPS_SERVER_PORT=18080 \
bash backend/scripts/run-envops-boot.sh
```

### 2.2 启动前端

```bash
pnpm --dir frontend install
pnpm --dir frontend dev
```

### 2.3 默认访问地址

- 后端：`http://127.0.0.1:18080`
- 前端：`http://localhost:9527`
- 前端 test 模式默认代理后端：`http://127.0.0.1:18080`

### 2.4 默认体验账号

- 用户名：`envops-admin`
- 密码：`EnvOps@123`

## 3. 登录系统

1. 打开 `http://localhost:9527`
2. 输入 `envops-admin / EnvOps@123`
3. 点击登录
4. 登录成功后进入控制台

## 4. 当前可体验范围

### 4.1 资产中心

当前版本资产中心只提供 Host、Credential、Group、Tag 四类入口。其中 Host 与 Credential 可创建并查看列表，Group 与 Tag 为只读列表。当前没有数据库资源、数据库实例或数据库专用生命周期管理页面，因此不要把“资产”理解成已支持数据库纳管。

### 4.2 Deploy

当前版本保留 Deploy 主链路体验能力，包括任务查看、创建、审批、详情与执行相关处理。对外说明以当前真实执行参数为准，不再对外承诺 `deployDir`。

### 4.3 Task Center

Task Center 在 `v0.0.4` 中明确是 deploy-only 队列视图：

- 用于查看 Deploy 任务列表与状态
- 可从队列跳转到 Deploy 详情
- 当前不是跨域统一队列

### 4.4 Traffic

Traffic 在 `v0.0.4` 中明确是 not-ready 页面：

- 页面会显示 not-ready warning
- 动作按钮保持禁用
- 当前不能把 `preview` / `apply` / `rollback` 视为已对外开放的真实切流能力

## 5. 本地使用建议

推荐按以下顺序体验当前版本：

1. 启动后端
2. 启动前端
3. 使用 `envops-admin / EnvOps@123` 登录
4. 体验资产、检测、应用与 Deploy 主链路
5. 在 Task Center 中查看 deploy-only 队列
6. 打开 Traffic 页面确认 not-ready warning 与禁用动作按钮

## 6. Deferred scope

以下内容明确不属于 `v0.0.4` 当前可用范围：

- Traffic 真实外部网关接通
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强

## 7. 版本口径提醒

如果你需要核对当前版本资料，请以以下 `v0.0.4` 文档为准：

- `README.md`
- `docs/envops-v0.0.4-项目详细说明.md`
- `docs/envops-v0.0.4-开发技术说明.md`
- `release/0.0.4-release-notes.md`
- `release/0.0.4-checklist.md`
