# EnvOps Traffic MVP 设计

## 1. 背景与目标

当前 EnvOps 的 Traffic 模块已经具备页面、后端接口、策略数据和插件抽象，但整体仍停留在 skeleton / not-ready 阶段。前端页面可以展示 policy、plugin 和 `preview / apply / rollback` 动作，后端也暴露了对应接口，但两个插件 `NGINX` 与 `REST` 仍是 `NOT_IMPLEMENTED`，导致对外只能承认占位能力，不能承认真实可执行的切流能力。

这使 Traffic 成为当前产品最明显的断层：用户能看到像是真功能的页面和动作，却不能把它当成真实能力使用。

本次 `v0.0.5` 的目标是把 Traffic 从 skeleton / not-ready 收敛成一条最小但真实可用的链路：

- 只支持一个插件通路：`REST`
- 只支持一个策略：`weighted_routing`
- 只支持单条 policy 的人工触发动作：`preview`、`apply`、`rollback`
- 三个动作都必须真实调用外部 REST 流量服务
- 只有外部调用成功，EnvOps 才更新 policy 状态和 `rollbackToken`
- 页面、测试、文档和对外口径全部与这个 MVP 范围一致

## 2. 用户确认的范围边界

### 2.1 本次纳入范围

- Traffic 插件只实现 `REST`
- Traffic 策略只支持 `weighted_routing`
- `preview / apply / rollback` 都必须真实调用外部 REST 流量服务
- EnvOps 使用通用 REST 适配器方式，不直接绑定某个固定厂商协议
- 插件配置通过后端配置文件 / 环境变量提供，不做数据库化管理
- 页面从“not-ready 占位”改成“有限可用的 Traffic MVP”
- 前端要明确展示按钮可用条件、失败原因和最近动作结果
- 文档、验证命令和 release materials 要同步更新

### 2.2 明确不做的范围

- 多插件并发支持
- 多策略矩阵支持
- `blue_green`、`header_canary` 的真实落地
- 审批体系重构
- 批量切流
- 高级流量编排
- 灰度分析报表
- 流量事件历史中心
- 网关厂商专用对接抽象
- 每条 policy 独立插件配置

## 3. 设计结论

本次采用 **REST 通用适配器 + weighted_routing 单策略 + 真三动作闭环** 方案。

### 3.1 为什么选这个方案

与其他可选方案相比：

1. **优于直接绑定某个现有流量平台**
   - 保持 EnvOps 的接口和能力边界稳定
   - 不把后续扩展完全绑死在一套厂商 API 上

2. **优于 EnvOps 内置伪外部流量服务**
   - 能真正承诺“真实切流能力”
   - 避免再次出现“像是真的，但实际是模拟”的口径问题

3. **符合最小可交付原则**
   - 只做一个插件、一个策略、一条闭环
   - 先把最小真实能力做实，而不是扩展成流量平台

### 3.2 核心原则

- 只有外部 REST 服务返回成功，EnvOps 才更新 policy 状态
- EnvOps 不再伪造 `rollbackToken`
- `apply` 成功但没有拿到 `rollbackToken`，整体算失败
- `rollback` 只能使用外部服务返回的最近一次有效 `rollbackToken`
- 任何失败都不更新 policy 状态
- 页面和文档必须明确说明当前只支持 `REST + weighted_routing`

## 4. 当前实现现状与需要收口的问题

### 4.1 当前已有能力

当前仓库已经具备以下基础：

- 前端 Traffic 页面与动作按钮
- `/api/traffic/policies`、`/api/traffic/plugins`、`preview`、`apply`、`rollback` 接口
- `TrafficApplicationService` 编排入口
- `TrafficPlugin` 抽象
- `RestTrafficPlugin` 与 `NginxTrafficPlugin` skeleton 实现
- `traffic_policy` 表与测试种子数据
- `TrafficControllerTest` 基础认证和 not-ready 测试

### 4.2 当前问题

