# CampusHub Phase 8 支付中心集成强化设计

日期：2026-05-23

## 1. 背景与目标

Phase 1-7 已完成 CampusHub 的四条业务线、治理信用、运营分析和响应式 Web。Phase 8 的目标是在不改变资金边界的前提下，把当前本地 mock 为主的支付能力升级为可对接 API-Transfer-Station 支付中心的生产型内部支付链路。

本阶段只强化平台服务费和身份保证金支付。CampusHub 仍不托管二手交易本金、跑腿报酬本金或店铺服务本金，不做逐单保证金冻结，也不直接持有、读取、复制、保存或打印支付宝私钥/公钥正文。

## 2. 资金与系统边界

CampusHub 负责：

- 创建本地支付意图和支付订单映射；
- 维护角色保证金、服务费记录与本地状态；
- 调用 API-Transfer-Station 支付中心内部接口创建收款单；
- 接收支付中心内部回调并做内部鉴权、幂等、金额、订单号和状态校验；
- 标记本地记录为 `PENDING`、`PAID`、`FAILED` 或 `EXPIRED`；
- 写入钱包流水、回调审计和管理员可见监控数据。

API-Transfer-Station 支付中心负责：

- 真实支付宝应用配置、支付宝私钥/公钥、支付宝签名和验签；
- 生成用户实际支付 URL、二维码或跳转参数；
- 处理支付宝异步通知；
- 将结果转换为 CampusHub 内部回调；
- 对外部支付渠道差异做适配。

CampusHub 与支付中心之间使用内部 HTTP 契约和环境变量配置。CampusHub 不访问 `/opt/ai-relay/secrets/alipay/`，不读取密钥文件正文，不把支付宝密钥字段加入配置样例。

## 3. Provider 架构

现有 `PaymentProvider` 抽象保留，但 Phase 8 将 provider 明确分为两类：

1. `mock`：本地和课程演示模式。创建模拟支付单，允许通过 mock success endpoint 完成回归测试。
2. `payment-center`：生产模式。通过内部接口调用 API-Transfer-Station 支付中心。

`AlipayPaymentProvider` 不作为生产路线继续扩展。若保留类名，只能作为弃用占位或改名为支付中心 provider，避免误导后续开发者把支付宝密钥接入 CampusHub。

Provider 选择由环境变量驱动：

- `CAMPUSHUB_PAYMENT_PROVIDER=mock|payment-center`
- `CAMPUSHUB_PAYMENT_CENTER_BASE_URL`
- `CAMPUSHUB_PAYMENT_CENTER_CREATE_PATH`
- `CAMPUSHUB_PAYMENT_CENTER_CALLBACK_TOKEN`
- `CAMPUSHUB_PAYMENT_CENTER_SIGNING_SECRET`
- `CAMPUSHUB_PAYMENT_CENTER_CALLBACK_URL`
- `CAMPUSHUB_PAYMENT_ORDER_EXPIRE_MINUTES`

缺少生产 provider 必需配置时，应用可以启动，但创建生产支付单应返回明确业务错误，不能降级为 mock，以免生产误收款状态失真。

## 4. 内部支付中心契约

### 4.1 创建支付单请求

CampusHub 调用支付中心创建收款单，建议请求字段为：

```json
{
  "app": "campushub",
  "orderNo": "CHP202605230001",
  "businessType": "SERVICE_FEE",
  "businessId": 123,
  "payerId": 2,
  "amount": "1.00",
  "subject": "CampusHub 服务费 SF202605230001",
  "callbackUrl": "https://ustc.suntomb.qzz.io/api/payment/callbacks/payment-center",
  "expireMinutes": 30
}
```

`businessType` 初始支持：

- `SERVICE_FEE`：平台服务费；
- `ROLE_DEPOSIT`：跑腿、二手发布者、店铺商家身份保证金。

### 4.2 创建支付单响应

支付中心返回：

```json
{
  "provider": "API_TRANSFER_STATION",
  "paymentCenterOrderNo": "ATS202605230001",
  "payUrl": "https://...",
  "status": "PENDING",
  "expiresAt": "2026-05-23T12:30:00"
}
```

CampusHub 保存 `paymentCenterOrderNo`、`payUrl`、provider、过期时间和状态。`payUrl` 可返回给前端，但不应出现在 CSV 导出或公开日志中。

### 4.3 支付结果回调

支付中心向 CampusHub 内部回调：

```json
{
  "eventId": "evt_202605230001",
  "orderNo": "CHP202605230001",
  "paymentCenterOrderNo": "ATS202605230001",
  "businessType": "SERVICE_FEE",
  "businessId": 123,
  "amount": "1.00",
  "status": "PAID",
  "paidAt": "2026-05-23T12:03:00",
  "failureReason": null
}
```

