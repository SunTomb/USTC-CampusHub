# CampusHub Phase 9 钱包账本、余额冻结与交易资金闭环设计

日期：2026-05-23

## 1. 背景与目标

Phase 8 已完成 CampusHub 与 API-Transfer-Station 支付中心的内部支付订单、回调幂等、服务费与身份保证金支付监控能力。Phase 9 在此基础上扩展平台资金模型：把当前展示型钱包升级为可支撑充值、提现申请、余额冻结、线上交易托管、线下交易服务费规则和运营审核的资金闭环。

本阶段目标是让 CampusHub 具备“平台余额 + 资金冻结 + 确认后划转”的基础能力，同时保持课程项目可控、生产部署低影响、支付密钥边界清晰。真实支付宝/微信支付渠道仍由 API-Transfer-Station 支付中心承担；CampusHub 不直接读取、复制、保存或打印支付宝私钥、公钥、支付 token、SMTP 密码、JWT secret 或服务器 `.env` 正文。

Phase 9 是一个偏大的阶段，采用一个主系统、四个可验证子模块的方式落地：

1. 钱包账本底座；
2. 充值与提现申请；
3. 二手交易线上托管与余额冻结；
4. 线下交易服务费规则与运营监控。

## 2. 范围与非目标

### 2.1 本阶段范围

- 增强钱包账户：可用余额、冻结余额、账户状态、更新时间和资金变动方法；
- 增强钱包流水：充值、提现、冻结、解冻、托管划转、服务费、保证金、人工调整等流水类型；
- 新增充值订单：支付宝实时到账并收取 0.6% 渠道手续费，微信充值进入人工审核；
- 新增提现申请：用户提交，管理员审核、完成或拒绝；
- 新增线上托管订单基础：优先接入二手 `GoodsOrder`，买方余额冻结，确认完成后划转给卖方；
- 新增线下交易服务费规则：金额小于 50 元不收费，50 元及以上收取 1% 服务费，上限 2 元；
- 新增线上托管服务费规则：不论金额多少收取 1% 服务费，上限 3 元；
- 新增管理端钱包运营工作台：充值审核、提现审核、托管订单、异常资金状态、钱包流水查询；
- 前端钱包页升级：余额总览、充值、提现、冻结明细、账本流水和托管订单状态；
- 更新 README、`CLAUDE.md` handoff 和生产验证清单。

### 2.2 非目标

Phase 9 不做：

- CampusHub 直连支付宝 SDK 或读取支付宝密钥；
- API-Transfer-Station 内部改造；
- 自动微信提现或支付宝转账打款通道；
- 完整财务清结算、发票、税务、会计级对账；
- 跑腿任务、店铺服务订单的完整托管交易深接入；
- 复杂争议仲裁资金裁决系统；
- 全量 JWT/RBAC 安全硬化；
- 多币种、多学校、多商户结算体系。

跑腿和店铺只保留后续可接入的资金模型与 UI 文案，不在 Phase 9 强行改造全部业务状态机。

## 3. 资金边界

CampusHub 负责：

- 维护用户钱包账户、可用余额、冻结余额和流水；
- 创建充值、提现、托管和服务费业务记录；
- 调用 Phase 8 的支付订单能力为支付宝充值创建支付中心订单；
- 接收支付中心内部回调后，幂等入账；
- 对微信充值、提现申请、异常托管订单做后台运营审核；
- 在二手线上交易中冻结买方余额、完成后划转给卖方、取消时解冻；
- 计算和记录线下/线上服务费。

API-Transfer-Station 支付中心负责：

- 真实支付宝/微信等渠道配置；
- 支付渠道签名、验签、外部通知处理；
- 生成真实收款链接、二维码或跳转参数；
- 将支付结果转换为 CampusHub 内部回调。

CampusHub 与支付中心之间继续复用 Phase 8 的 payment provider、payment order 和 callback 机制。新增充值业务的 `businessType` 使用 `WALLET_RECHARGE`，不得把真实渠道密钥复制进 CampusHub。