- `REST` 与 `NGINX` 插件都返回 `NOT_IMPLEMENTED`
- 页面还在展示 not-ready warning
- 种子数据混用了 `header_canary`、`blue_green`、`weighted_routing`
- 当前 `rollbackToken` 存在伪造逻辑，`apply` 成功时可自动生成 `traffic-rb-{id}`
- 后端没有真实外部 REST 调用、超时治理和失败语义统一

## 5. 总体架构

本次 Traffic MVP 的真实链路如下：

1. 用户在 Traffic 页面选择一条 policy
2. 前端根据 policy 的 `pluginType`、`strategy`、`rollbackToken` 和插件 readiness 决定按钮是否可用
3. 用户点击 `preview` / `apply` / `rollback`
4. 前端调用 EnvOps 后端 Traffic 接口
5. `TrafficApplicationService` 校验 policy 是否处于本次支持范围
6. `RestTrafficPlugin` 按统一的最小契约调用外部 REST 流量服务
7. 外部服务返回成功结果
8. EnvOps 回写 policy 状态与 `rollbackToken`
9. 前端刷新列表并展示最近动作结果

这个架构保持现有模块边界，只把 skeleton 换成真实实现，不扩展为更大的子系统。

## 6. 外部 REST 最小契约

EnvOps 不直接理解具体厂商网关规则，只依赖统一的最小 REST 契约。

### 6.1 Preview 接口

- 方法：`POST {baseUrl}/traffic/policies/preview`
- 认证：`Authorization: Bearer <token>`

请求体：

```json
{
  "app": "checkout-gateway",
  "strategy": "weighted_routing",
  "scope": "prod / cn-beijing-b",
  "trafficRatio": "10%",
  "owner": "platform-team"
}
```

成功响应：

```json
{
  "success": true,
  "message": "preview accepted",
  "rollbackToken": "rb-preview-123"
}
```

### 6.2 Apply 接口

- 方法：`POST {baseUrl}/traffic/policies/apply`
- 认证：`Authorization: Bearer <token>`

请求体与 preview 相同。

成功响应：

```json
{
  "success": true,
  "message": "traffic rule applied",
  "rollbackToken": "rb-apply-456"
}
```

### 6.3 Rollback 接口

- 方法：`POST {baseUrl}/traffic/policies/rollback`
- 认证：`Authorization: Bearer <token>`

请求体：

```json
{
  "app": "checkout-gateway",
  "rollbackToken": "rb-apply-456",
  "reason": "manual rollback"
}
```

成功响应：

```json
{
  "success": true,
  "message": "rollback applied"
}
```

### 6.4 EnvOps 只要求的字段

为了保持 MVP 最小，EnvOps 只依赖：

- `success`
- `message`
- `rollbackToken`（preview 可选，apply 必需，rollback 响应可省略）

本次不引入更重的流量 DSL、阶段化回滚模型或多步审批字段。

## 7. 后端设计

### 7.1 应用层职责

#### `TrafficApplicationService`

继续作为编排层，负责：

- 读取 policy
- 校验 policy 是否在 MVP 支持范围内
- 校验 plugin readiness
- 调用对应 plugin
- 按结果决定是否回写状态和 `rollbackToken`

它不负责直接拼接 HTTP 请求细节。

#### `RestTrafficPlugin`

从 skeleton 改成真实实现，负责：

- 调用外部 REST 流量服务
- 携带 Bearer token
- 处理超时、非 2xx、业务失败
- 返回统一的 `TrafficPluginResult`

#### `TrafficRestPluginProperties`

新增配置对象，负责：

- `baseUrl`
- `token`
- `connectTimeoutMs`
- `readTimeoutMs`

插件 readiness 基于配置完整度和运行条件判断，而不是写死常量字符串。

#### `TrafficRestClient`

新增一个很薄的 HTTP 适配层，负责发请求和解析响应，避免 `RestTrafficPlugin` 同时承担协议编排和 HTTP 细节。

### 7.2 配置项设计

使用后端配置 / 环境变量管理插件配置，不做数据库化管理：

