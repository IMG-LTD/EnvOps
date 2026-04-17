# EnvOps 0.0.1-beta

EnvOps 是一个面向资产管理、应用管理、巡检、发布与任务编排场景的运维平台仓库。本 README 仅描述当前已经整理到可对外说明状态的 `0.0.1-beta` 范围，帮助开源读者快速了解仓库结构、已完成能力与本地启动方式。

## 当前 beta 范围

当前仓库主结构为：

- `backend/`：Spring Boot 3.3 多模块 Maven 项目
- `frontend/`：Vue 3 + Vite + TypeScript 项目

本次 `0.0.1-beta` 可以明确对外说明的内容包括：

- 资产、应用、巡检、发布、任务编排的首版控制台壳层
- 动态路由登录流程
- deploy task 执行链：`execute` / `retry` / `rollback` / `cancel`
- `deploy/task` 页面列表、详情、主机明细、执行日志
- 登录密码规则与 i18n 字面量 `@` 相关阻塞已修复，并已补充回归测试
- `deploy/task` 页面浏览器级 smoke 已通过人工验证

## 仓库结构

```text
.
├── backend/     # Spring Boot 3.3 多模块后端
├── frontend/    # Vue 3 + Vite + TypeScript 前端控制台
├── release/     # 0.0.1-beta 校验清单与发布说明草稿
└── README.md    # 仓库对外首页
```

说明：为保持公开 beta 边界收敛，此基线未纳入此前内部使用的 `envops-architecture-full.md` 与 `docs/` 内容。

## 运行环境建议

- JDK 17
- Maven 3
- Node.js >= 20.19.0
- pnpm >= 10.5.0

## 本地启动

### 1. 启动后端

```bash
cd backend
mvn -pl envops-boot spring-boot:run
```

### 2. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

补充说明：

- 前端开发服务默认端口为 `9527`
- 本地测试环境下，前端默认请求的后端基地址为 `http://127.0.0.1:8080`
- `frontend/.env.prod` 中 `VITE_SERVICE_BASE_URL` 当前为空，生产部署前仍需确认 API 路由方式

## 默认体验账号与种子数据

仓库已提供本地体验所需的初始化数据，来源于 `backend/envops-boot/src/main/resources/data.sql`：

- 默认体验账号：`envops-admin`
- 默认体验密码：`EnvOps@123`
- 种子任务编号：`DT202604151600000001`

这些默认数据适用于本地体验与联调环境，若用于更长期的共享环境，建议在落地时自行调整。

## 测试与验证

- 后端：`cd backend && mvn test`
- 前端：`cd frontend && pnpm test:unit && pnpm typecheck`
- `deploy/task` 页面浏览器级 smoke 已通过
- 种子任务 `DT202604151600000001` 可查看详情、主机明细、执行日志

## 发布说明

`0.0.1-beta` 目前适合作为首个公开预览版本，对外重点说明以下事实：

- 已具备前后端分离的基础运行形态
- 已形成 deploy/task 相关的核心浏览、执行与回滚链路
- 已完成一轮与登录密码规则、i18n 字面量 `@` 相关问题的修复与回归测试补齐
- 前端生产环境 API 接入方式仍需在正式部署前确认

## 已知限制

- 当前仍是 beta，不代表完整生产版能力
- 生产部署前仍需确认前端访问后端 API 的方式
- `envops-architecture-full.md` 与当前 `docs/` 不属于本次 beta 对外发布内容

## Roadmap

- 完善资产与应用管理闭环
- 增强任务执行与回滚能力
- 收敛对外文档与部署指南

## License

- `frontend/` 保留其现有 LICENSE
- 仓库级 License 策略按后续正式开源整理统一

## 非本次 beta 对外发布内容

以下内容当前不属于 `0.0.1-beta` 对外发布内容：

- `envops-architecture-full.md`
- `docs/`

如需了解当前可发布范围，请以本 README 为准。