## 4. 核心领域模型

### 4.1 钱包账户 `wallet_accounts`

现有字段 `balance`、`frozen_balance` 和 `status` 保留，但语义明确化：

- `balance`：可用余额，可用于支付、提现或冻结；
- `frozen_balance`：已冻结但尚未划转/解冻的余额；
- `status`：`ACTIVE`、`FROZEN`、`DISABLED`；
- `updated_at`：账户最近资金变动时间。

后端新增集中式 `WalletService`，所有余额变更只能通过它完成。`PaymentService`、`GoodsService`、提现审核、充值回调都调用 `WalletService`，避免直接修改余额或只写流水不改余额。

关键方法：

- `credit(userId, amount, businessType, businessId, remark)`：增加可用余额；
- `debit(userId, amount, businessType, businessId, remark)`：扣减可用余额；
- `freeze(userId, amount, businessType, businessId, remark)`：可用余额转冻结余额；
- `unfreeze(userId, amount, businessType, businessId, remark)`：冻结余额转回可用余额；
- `transferFrozen(fromUserId, toUserId, amount, businessType, businessId, remark)`：扣减一方冻结余额并增加另一方可用余额；
- `recordServiceFee(userId, amount, businessType, businessId, remark)`：服务费扣减或生成待支付记录。

每个方法必须校验金额为正、账户存在且可用、余额足够、业务流水幂等键未重复。

### 4.2 钱包流水 `wallet_flows`

现有 `wallet_flows` 扩展为真实账本流水。建议新增字段：

- `flow_type`：`RECHARGE`、`WITHDRAW`、`FREEZE`、`UNFREEZE`、`ESCROW_TRANSFER_OUT`、`ESCROW_TRANSFER_IN`、`SERVICE_FEE`、`ROLE_DEPOSIT`、`MANUAL_ADJUST`；
- `available_balance_after`：本次变动后的可用余额；
- `frozen_balance_after`：本次变动后的冻结余额；
- `idempotency_key`：业务幂等键，唯一；
- `counterparty_user_id`：托管划转对手方，可为空；
- `created_by`：`USER`、`ADMIN`、`SYSTEM`、`PAYMENT_CALLBACK`；
- `operator_id`：管理员操作人，可为空。

旧字段 `direction` 继续保留以降低迁移风险，但新逻辑优先使用 `flow_type` 表达业务含义。

## 5. 充值设计

### 5.1 充值订单 `wallet_recharge_orders`

新增充值订单表，字段建议：

- `id`；
- `recharge_no`，唯一；
- `user_id`；
- `channel`：`ALIPAY`、`WECHAT`；
- `amount`：用户希望到账金额；
- `channel_fee`：渠道手续费；
- `pay_amount`：实际需支付金额；
- `status`：`PENDING_PAYMENT`、`PENDING_REVIEW`、`PAID`、`REJECTED`、`FAILED`、`EXPIRED`；
- `payment_order_no`：关联 Phase 8 `payment_orders`，微信人工审核可为空；
- `review_note`、`reviewer_id`、`reviewed_at`；
- `created_at`、`updated_at`。

### 5.2 支付宝充值

支付宝充值实时到账，收取 0.6% 手续费：

- `channel_fee = amount * 0.006`，按分精度向上或四舍五入到 2 位，实施计划需固定一种规则；
- `pay_amount = amount + channel_fee`；
- 创建 `wallet_recharge_orders`，状态 `PENDING_PAYMENT`；
- 创建 Phase 8 `payment_orders`，`businessType = WALLET_RECHARGE`，`amount = pay_amount`；
- 支付中心回调 `PAID` 后，充值订单标记 `PAID`；
- `WalletService.credit` 给用户增加 `amount` 可用余额；
- `channel_fee` 记录在充值订单和运营统计中，不作为用户余额入账。

### 5.3 微信充值

微信充值不收手续费，但需要人工审核：