回调请求必须携带内部 token 或签名头。建议支持：

- `X-CampusHub-Payment-Token`：共享内部 token；
- `X-CampusHub-Payment-Signature`：基于 body 和 shared secret 的 HMAC；
- `X-CampusHub-Payment-Timestamp`：防止长期重放。

Phase 8 至少实现 token 校验和幂等；若实现签名，则签名失败必须拒绝并记录审计。文档和代码不得输出 token 或 secret。

## 5. 数据模型

生产数据库已应用 V1-V10，Phase 8 如需 schema 变更只能新增 V11+。

建议新增 `V11__payment_center_integration.sql`，包含：

### 5.1 扩展 `service_fee_records`

新增字段：

- `payment_order_no`：CampusHub 本地支付订单号；
- `payment_provider`：`MOCK` / `PAYMENT_CENTER`；
- `payment_center_order_no`：支付中心订单号；
- `pay_url`：用户支付入口；
- `expires_at`：支付过期时间；
- `failed_at`：失败时间；
- `failure_reason`：失败原因；
- `updated_at`：状态更新时间。

### 5.2 新增 `payment_orders`

用于统一承载服务费和角色保证金支付意图：

- `id`；
- `order_no`，唯一；
- `business_type`；
- `business_id`；
- `payer_id`；
- `amount`；
- `provider`；
- `provider_order_no`；
- `pay_url`；
- `status`：`PENDING`、`PAID`、`FAILED`、`EXPIRED`；
- `expires_at`、`paid_at`、`failed_at`；
- `failure_reason`；
- `created_at`、`updated_at`。

服务费记录继续作为业务源记录，`payment_orders` 作为支付层统一索引。角色保证金也通过 `payment_orders` 进入支付流程，避免把支付中心字段散落到身份申请表。

### 5.3 新增 `payment_callback_events`

用于幂等和审计：

- `id`；
- `event_id`，唯一；
- `order_no`；
- `provider_order_no`；
- `status`；
- `amount`；
- `verified`；
- `handled`；
- `failure_reason`；
- `created_at`。

不保存完整请求头中的 token 或签名。回调 body 可保存经过脱敏的摘要字段，不保存敏感凭证。

## 6. 业务流程

### 6.1 服务费支付

1. 业务流程创建 `ServiceFeeRecord`，状态为 `PENDING`。
2. 用户或前端请求创建服务费支付单。
3. `PaymentService` 查找或创建 `payment_orders`，若已有未过期 `PENDING` 订单则复用。
4. provider 为 `mock` 时返回 mock 支付信息。
5. provider 为 `payment-center` 时调用支付中心创建收款单并保存映射。
6. 支付中心回调 `PAID` 后，CampusHub 幂等标记 `payment_orders` 和 `service_fee_records` 为 `PAID`，写钱包流水。
7. 回调 `FAILED` 或过期任务标记 `FAILED` / `EXPIRED`，保留失败原因。

### 6.2 角色保证金支付

Phase 1 当前角色申请把保证金视为已支付并自动/人工审核。Phase 8 应改为更真实的支付链路：

1. 用户提交角色申请，生成或复用 `RoleApplication`，状态进入待支付或待确认支付。
2. 创建 `ROLE_DEPOSIT` 支付订单，金额来自 `PlatformRoleType`：跑腿 5 元、二手发布者 10 元、店铺商家 20 元。
3. 回调 `PAID` 后标记申请保证金已支付。
4. 跑腿和二手发布者在保证金支付成功后自动通过；店铺商家在保证金支付成功后进入人工审核。
5. 支付失败或过期不会开通身份。

如为了降低改造风险，实施计划可以先保留现有角色申请接口行为，但新增专用支付单创建与回调状态字段；最终用户体验必须清楚区分“待支付”“待审核”“已通过”。

## 7. 回调校验与幂等规则

回调处理必须按顺序校验：

1. 内部 token 或签名有效；
2. `eventId` 未处理，若已处理则直接返回成功结果；
3. `orderNo` 存在；
4. `paymentCenterOrderNo` 与本地记录匹配；
5. `businessType` 和 `businessId` 与本地订单匹配；
6. `amount` 与本地订单金额精确一致；
7. 当前状态允许流转：
   - `PENDING -> PAID`；
   - `PENDING -> FAILED`；
   - `PENDING -> EXPIRED`；
   - `PAID` 再次收到 `PAID` 视为幂等成功；
   - `PAID` 后收到失败或过期必须拒绝并记录异常。

所有拒绝原因写入 `payment_callback_events.failure_reason` 或安全审计日志，但不写入 token、secret、支付宝密钥或完整支付 URL。

