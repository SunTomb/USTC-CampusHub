# CampusHub Phase 10 认证、授权与生产安全加固设计

日期：2026-05-24

## 1. 背景与目标

CampusHub 已完成 Phase 1-9：跑腿任务、二手交易、学生店铺、项目广告、治理信用、运营分析、响应式 Web、支付中心集成，以及钱包账本、充值、提现、冻结余额和二手线上托管。当前生产分支为 `phase9-wallet-escrow`，生产最终同步提交为 `23aa646`。

原总体路线图中的 Phase 9 是 “Authentication, authorization, and production security hardening”。由于实际 Phase 9 已调整为钱包/托管资金，本设计将认证授权安全加固作为新的 Phase 10；原 Beta launch readiness / operational runbook 顺延为 Phase 11。

Phase 10 的目标是把 CampusHub 从课程原型的开放 API 推进到可控 Beta 生产状态：公共浏览继续顺畅，关键写操作要求登录，管理端要求管理员角色，用户身份尽量从 JWT 当前用户取得，生产关键接口不再完全裸奔。

本阶段不是企业级身份系统重写，而是一次渐进式、可验证、低风险的安全闭环。

## 2. 当前安全状态盘点

### 2.1 已具备能力

- 登录接口会校验用户名密码和账号状态，并签发 JWT。
- JWT payload 当前包含 `issuer`、`subject=username`、`userId`、签发时间和过期时间。
- 前端 Axios client 已在有 token 时注入 `Authorization: Bearer <token>`。
- 数据库已有 `roles`、`user_roles`，种子数据中：
  - `ROLE_STUDENT`：普通学生；
  - `ROLE_ADMIN`：平台管理员；
  - demo `admin` 用户拥有 `ROLE_ADMIN`。
- 用户限制、信用、管理员动作日志、安全日志等治理基础已在 Phase 5 之后存在，部分业务服务已经有发布/服务限制检查。
- Phase 8 支付中心 callback 已有内部 token / 签名边界，本阶段不得破坏该内部回调通道。

### 2.2 主要风险

- `SecurityConfig` 当前对 `/api/**` 全部 `permitAll`，任何人可直接调用多数写接口。
- 后端没有 JWT authentication filter，也没有统一当前用户解析工具。
- 控制器大量信任 query/path/body 中的 `userId`、`sellerId`、`buyerId`、`publisherId`、`merchantId`、`adminId`。
- `/api/admin/**` 主要靠路径命名和前端传入 `adminId`，未由后端强制 `ROLE_ADMIN`。
- 前端刷新后只保留 token，`currentUser` 不会自动恢复；这会影响严格登录态下的体验。
- 部分页面仍有 demo 兜底用户，例如 `auth.currentUser?.id ?? 1` 或固定 `adminId = 1`。
- 文件绑定接口只做部分数量限制，没有登录、文件归属或目标归属校验。

## 3. Phase 10 范围

### 3.1 本阶段范围

1. 增加 JWT 请求认证能力，解析当前用户和角色。
2. 新增 `/api/auth/me`，用于前端刷新后恢复登录态。
3. 重写 `SecurityConfig` 路由规则：公共读放行，写操作要求登录，管理接口要求 `ROLE_ADMIN`。
4. 改造关键写接口：优先从 JWT 当前用户获取用户身份；兼容期保留必要 userId 参数时必须与 JWT 一致。
5. 改造管理接口：从 JWT 当前管理员获取 adminId，不再信任前端传入的 adminId。
6. 前端处理 401/403：登录态失效时清理 session，写操作未登录时提示登录，非管理员访问 admin 页面时显示清晰提示。
7. 加固文件绑定：要求登录，并校验文件上传者和业务目标归属。
8. 对敏感用户动作和管理员动作补充安全日志或管理员动作日志。
9. 更新 README、CLAUDE handoff、接口权限清单和验证矩阵。
10. 通过服务器 Docker build、API smoke 和 Playwriter 验证。

### 3.2 非目标

Phase 10 不做：

- OAuth、SAML、统一身份认证或多学校 SSO；
- refresh token / rotate token / 设备管理；
- 企业级细粒度 permission matrix；
- 全站 WAF、完整限流中间件或完整渗透测试整改；
- 完整上传系统重写；
- 业务架构大重构；
- API-Transfer-Station 支付密钥迁移；
- CampusHub 读取、打印、复制或保存支付宝密钥、SMTP 密码、JWT secret、payment token、服务器 `.env`；
- 修改已应用迁移 V1-V12。