- 用户提交充值金额和备注，状态 `PENDING_REVIEW`；
- 管理员在钱包运营工作台审核；
- 通过后 `WalletService.credit` 增加用户可用余额，状态 `PAID`；
- 拒绝则状态 `REJECTED`，记录原因；
- Phase 9 不做微信真实渠道支付或自动到账。

## 6. 提现设计

### 6.1 提现申请 `wallet_withdrawal_requests`

新增提现申请表，字段建议：

- `id`；
- `withdrawal_no`，唯一；
- `user_id`；
- `amount`；
- `channel`：`ALIPAY`、`WECHAT`、`BANK_CARD`、`OFFLINE`；
- `account_snapshot`：脱敏收款账号摘要，例如微信/支付宝昵称或尾号，不保存敏感完整凭证；
- `status`：`PENDING_REVIEW`、`APPROVED`、`COMPLETED`、`REJECTED`、`CANCELED`；
- `review_note`、`reviewer_id`、`reviewed_at`；
- `completed_at`；
- `created_at`、`updated_at`。

### 6.2 提现流程

提现采用“申请时冻结，完成时扣除”的模型：

1. 用户提交提现申请；
2. `WalletService.freeze` 冻结提现金额，状态 `PENDING_REVIEW`；
3. 管理员审核通过后状态 `APPROVED`；
4. 管理员确认线下/人工打款完成后，`WalletService` 扣减冻结余额，状态 `COMPLETED`；
5. 管理员拒绝或用户取消时，`WalletService.unfreeze` 退回可用余额；
6. Phase 9 不做自动打款接口。

提现申请、审核、完成和拒绝都应写入管理员操作日志或钱包运营审计记录。

## 7. 线上托管交易设计

### 7.1 首个接入点：二手 `GoodsOrder`

Phase 9 的线上托管优先接入二手交易，因为当前已有 `GoodsOrder` 的 `amount`、`serviceFee`、`paidAt`、`completedAt`、`canceledAt` 等字段。新流程不强行替换所有线下联系逻辑，而是在二手详情/意向流程中新增“线上托管交易”选项。

建议新增或扩展字段：

- `trade_mode`：`OFFLINE`、`ONLINE_ESCROW`；
- `escrow_status`：`NONE`、`PENDING_FREEZE`、`FROZEN`、`RELEASED`、`UNFROZEN`、`CANCELED`、`DISPUTED`；
- `buyer_id`、`seller_id`；
- `escrow_amount`；
- `platform_service_fee`；
- `escrow_frozen_at`、`escrow_released_at`、`escrow_unfrozen_at`；
- `cancel_reason`、`dispute_reason`。

### 7.2 线上托管状态机

推荐状态流：

1. 买卖双方形成 `GoodsOrder`，`trade_mode = ONLINE_ESCROW`；
2. 买方确认使用余额托管，系统计算 `escrow_amount = 商品金额`，`platform_service_fee = min(金额 * 1%, 3 元)`；
3. 系统从买方可用余额冻结 `escrow_amount + platform_service_fee`；
4. 冻结成功后 `escrow_status = FROZEN`，订单进入待线下交付/验收；
5. 买方确认交易成功后：
   - 冻结本金 `escrow_amount` 划转给卖方；
   - 冻结服务费作为平台服务费记录或平台收入流水；
   - `escrow_status = RELEASED`；
6. 交易取消且未争议时：
   - 解冻本金和服务费；
   - `escrow_status = UNFROZEN` 或 `CANCELED`；
7. 进入争议时：
   - 冻结资金保持不动；
   - `escrow_status = DISPUTED`；
   - Phase 9 只提供管理员可见和手工处理入口，不做复杂仲裁自动化。

### 7.3 资金流示例

商品金额 80 元：

- 线上托管服务费 = min(80 * 1%, 3) = 0.80 元；
- 买方冻结 80.80 元；
- 确认完成后，80 元划转给卖方，0.80 元作为平台服务费；
- 买卖双方各生成对应钱包流水，平台服务费生成 `ServiceFeeRecord` 或独立平台收入流水。

商品金额 500 元：