## 8. 后端 API

用户侧：

- `POST /api/payment/service-fees/{feeId}/pay`：创建或复用服务费支付单；
- `GET /api/payment/orders/{orderNo}`：查询支付订单状态；
- `POST /api/identity/users/{userId}/roles/{applicationId}/deposit-pay`：为角色申请创建保证金支付单。

内部回调：

- `POST /api/payment/callbacks/payment-center`：支付中心回调入口，只接受内部 token/签名。

管理端：

- `GET /api/admin/payment/orders`：支付订单监控；
- `GET /api/admin/payment/callback-events`：回调事件审计；
- 可选：`POST /api/admin/payment/orders/{orderNo}/sync`：低频手动查询支付中心状态。若实现，必须走 payment-center provider，不读取外部密钥。

保留 mock endpoint 用于本地演示，但 UI 文案必须标明 mock 模式，仅在 `CAMPUSHUB_PAYMENT_PROVIDER=mock` 时可用。

## 9. 前端与运营 UI

用户侧支付入口保持轻量：

- 服务费记录显示“去支付”“支付中”“已支付”“失败/已过期”；
- 角色申请卡片显示保证金金额和支付状态；
- 支付创建成功后显示支付链接或二维码占位信息；
- mock 模式下显示“本地模拟支付”提示。

管理端新增支付监控页或在 `/admin/ops` 增加支付标签：

- 状态筛选：`PENDING`、`PAID`、`FAILED`、`EXPIRED`；
- 类型筛选：服务费、角色保证金；
- 时间范围筛选；
- 列表字段：本地订单号、业务类型、业务 ID、付款用户、金额、provider、支付中心订单号、状态、创建时间、过期时间、支付时间、失败原因；
- 回调事件列表显示 event id、订单号、状态、是否校验通过、是否已处理、失败原因和时间。

管理端不得显示内部 token、签名 secret、支付宝密钥或完整敏感配置。

## 10. 配置与部署

`.env.prod.example` 和 README 只记录变量名、用途和示例占位符，不记录真实值。

生产建议：

- `CAMPUSHUB_PAYMENT_PROVIDER=payment-center`；
- 支付中心 base URL 使用内网或 localhost 可达地址；
- callback URL 使用 CampusHub 后端公开 API 或反向代理路径；
- token/secret 通过 `.env` 注入；
- Docker Compose 只传递变量，不在镜像内写入 secret。

部署验证必须低影响：优先后端定向 Docker build、低频 API smoke、Playwriter 只验证页面渲染和状态展示。不得读取服务器 `.env` 或支付中心密钥文件正文。

## 11. 测试与验证

后端验证：

- mock provider 创建服务费支付单仍可用；
- payment-center provider 在配置缺失时返回明确错误；
- 服务费支付回调 `PAID` 后幂等标记服务费已支付并写钱包流水；
- 重复 event id 不重复写流水；
- 金额不匹配、订单号不匹配、状态非法流转会被拒绝并记录；
- 角色保证金支付成功后触发对应申请状态变化。

前端验证：

- 钱包/角色页面可创建支付单并展示状态；
- 管理端支付监控可筛选 pending/paid/failed/expired；
- mock 模式文案明确；
- 移动端无明显横向溢出。

生产验证：

- API smoke 只调用非破坏性查询和受控 mock/staging token 回调；
- 若支付中心真实收款未配置，只验证配置缺失时安全失败，不伪造成功状态；
- 不打印任何 secret、token、支付宝密钥或完整生产支付 URL。

## 12. 非目标

Phase 8 不做：

- CampusHub 直接接入支付宝 SDK 或读取支付宝密钥；
- 二手交易本金、跑腿报酬本金、店铺服务本金托管；
- 逐单保证金冻结/解冻；
- 自动扣罚保证金；
- 完整对账、退款、结算、发票或财务报表；
- 全量 JWT/RBAC 硬化；
- 替换 API-Transfer-Station 支付中心。

## 13. 自检

- 范围：本设计只覆盖支付中心 provider、服务费/角色保证金支付订单、回调幂等和支付监控，符合 Phase-4-sized 约束。
- 边界：明确禁止 CampusHub 读取、复制、保存或打印支付宝密钥正文，不引入交易本金托管或逐单保证金冻结。
- 数据库：生产 V1-V10 不可修改，若实施需要 schema 变更则新增 V11。
- 安全：回调 token/签名、金额、订单号、状态和幂等校验均有明确规则，敏感凭证不写入日志或导出。
- 运维：保留 mock 模式，生产 provider 由环境变量驱动，部署验证遵守小服务器低影响原则。