## 4. 认证设计

### 4.1 JWT authentication filter

新增后端认证过滤器，处理所有 `/api/**` 请求中的 Bearer token：

1. 没有 `Authorization` header：保持匿名请求。
2. Header 不以 `Bearer ` 开头：保持匿名或返回认证失败，具体实现以不破坏公共读为准。
3. Bearer token 存在：
   - 校验签名；
   - 校验 issuer；
   - 校验 expiration；
   - 读取 `userId` 和 `subject`；
   - 查询用户是否存在且 `status = ACTIVE`；
   - 查询 `user_roles` 得到 Spring Security authorities；
   - 放入 `SecurityContext`。
4. token 无效或过期：返回 401，不泄露 secret、签名细节或栈信息。

### 4.2 当前用户工具

新增 `CurrentUserService`，作为 controller/service 获取登录上下文的唯一入口：

- `requireUserId()`：未登录返回 401/业务异常。
- `requireUser()`：返回当前 `User`。
- `requireAdminId()`：当前用户必须有 `ROLE_ADMIN`。
- `optionalUserId()`：公共详情页可用，用于判断收藏、联系可见性等。
- `requireSameUser(Long requestedUserId)`：兼容旧接口参数，确保路径/query 中 userId 与 JWT userId 一致。

业务服务层仍保留 ownership 校验，例如商品只能卖家下架、订单只能买家确认、店铺只能商家管理。认证只证明“是谁”，业务服务继续判断“能不能做这件事”。

### 4.3 `/api/auth/me`

新增当前用户接口：

- `GET /api/auth/me`：要求登录，返回 `CurrentUserSummary`。
- 前端应用启动或页面刷新时，如果 localStorage 中有 token，则调用该接口恢复 `currentUser`。
- 如果返回 401，前端清理 token 并回到匿名状态。

## 5. 授权与接口分级

### 5.1 匿名允许

以下接口继续允许匿名访问：

- `POST /api/auth/register/send-code`
- `POST /api/auth/register`
- `POST /api/auth/login`
- 公共 GET 列表/详情：
  - `/api/goods`、`/api/goods/{id}`；
  - `/api/tasks`、任务公开详情；
  - `/api/shops`、`/api/shops/{id}`、公开 service items；
  - `/api/project-ads`、`/api/project-ads/featured`、`/api/project-ads/{id}`；
  - 公开文件绑定读取；
  - 不含私人 userId 语义的公开展示接口。
- 支付中心内部 callback endpoint。该接口不走用户 JWT，而继续使用 Phase 8 的 payment-center token / signature 校验。

### 5.2 登录必需

所有业务写操作默认要求登录，包括：

- 发布、编辑、下架、出售商品；
- 商品意向、线上托管创建/冻结/确认/取消/争议；
- 发布跑腿、抢单、申请、接单、流转、确认、问题上报；
- 创建/编辑店铺、服务项目、预约订单、商家处理预约；
- 创建/编辑/提交/关闭项目广告；
- 评论、收藏、举报、评价；
- 角色申请和保证金支付；
- 通知读取和已读；
- 钱包充值、提现、余额和流水私有读取；
- 文件绑定。

### 5.3 管理员必需

所有 `/api/admin/**` 要求 `ROLE_ADMIN`，包括：

- 身份申请审核；
- 内容审核；
- 运营 dashboard / analytics / CSV export；
- 治理工作台；
- 支付订单和 callback 监控；
- 钱包运营：微信充值审核、提现审核/完成/拒绝、冻结记录查看；
- 项目广告 admin action；
- 未来 Phase 11 的 beta 运维接口。

### 5.4 私有读接口

以下接口要求登录，并且只能读取当前用户自己的数据，除非当前用户是管理员：

- `/api/wallet/users/{userId}` 及 flows、frozen-items、recharges、withdrawals；
- `/api/notifications/users/{userId}`；
- `/api/credit/users/{userId}`；
- 用户自己的项目广告管理列表；
- 用户自己的店铺/预约工作台；
- 用户自己的评论/收藏列表，如作为个人中心私有数据使用。

兼容期可以保留路径中的 `{userId}`，但必须通过 `requireSameUser(userId)`。

## 6. 后端改造策略

### 6.1 安全基础设施

新增或修改：

