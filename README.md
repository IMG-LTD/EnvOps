# EnvOps v0.0.2

EnvOps 是一个面向资产管理、检测采集、应用建模、发布任务与任务跟踪场景的运维平台仓库。当前 `v0.0.2` 基线已经包含可运行的 `frontend/` 与 `backend/`，并补齐了面向用户和开发者的说明文档，便于本地体验、联调与后续迭代。

## 当前版本概览

当前版本可以明确对外说明的内容包括：

- 前后端分离的基础运行形态已经成型
- 动态路由登录流程已接入真实后端接口
- 资产、检测、应用、发布任务、任务中心已形成一条可联调主链路
- `deploy/task` 页面已支持列表、详情、主机明细、执行日志，以及 `execute` / `retry` / `rollback` / `cancel`
- 已提供默认体验账号、种子数据与本地启动方式
- 已补充 `v0.0.2` 项目说明、开发说明和用户手册

## 仓库结构

```text
.
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── docs/        # v0.0.2 项目说明、开发说明、用户手册
├── release/     # 发布清单与发布说明草稿
├── README.md    # 仓库首页
└── LICENSE
```

## 核心能力

当前版本已具备的主要能力包括：

1. **资产中心**
   - 主机列表查询
   - 凭据列表查询与新增
   - 分组与标签浏览

2. **检测中心**
   - 检测任务列表查询
   - 主机最新事实快照查询

3. **应用中心**
   - 应用定义增删改查
   - 版本增删改查
   - 安装包上传与删除
   - 配置模板与脚本模板增删改查

4. **安装发布与任务中心**
   - 发布任务列表与详情
   - 主机明细与执行日志
   - 任务执行、重试、回滚、取消
   - 任务中心聚合查询

## 文档入口

当前仓库已提供以下文档：

- [项目详细说明](docs/envops-v0.0.2-项目详细说明.md)
- [开发技术说明](docs/envops-v0.0.2-开发技术说明.md)
- [用户操作手册](docs/envops-v0.0.2-用户操作手册.md)

如果你想快速了解当前版本，建议先读：

1. 本 README
2. `docs/envops-v0.0.2-项目详细说明.md`
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
- 示例发布任务：`DT202604151600000001`
- 主机、检测任务、应用、安装包、模板、版本等基础演示数据

这些数据适用于本地体验与联调环境。如果用于更长期的共享环境，建议自行替换默认账号、密码和密钥。

## 测试与验证

- 后端测试：`cd backend && mvn test`
- 前端单测：`cd frontend && pnpm test:unit`
- 前端类型检查：`cd frontend && pnpm typecheck`

当前版本至少可以验证以下路径：

1. 本地启动前后端
2. 使用默认账号登录
3. 动态菜单正常加载
4. 打开 `deploy/task` 页面查看示例任务详情、主机明细与执行日志

## 当前限制

当前 `v0.0.2` 仍有以下边界：

- `home`、`system/user`、`traffic/controller` 仍以展示页为主
- 前端当前没有“新建发布任务”“审批通过/拒绝”的页面入口
- `envops-exec` 仍是模块骨架
- 流量控制接口与页面当前更偏目录展示与能力预留，不是完整生产化流量系统

## Roadmap

后续建议优先推进：

- 补齐发布任务创建页与审批页
- 补齐系统管理真实接口与页面
- 补齐流量控制真实操作页
- 推进执行器与生产化能力完善
- 统一外部文档与发布口径

## License

- 仓库级 `LICENSE` 适用于当前公开的 EnvOps 仓库基线
- `frontend/` 的依赖与第三方许可证仍以各自上游许可条款为准