- 线上托管服务费 = min(500 * 1%, 3) = 3 元；
- 买方冻结 503 元；
- 完成后 500 元给卖方，3 元归平台服务费。

## 8. 线下交易服务费规则

线下交易指用户自行添加微信/QQ 进行收付款，CampusHub 只提供撮合、信用、联系与运营治理。Phase 9 新增明确服务费规则：

- 交易金额 `< 50` 元：不收平台服务费；
- 交易金额 `>= 50` 元：收取 1% 服务费，上限 2 元；
- 服务费由卖方还是买方承担可在实施计划中确定，建议 Phase 9 默认由发布者/卖方承担，并在 UI 明示；
- 服务费生成 `ServiceFeeRecord`，可通过 Phase 8 支付订单支付，也可从余额扣减；
- 线下交易服务费不代表 CampusHub 托管本金。

建议新增 `FeePolicyService`，统一计算：

- `calculateOfflineTradeFee(amount)`；
- `calculateOnlineEscrowFee(amount)`；
- `calculateAlipayRechargeFee(amount)`。

业务服务不得各自硬编码费率，便于后续运营调整。

## 9. 后端 API 设计

### 9.1 用户钱包 API

- `GET /api/wallet/users/{userId}`：返回余额、冻结余额、账户状态；
- `GET /api/wallet/users/{userId}/flows`：支持类型、时间范围、业务类型筛选；
- `GET /api/wallet/users/{userId}/frozen-items`：冻结明细；
- `POST /api/wallet/users/{userId}/recharges`：创建充值订单；
- `GET /api/wallet/users/{userId}/recharges`：充值记录；
- `POST /api/wallet/users/{userId}/withdrawals`：创建提现申请；
- `GET /api/wallet/users/{userId}/withdrawals`：提现记录。

### 9.2 二手托管 API

- `POST /api/goods/orders/{orderId}/escrow/freeze`：买方冻结余额；
- `POST /api/goods/orders/{orderId}/escrow/confirm`：买方确认完成并释放给卖方；
- `POST /api/goods/orders/{orderId}/escrow/cancel`：取消并解冻；
- `POST /api/goods/orders/{orderId}/escrow/dispute`：进入争议；
- `GET /api/goods/orders/{orderId}/escrow`：查询托管状态。

如现有 `GoodsOrder` 缺少创建线上订单的用户侧入口，实施计划应先补 `POST /api/goods/{goodsId}/orders` 或从已存在 `GoodsIntent` 转换为订单的接口。

### 9.3 管理端钱包运营 API

- `GET /api/admin/wallet/recharges`：充值订单列表；
- `POST /api/admin/wallet/recharges/{id}/approve`：通过微信充值；
- `POST /api/admin/wallet/recharges/{id}/reject`：拒绝微信充值；
- `GET /api/admin/wallet/withdrawals`：提现申请列表；
- `POST /api/admin/wallet/withdrawals/{id}/approve`：审核通过；
- `POST /api/admin/wallet/withdrawals/{id}/complete`：确认打款完成；
- `POST /api/admin/wallet/withdrawals/{id}/reject`：拒绝并解冻；
- `GET /api/admin/wallet/escrows`：托管订单列表；
- `GET /api/admin/wallet/flows`：钱包流水查询；
- `GET /api/admin/wallet/fee-policies`：查看费率规则。

管理端不得显示 token、secret、完整支付 URL、支付宝密钥、服务器环境变量或用户完整敏感账号。

## 10. 前端设计

### 10.1 钱包页 `/wallet`

钱包页升级为多区域：

1. 余额总览：可用余额、冻结余额、账户状态；
2. 充值入口：支付宝充值、微信人工充值；
3. 提现入口：提现申请、当前状态；
4. 冻结明细：托管交易、提现冻结、异常冻结；
5. 账本流水：按类型筛选；
6. 服务费与保证金支付：保留 Phase 8 能力。

移动端优先使用卡片和时间线，不依赖宽表格。充值、提现、冻结确认必须有明确风险文案、加载状态、幂等提示和操作后状态刷新。