- `backend/src/main/java/com/campushub/auth/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/campushub/auth/JwtTokenService.java`
- `backend/src/main/java/com/campushub/auth/CurrentUserService.java`
- `backend/src/main/java/com/campushub/auth/CurrentUserPrincipal.java`
- `backend/src/main/java/com/campushub/user/UserRoleRepository.java` 或 JDBC 查询 helper
- `backend/src/main/java/com/campushub/config/SecurityConfig.java`

`AuthController` 登录签发 token 的逻辑可迁移到 `JwtTokenService`，避免 filter 和 controller 各自解析配置。

### 6.2 控制器改造优先级

优先改造高风险写操作和管理接口：

1. `goods`：发布、编辑、下架、意向、mark-sold、escrow 操作。
2. `task`：发布、抢单、申请、接受申请、工作流流转、完成码、确认、问题上报。
3. `wallet`：用户充值/提现/私有账本读取，管理员充值/提现审核。
4. `identity`：角色申请、身份审核、保证金支付归属。
5. `shop`：店铺、服务项目、预约订单、商家处理。
6. `projectad`：发布者管理和 admin action。
7. `interaction` / `moderation`：评论、收藏、举报、评价、治理 admin action。
8. `file`：文件绑定。

### 6.3 兼容策略

为降低一次性前后端联动风险，Phase 10 可采用“兼容但校验”的策略：

- 前端短期仍可传 `userId`，后端使用 `requireSameUser(userId)`。
- 新增 API helper 函数时逐步移除 userId/adminId 参数。
- `adminId` 对管理动作只作为兼容参数存在时也必须与 JWT admin userId 一致；推荐直接从 `CurrentUserService.requireAdminId()` 获取。
- 公共详情页 `viewerId` 改为可选 JWT：登录时从 token 识别 viewer，匿名时 viewer 为空；兼容期可以保留 `viewerId` 但不得用于越权私有数据读取。

### 6.4 审计日志

以下动作应写入已有 admin action log 或 security audit log：

- 登录成功和登录失败；
- token 认证失败可按低频采样或关键原因记录，不记录 token 原文；
- 管理员身份申请审核；
- 管理员治理、违规、信用、限制操作；
- 管理员钱包充值/提现处理；
- 支付 callback 异常继续使用 Phase 8 callback event 与安全审计；
- 用户发起提现、线上托管冻结/确认/争议等资金敏感动作。

日志不得写入密码、JWT 原文、payment token、签名 secret、完整支付 URL 或支付宝密钥内容。

## 7. 文件绑定安全设计

当前代码主要存在 `FileResource` 和 `FileBinding`，但没有发现完整 multipart upload controller。Phase 10 不强行扩展上传系统，先加固现有文件绑定能力。

`POST /api/files/bindings` 应要求登录，并校验：

1. 文件存在；
2. 当前用户是文件上传者，或当前用户是管理员；
3. target type 在允许列表内：`GOODS`、`SHOP`、`SERVICE_ITEM`、`PROJECT_AD`、`REWARD_TASK` 等；
4. 当前用户拥有该 target 的编辑权，或当前用户是管理员；
5. 商品图片数量上限继续保留；
6. 绑定失败返回明确业务错误，不暴露内部表结构。

如果实施中发现实际上传入口存在，则补充：

- 单文件大小上限；
- content-type 白名单；
- 服务器生成存储文件名；
- 路径 normalize 后必须位于 upload root 下；
- 不允许用户控制最终存储路径。

## 8. 前端适配设计

### 8.1 登录态恢复

- auth store 增加 `loadCurrentUser()`。
- 应用启动或路由进入时，如果有 token 且无 currentUser，调用 `/auth/me`。
- 401 时清理 token 和 currentUser。

### 8.2 API 错误处理

- Axios response interceptor 处理 401/403。
- 401：清理会话，提示“登录已过期，请重新登录”。
- 403：提示“当前账号无权限执行此操作”。
- 保持现有 `ApiResponse<T>` unwrap 行为。

### 8.3 写操作登录提示

关键页面在调用写 API 前检查登录：

- goods detail / publish；
- tasks hall / workspace；
- shop detail / merchant；
- project ad detail / manage；
- wallet；
- roles；
- notifications；
- comments/favorites/reports/reviews。

未登录时不发请求，直接提示并引导到 `/auth`。

### 8.4 管理页面权限提示

`/admin/*` 页面在前端做体验层检查：

- 未登录：显示登录提示；
- 已登录但无 `ROLE_ADMIN`：显示无权限说明；
- 有 `ROLE_ADMIN`：加载页面数据。

