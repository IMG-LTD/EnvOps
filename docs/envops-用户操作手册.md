# EnvOps 用户操作手册

## 1. 手册说明

本文档面向当前基线的本地体验者、联调人员与演示使用者。当前阶段重点在于同步本地启动方式、Task Center 的范围收口，以及 Traffic 最小真实能力的使用口径。

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

当前版本资产中心提供 Host、Credential、Group、Tag、Database 五类入口。其中 Host 与 Credential 可创建并查看列表，Group 与 Tag 为只读列表，Database 可创建、筛选、编辑、删除，关联所属主机和通用凭据，并发起真实数据库连通性检测。

数据库资源页面当前支持的主流数据库类型为 `mysql`、`postgresql`、`oracle`、`sqlserver`、`mongodb`、`redis`。页面中的连通性状态 `unknown / online / warning / offline` 中，`online` 与 `offline` 可由真实检测回写，`warning` 仍为人工维护字段。

#### 4.1.1 数据库连通性检测使用方式

1. 进入“资产中心 / 数据库资源”。
2. 新建或编辑数据库资源时，可填写连接用户名与连接密码。
3. 保存后再次打开编辑抽屉，密码输入框会保持空白。留空再次保存表示沿用已保存密码，不会回显旧密码。
4. 在表格行内点击“检测”可发起单条检测。
5. 勾选多条记录后点击“批量检测已选”可发起已选批检。
6. 点击“检测当前页”可对当前分页中的全部记录执行检测。
7. 设置筛选条件后点击“检测全部筛选结果”可按当前筛选条件批量执行检测。
8. 检测结果会刷新列表；批量检测会弹出结果汇总，展示成功、失败、跳过数量和逐条说明。
9. 缺少连接用户名或密码的记录会被跳过并返回原因。

#### 4.1.2 数据库能力边界

- 当前检测通过真实数据库连接与认证执行。
- 当前只支持基础直连，不支持 SSL、连接串、MongoDB 副本集、Redis Sentinel / Cluster 等高级连接能力。
- 当前不包含数据库专用凭据中心、数据库版本采集或数据库专用生命周期编排。

### 4.2 Deploy

当前版本保留 Deploy 主链路体验能力，包括任务查看、创建、审批、详情与执行相关处理。对外说明以当前真实执行参数为准，不再对外承诺 `deployDir`。

### 4.3 Task Center

Task Center 当前明确是 deploy-only 队列视图：

- 用于查看 Deploy 任务列表与状态
- 可从队列跳转到 Deploy 详情
- 当前不是跨域统一队列

### 4.4 Traffic

Traffic 当前可体验的是最小真实切流 MVP：

- 页面会提示当前只支持 `REST` 插件与 `weighted_routing` 策略
- 满足支持条件的记录可执行 `Preview`、`Apply`、`Rollback`
- `Preview`、`Apply`、`Rollback` 都会真实调用外部流量服务
- `Apply` 成功后会写入新的状态与 `rollbackToken`
- 只有已有 `rollbackToken` 的记录才可执行 `Rollback`
- 不支持的记录会保持禁用，并在表格中展示原因
- 当前不提供多插件、多策略矩阵、批量切流、高级编排或灰度报表

## 5. 本地使用建议

推荐按以下顺序体验当前版本：

1. 启动后端
2. 启动前端
3. 使用 `envops-admin / EnvOps@123` 登录
4. 体验资产、检测、应用与 Deploy 主链路
5. 在 Task Center 中查看 deploy-only 队列
6. 打开 Traffic 页面，确认支持范围 warning、动作按钮可用性，以及最近一次动作反馈

## 6. Deferred scope

以下内容明确不属于当前基线可用范围：

- Traffic 的多插件适配、多策略矩阵、批量切流、高级编排与灰度报表
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强

## 7. 文档入口提醒

如果你需要核对当前资料，请以以下材料为准：

- `README.md`
- `docs/envops-项目详细说明.md`
- `docs/envops-开发技术说明.md`
- `docs/envops-用户操作手册.md`
- `release/0.0.5-release-notes.md`
