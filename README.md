# EnvOps v0.0.4

EnvOps 是一个面向资产管理、检测采集、应用建模、发布任务与流量控制场景的运维平台仓库。当前 `v0.0.4` 基线重点完成 release gate 修复，修正工程闸口、账号安全、本地联调默认配置，以及 Deploy / Task / Traffic 的对外口径。

## 当前版本概览

当前 `v0.0.4` 明确纳入说明与交付范围的内容包括：

- 后端 release gate 与本地启动入口统一收敛到 reactor-safe 脚本。
- 前端 targeted Vitest runner 与完整单测基线已恢复，可分别用于 release gate 与全量验证。
- 系统账号密码已改为哈希存储与校验，本地默认账号仍保留 `envops-admin / EnvOps@123`。
- Deploy 创建表单对外只承诺真实执行参数，不再承诺 `deployDir`。
- Task Center 当前是 deploy-only 队列视图，不是跨域统一队列。
- Traffic 页面与接口当前明确处于 skeleton / not-ready 边界，不对外承诺真实切流能力。

## 仓库结构

```text
.
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # v0.0.2 / v0.0.3 / v0.0.4 版本化文档
├── release/     # 发布说明与核对清单
├── README.md    # 仓库首页
└── LICENSE
```

## 文档入口

当前 `v0.0.4` 文档集包括：

- [项目详细说明](docs/envops-v0.0.4-项目详细说明.md)
- [开发技术说明](docs/envops-v0.0.4-开发技术说明.md)
- [用户操作手册](docs/envops-v0.0.4-用户操作手册.md)
- [Release Notes](release/0.0.4-release-notes.md)
- [Checklist](release/0.0.4-checklist.md)

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
   - Deploy 创建契约聚焦真实执行参数，不再对外承诺 `deployDir`。

2. **Task Center**
   - 当前仅提供 deploy-only 队列视图。
   - 用于查看 Deploy 任务列表、状态和跳转详情。
   - 当前不是跨域统一队列。

3. **Traffic**
   - 当前页面与接口明确标记为 skeleton / not-ready。
   - 页面应展示 not-ready warning，动作按钮保持禁用。
   - 当前不对外承诺 `preview` / `apply` / `rollback` 为可执行的真实切流能力。

## 验证命令

推荐至少执行以下命令验证 `v0.0.4` 基线：

- `bash backend/scripts/test-envops-boot.sh`
- `pnpm --dir frontend test:unit`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
- `pnpm --dir frontend build:test`
- `pnpm --dir frontend exec oxlint .`
- `pnpm --dir frontend exec eslint .`

## Deferred scope

以下内容明确继续留在 `v0.0.4` 之外：

- Traffic 真实外部网关接通
- Task Center 跨域统一队列
- Deploy 大规模主机检索与更深执行器增强

## License

- 仓库级 `LICENSE` 适用于当前公开的 EnvOps 仓库基线
- `frontend/` 的依赖与第三方许可证仍以各自上游许可条款为准