后端仍是最终权限来源，前端检查只用于减少困惑。

### 8.5 移除 demo 兜底身份

Phase 10 应移除或隔离生产路径中的：

- `auth.currentUser?.id ?? 1`；
- 固定 `adminId = 1`；
- 管理页面手动输入 adminId。

如保留课程演示便利，应只放在明确的 demo-only 文案或非生产 helper 中，不能绕过后端权限。

## 9. 数据库与迁移

Phase 10 优先不新增 schema。已有表足够支持：

- `users`；
- `roles`；
- `user_roles`；
- `security_logs` 或现有 audit/admin action logs；
- Phase 5 的治理审计记录；
- Phase 8/9 的支付与钱包审计记录。

如果实施中确实需要补充安全日志字段或登录事件表，只能新增 `V13__*.sql`，不得编辑 V1-V12。默认建议通过已有日志表完成本阶段。

## 10. 生产与部署边界

- 生产分支当前为 `phase9-wallet-escrow`，Phase 10 应从该基线继续。
- 生产目录为 `/opt/campushub`。
- 服务器较小，构建、重启和 smoke 检查要低频、低影响。
- 不读取、不打印、不复制、不提交任何 `.env`、SMTP 密码、JWT secret、payment token、支付宝密钥或服务器 secret。
- CampusHub 不接管 API-Transfer-Station 的支付宝密钥或真实支付渠道逻辑。
- 支付中心 callback endpoint 不应被普通用户 JWT 规则误拦截；它继续使用内部 token/signature。

## 11. 验证矩阵

### 11.1 后端构建

- server-side Docker backend build 成功，Maven package 显示 `BUILD SUCCESS`。
- server-side Docker frontend build 成功，仅允许已知 Vite large chunk / dependency pure-comment warnings。

### 11.2 API smoke

匿名请求：

- `GET /api/goods` 返回 200。
- `GET /api/tasks` 返回 200。
- `GET /api/shops` 返回 200。
- `GET /api/project-ads` 返回 200。
- `POST /api/goods` 返回 401。
- `POST /api/wallet/users/1/recharges` 返回 401。
- `GET /api/admin/wallet/recharges` 返回 401 或 403。

普通用户登录后：

- `GET /api/auth/me` 返回当前用户。
- 普通用户可执行自己的业务写操作。
- 普通用户访问 `/api/admin/**` 返回 403。
- userId 与 JWT 不一致的请求返回 403 或明确业务错误。

管理员登录后：

- `GET /api/auth/me` 包含 `ROLE_ADMIN`。
- `/api/admin/wallet/recharges` 返回 200。
- `/api/admin/payment/orders` 返回 200。
- `/api/admin/ops/analytics/overview` 返回 200。
- `/api/admin/governance/dashboard` 返回 200。

支付 callback：

- 合法 payment-center token/signature 的 callback 不因 JWT 改造失败。
- 非法 callback 继续拒绝并记录审计，不输出 secret。

### 11.3 Playwriter 浏览器验证

桌面端：

- `/auth` 登录成功。
- `/goods`、`/goods/1`、`/tasks`、`/shops`、`/project-ads` 匿名可浏览。
- 未登录写操作提示登录。
- 登录后关键写入口可用。
- `/wallet` 正常显示当前用户钱包。
- `/admin/wallet`、`/admin/payment`、`/admin/ops`、`/admin/governance` 对非管理员显示无权限，对管理员正常渲染。

移动端 375-390px：

- `/auth`、`/goods`、`/tasks`、`/shops`、`/project-ads`、`/wallet`、`/admin/wallet` 基础渲染正常。
- 无明显 document-level horizontal overflow。
- 底部导航和“更多”抽屉不因权限提示破版。

## 12. Phase 10 完成标准

Phase 10 完成后，应满足：

1. 公共浏览不需要登录。
2. 关键写接口匿名不可调用。
3. 用户写接口不再无条件信任前端传入 userId。
4. 管理接口必须 `ROLE_ADMIN`。
5. 前端能恢复 token 对应的 current user，并能清晰处理 401/403。
6. 文件绑定至少具备登录和归属校验。
7. README 和 CLAUDE handoff 明确 Phase 10 安全边界。
8. 服务器 Docker build、API smoke 和 Playwriter 验证通过。
9. 未读取或输出任何生产 secret。
10. 未编辑 V1-V12 已应用迁移。