```yaml
envops:
  traffic:
    rest:
      base-url: ${ENVOPS_TRAFFIC_REST_BASE_URL:}
      token: ${ENVOPS_TRAFFIC_REST_TOKEN:}
      connect-timeout-ms: ${ENVOPS_TRAFFIC_REST_CONNECT_TIMEOUT_MS:3000}
      read-timeout-ms: ${ENVOPS_TRAFFIC_REST_READ_TIMEOUT_MS:5000}
```

设计原则：

- `baseUrl` 或 `token` 缺失时，插件视为 not ready
- 固定使用 Bearer token 认证
- 不做多 endpoint 配置
- 不做多租户配置
- 不做每条 policy 独立配置

### 7.3 支持规则

本次不改 `traffic_policy` 表结构，直接使用现有字段决定是否受支持：

- `plugin_type != REST`，拒绝动作
- `strategy != weighted_routing`，拒绝动作
- 只有同时满足这两个条件，才允许执行动作

这意味着旧数据和旧策略可以继续保留，但只有符合 MVP 边界的数据是“真能力”。

### 7.4 状态与回写规则

#### preview 成功

- 调用外部 preview 接口成功
- policy 状态回写为 `PREVIEW`
- 如果外部响应返回 `rollbackToken`，则保存
- 如果外部未返回 `rollbackToken`，不强求，但仍可成功

#### apply 成功

- 调用外部 apply 接口成功
- 外部响应必须返回 `rollbackToken`
- policy 状态回写为 `ENABLED`
- 保存返回的 `rollbackToken`
- 如果 apply 成功但没有 token，整体视为失败，不更新状态

#### rollback 成功

- 当前 policy 必须已有 `rollbackToken`
- 调用外部 rollback 接口成功
- policy 状态回写为 `ROLLED_BACK`
- 保留最近一次成功的 `rollbackToken`

#### 失败统一规则

- 不更新 policy 状态
- 不伪造 `rollbackToken`
- 不写入伪成功结果

### 7.5 失败语义

统一失败场景：

- policy 不存在，返回 `404`
- `pluginType != REST`，返回 `400`
- `strategy != weighted_routing`，返回 `400`
- `baseUrl` 或 `token` 缺失，返回 `400`
- 外部服务连接失败、超时、5xx，返回 `502`
- 外部服务返回业务失败，返回 `400`，并透传可解释 message
- rollback 时缺少 `rollbackToken`，返回 `400`

最重要的要求：

- **任何失败都不允许更新 policy 状态**

### 7.6 rollbackToken 语义修正

本次必须删除当前自动伪造 token 的逻辑。

新的语义：

- token 来源只认外部流量服务成功返回
- EnvOps 不再自动生成 `traffic-rb-{id}`
- apply 成功但没有 token，按失败处理
- rollback 只使用最近一次成功 apply 产出的 token

## 8. 前端设计

### 8.1 页面定位调整

Traffic 页面从“not-ready 占位页”调整为“有限可用的 Traffic MVP 页面”：

- 只支持 `REST`
- 只支持 `weighted_routing`
- 支持人工触发 `preview / apply / rollback`
- 对不在支持范围内的 policy 清楚标记为“当前版本不支持”

### 8.2 列表展示要求

在现有表格结构基础上补充可解释性信息：

- 展示或间接表达 pluginType
- 展示是否受当前 MVP 支持
- 展示 rollbackToken 是否存在
- 顶部 latest action 区块既能显示成功，也能显示最近失败摘要

本次不要求重做整页布局，优先复用当前表格和 summary 结构。

### 8.3 按钮可用规则

#### Preview 按钮可用条件

必须同时满足：

- `pluginType = REST`
- `strategy = weighted_routing`
- REST 插件配置完整
- 当前没有动作执行中

#### Apply 按钮可用条件

必须同时满足：

- `pluginType = REST`
- `strategy = weighted_routing`
- REST 插件配置完整
- 当前没有动作执行中

本次 **不强制要求先 preview 再 apply**，避免引入额外流程复杂度。

#### Rollback 按钮可用条件

必须同时满足：

- `pluginType = REST`
- `strategy = weighted_routing`
- 已有 `rollbackToken`
- 当前没有动作执行中

### 8.4 结果反馈要求

#### 成功