### 10.2 二手交易页面

二手商品详情和订单/意向流程新增交易方式提示：

- 线下交易：展示“自行微信/QQ 转账，金额小于 50 元免平台服务费，50 元及以上按 1% 收取，上限 2 元”；
- 线上托管：展示“从买方余额冻结本金与服务费，确认完成后划转给卖方”；
- 余额不足时引导充值；
- 托管状态用时间线展示：待冻结、已冻结、待确认、已完成、已取消、争议中。

### 10.3 管理端 `/admin/wallet`

新增钱包运营工作台，建议独立路由 `/admin/wallet`，而不是继续塞进 `/admin/payment`：

- 充值审核 tab；
- 提现审核 tab；
- 托管订单 tab；
- 钱包流水 tab；
- 费率规则 tab；
- 异常状态提示卡。

`/admin/payment` 继续负责 Phase 8 支付订单和回调事件；`/admin/wallet` 负责平台余额账本和运营动作。

## 11. 数据一致性与幂等

Phase 9 涉及真实余额变动，必须强化一致性：

- 所有资金操作在数据库事务内完成；
- 账户余额变更方法应使用行级锁或等价的 JPA pessimistic lock 查询；
- 每个钱包流水有唯一 `idempotency_key`；
- 支付中心回调重复到达不得重复入账；
- 提现审核重复点击不得重复冻结/扣减；
- 托管释放重复点击不得重复划转；
- 余额不足必须阻止冻结、提现和扣费；
- 金额计算统一使用 `BigDecimal`，固定 2 位小数规则；
- 管理端人工操作必须写审计记录。

## 12. 治理与风险控制

Phase 5 的用户限制能力应扩展到钱包动作：

- 账号被 `ACCOUNT_DISABLED` 或钱包状态 `DISABLED` 时，禁止充值、提现、托管冻结和确认；
- 服务冻结或交易冻结状态下，禁止线上托管新订单；
- 争议中的托管订单不允许用户自行释放或解冻；
- 管理端可查看异常订单，但 Phase 9 不做复杂自动裁决；
- 提现申请需展示用户信用分、违规记录摘要和近期异常流水。

如现有治理限制枚举不足，Phase 9 可新增钱包相关 restriction reason，但不要开启全量 RBAC 重构。

## 13. 数据库迁移

生产已应用 V1-V11，Phase 9 必须新增 V12+，不得编辑旧迁移。

建议新增 `V12__wallet_escrow_upgrade.sql`，包含：

1. 扩展 `wallet_accounts`：`updated_at`；
2. 扩展 `wallet_flows`：`flow_type`、`available_balance_after`、`frozen_balance_after`、`idempotency_key`、`counterparty_user_id`、`created_by`、`operator_id`；
3. 新增 `wallet_recharge_orders`；
4. 新增 `wallet_withdrawal_requests`；
5. 新增 `wallet_frozen_records` 或 `wallet_escrow_orders`，用于冻结明细；
6. 扩展 `goods_orders`：交易方式、托管状态、买方/卖方、托管金额、服务费、状态时间戳；
7. 可选新增 `fee_policy_snapshots`，用于记录当时使用的费率规则。

如实施中发现 V12 过大，可拆为 V12 钱包账本、V13 托管订单，但仍不得修改 V1-V11。

## 14. 实施分层

后端建议按以下包组织：

- `wallet`：账户、流水、充值、提现、冻结记录、`WalletService`、用户/管理端钱包 API；
- `payment`：继续负责支付订单、支付中心 provider、回调；新增 `WALLET_RECHARGE` 业务回调处理；
- `goods`：只负责二手交易订单与托管状态，不直接修改余额；
- `ops` 或 `wallet` 管理端：钱包运营工作台；
- `moderation/governance`：提供限制检查，不与钱包账本耦合。

前端建议新增：

- `frontend/src/views/WalletView.vue`：升级用户钱包中心；
- `frontend/src/views/AdminWalletView.vue`：新增钱包运营工作台；
- `frontend/src/components/wallet/`：`MoneyAmount`、`LedgerTable`、`RechargeDialog`、`WithdrawDialog`、`EscrowTimeline`、`FeeRuleBadge`；
- `frontend/src/api/campushub.ts`：新增充值、提现、冻结、托管和管理端类型/函数。

## 15. 验证计划

### 15.1 后端验证

优先在服务器低影响 Docker build 或可用 Maven 环境中验证：

- 钱包充值：支付宝充值支付回调成功后只入账一次；
- 微信充值：管理员通过后入账，拒绝不入账；
- 提现：申请冻结余额，拒绝解冻，完成扣减冻结余额；
- 线上托管：余额不足冻结失败；冻结成功后可用余额减少、冻结余额增加；确认完成后卖方余额增加；取消后买方余额恢复；
- 服务费规则：线下 `<50` 为 0，线下 `>=50` 为 1% 且封顶 2，线上为 1% 且封顶 3；
- 幂等：重复回调、重复提现审核、重复托管释放不会重复写流水；
- 回归：Phase 8 服务费/角色保证金支付仍可用。

### 15.2 API smoke

部署后低频检查：

- `/api/wallet/users/1`；
- `/api/wallet/users/1/flows`；
- `/api/admin/wallet/recharges`；
- `/api/admin/wallet/withdrawals`；
- `/api/admin/wallet/escrows`；
- `/api/admin/payment/orders`；
- `/api/goods`、`/api/tasks`、`/api/shops`、`/api/project-ads`；
- `/api/admin/ops/analytics/overview`。

### 15.3 Playwriter 验证

桌面端：

- `/wallet` 余额、充值、提现、冻结明细、流水；
- `/goods` 和商品详情的线下/线上交易提示；
- `/admin/wallet` 充值、提现、托管、流水 tabs；
- `/admin/payment` 仍显示支付订单和回调；
- `/admin/ops`、`/admin/governance`、`/tasks`、`/shops`、`/project-ads` 回归渲染。

移动端 390x844：

- `/wallet` 无 document-level 横向溢出；
- `/admin/wallet` 主要 tabs 可访问，表格有卡片或安全横向滚动；
- 充值/提现弹窗在窄屏可完成输入、取消和提交。

## 16. 部署与运维约束

- 不本地安装依赖，除非用户明确批准；
- 完整构建优先使用服务器低影响 Docker build；
- 小服务器上避免频繁全量 rebuild 和高频 polling；
- 不读取或打印服务器 `.env`、支付 token、SMTP 密码、JWT secret、支付宝密钥；
- `.env.prod.example` 只能出现占位符；
- 如生产支付中心未配置充值真实收款，只验证安全失败或 mock/staging token 模式，不伪造生产成功状态；
- 变更应分批提交，每批有明确可回滚边界。

## 17. Phase 10+ 预留

Phase 9 完成后，后续阶段可继续：

- 跑腿任务报酬托管；
- 店铺服务订单在线托管；
- 提现自动打款通道；
- 对账报表和导出；
- 更严格认证授权；
- 争议仲裁和冻结资金人工裁决；
- 平台收入统计和运营风控。

这些能力不应提前塞入 Phase 9，以免单阶段不可验证。

## 18. 自检

- 范围：设计覆盖用户要求的充值、冻结托管、提现和线下费规则，但拆成钱包底座、充值提现、二手托管、服务费规则四个可验证子模块。
- 边界：明确真实支付渠道仍由 API-Transfer-Station 处理，CampusHub 不读取或保存支付宝密钥，不做 API-Transfer-Station 内部改造。
- 数据库：明确生产 V1-V11 不可修改，Phase 9 使用 V12+。
- 一致性：所有余额变更集中到 `WalletService`，要求事务、幂等键、余额校验和重复操作防护。
- 规模控制：完整 Phase 9 偏大，但首个托管接入点限定为二手 `GoodsOrder`，跑腿和店铺只预留，不强行深改。
- 运维：验证和部署遵守小服务器低影响原则，不读取服务器 secret。