- 保留 `window.$message.success`
- 保留顶部 latest action 区块
- 刷新列表，展示最新状态与 token 变化

#### 失败

页面必须明确展示失败原因，例如：

- 插件配置缺失
- 当前策略不支持
- rollback token 缺失
- 外部服务超时
- 外部服务返回业务失败

页面同时需要：

- `window.$message.error(message)`
- 顶部 latest action 区块展示最近失败摘要

### 8.5 文案收口

当前页面里的 `notReadyWarning` 应改成 MVP 范围提示，文案需要明确：

- 当前仅支持 REST 插件
- 当前仅支持 weighted routing
- 当前只支持手动 preview / apply / rollback
- `NGINX`、`blue_green`、`header_canary` 仍不在当前版本范围内

页面不再表达“Traffic 全部没准备好”，而是表达“Traffic 已有限可用，但边界明确”。

## 9. 数据与种子数据策略

当前 `data.sql` 中已有三条 traffic policy，包含：

- `header_canary`
- `blue_green`
- `weighted_routing`
- `REST`
- `NGINX`

本次不删除这些数据，而是保留它们用于展示“支持与不支持并存”的真实页面状态。

为保证 MVP 演示稳定，种子数据需要保证存在：

- 一条 `REST + weighted_routing + 有 rollbackToken` 的可回滚记录
- 一条 `REST + weighted_routing + 无 rollbackToken` 的可 apply 记录
- 一条超出范围的策略或插件记录，用于展示禁用状态与边界提示

## 10. 测试设计

### 10.1 后端控制器测试

在现有 `TrafficControllerTest` 基础上补充：

- 登录后获取 policy / plugin 列表
- `REST + weighted_routing` 的 preview 成功
- apply 成功并回写 `ENABLED + rollbackToken`
- rollback 成功并回写 `ROLLED_BACK`
- 不支持策略时报错
- 不支持插件时报错
- 配置缺失时报错
- 外部服务超时报错
- 外部服务返回业务失败时报错
- 失败时 policy 状态保持不变

### 10.2 插件级测试

补充 `RestTrafficPlugin` 单测，验证：

- 请求体是否符合最小契约
- Bearer token 是否带上
- `2xx + success=true` 视为成功
- `2xx + success=false` 视为业务失败
- `4xx / 5xx / 超时` 转为统一失败语义

### 10.3 前端页面测试

补充 Traffic 页测试，覆盖：

- 只有 `REST + weighted_routing` 能点按钮
- rollback 无 token 时禁用
- 成功后刷新列表并显示最新摘要
- 失败时显示明确错误
- warning 文案已从 not-ready 改成 MVP 范围提示

### 10.4 验证命令

实施完成后执行：

- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test`
- `mvn -f backend/pom.xml -pl envops-traffic -am -Dtest=RestTrafficPluginTest test`
- `pnpm --dir frontend exec vitest run src/views/traffic/traffic-contract.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`

如页面有显著交互变化，还应手动验证：

- preview 成功
- apply 成功并拿到 rollbackToken
- rollback 成功
- 不支持策略禁用
- 不支持插件禁用
- 配置缺失时失败提示

## 11. 文档同步要求

需要同步更新以下文档，使口径从 not-ready 收敛成 MVP 有限可用：

- `README.md`
- `docs/envops-项目详细说明.md`
- `docs/envops-开发技术说明.md`
- `docs/envops-用户操作手册.md`
- `release/0.0.5-release-notes.md` 或对应发布材料

文档必须明确：

- Traffic 已支持最小真实切流能力
- 当前只支持 `REST + weighted_routing`
- 支持手动 `preview / apply / rollback`
- rollback 依赖外部服务返回的真实 `rollbackToken`
- `NGINX`、`blue_green`、`header_canary` 仍不在本次范围内

## 12. 实现原则

- 只把 Traffic 做成最小真实能力，不扩成流量平台
- 先把单插件、单策略、单链路做实
- 页面、接口、测试、文档口径必须完全一致
- 不允许出现“按钮能点但实际是 mock”的新断层
- 失败语义必须可解释，不能通过伪成功掩盖问题
