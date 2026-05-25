# CampusHub Phase 14 Real Feedback Fixes Design

## Goal

Phase 14 turns the latest real-user feedback into a focused production upgrade. It fixes the immediate blockers around session expiry and role-deposit payment, then expands the admin system from a single generic admin into a master-plus-domain-admin model with concrete management workspaces.

The phase is intentionally split into two deployment slices:

1. Login/session and identity-payment fixes.
2. Domain admin roles and management workspaces.

This keeps the small production server risk low and allows the most painful user-facing bugs to land first.

## Current problems

1. Clicking around the authenticated menu, especially Wallet Center, can force logout with “登录已过期，请重新登录”. The current JWT default is 720 minutes and the frontend clears the whole session on any 401 response.
2. Role-deposit payment only supports the payment-center Alipay flow. Users with wallet balance cannot pay deposits directly.
3. A user who previously applied for runner or goods-publisher identity but did not finish payment is blocked by “该身份已申请”. Pending unpaid applications should be recoverable rather than dead ends.
4. Project ads have a submit-for-review path, but the review/approval surface is not explicit enough for users and admins. Tasks and goods do not require review, but still need admin management pages for takedown/cleanup. Shops should follow the project-ad pattern: shop visibility requires review, while merchant-owned service items do not require per-item review after merchant approval.
5. The admin model is too flat. The platform needs a `master` super-admin, plus domain admins for trade-related work and showcase/shop-related work. Users should be able to apply for these admin identities, and only master can approve them.

## Scope

### In scope

- Extend default JWT lifetime to long-lived login, with production env override preserved.
- Improve frontend 401 handling so background or recoverable auth failures do not always produce a disruptive logout message.
- Show protected pages such as Wallet Center with login guidance when needed.
- Let role applicants continue existing unpaid applications.
- Add wallet-balance payment for role deposits.
- Keep Alipay payment-center payment for deposits.
- Do not add WeChat payment for role deposits.
- Add domain admin roles:
  - `ROLE_MASTER_ADMIN` / `master`
  - `ROLE_TRADE_ADMIN`
  - `ROLE_SHOWCASE_ADMIN`
- Preserve `ROLE_ADMIN` compatibility for existing admin checks during rollout.
- Ensure `yeshenghao@mail.ustc.edu.cn` has `ROLE_MASTER_ADMIN` and `ROLE_ADMIN` through an idempotent migration.
- Add admin identity applications for trade admin and showcase admin. These require review and no deposit.
- Replace the old visible “内容审核” navigation entry with domain-specific management pages.
- Add or refine management surfaces for:
  - 跑腿管理
  - 商品管理
  - 跑腿商品售后纠纷
  - 广告管理
  - 商店管理
  - 商店服务售后纠纷
  - 管理员申请审核
- Add backend authorization checks so master can do everything, trade admin can manage trade surfaces, and showcase admin can manage showcase/shop surfaces.
- Keep production verification low-impact: targeted builds, server-local smoke, and Playwriter checks.

### Out of scope

- Full RBAC framework rewrite with arbitrary permissions and role-permission tables.
- WeChat role-deposit payment.
- Complex escrow/arbitration money movement for disputes beyond safe status handling and notifications.
- Reading, printing, or changing production secrets.
- Editing already-applied migrations V1-V12.
- Resetting production passwords for smoke tests.

## Slice 1: Login and role-deposit fixes

### Long-lived login

The backend JWT default changes from 720 minutes to 30 days. The existing `CAMPUSHUB_JWT_EXPIRATION_MINUTES` override remains, so production can tune the lifetime without code changes.

The frontend auth client stops treating every 401 as an immediate global “session expired” event. It should still clear the token when `/auth/me` or an intentional authenticated operation proves the token is invalid, but protected page loading should produce a login prompt or redirect rather than a surprising logout toast during navigation.

Success criteria:

- A valid user can navigate between authenticated menu items without being logged out just because a protected endpoint responds 401 during page bootstrapping.
- Expired or invalid tokens still eventually clear and route the user to login.
- Wallet Center either loads normally for logged-in users or shows a controlled login-required state for guests/invalid sessions.

### Recoverable role applications

`IdentityService.apply` currently rejects any existing `(user_id, role_type)` record. Phase 14 changes the behavior:

- If the role is already granted or the application is paid/approved/pending review, block duplicate application.
- If the existing application is `PENDING_PAYMENT`, return the existing application so the UI can continue payment.
- If the existing application is failed or expired, allow a new payment attempt against the same application or reset it to a payable state without violating the unique `(user_id, role_type)` constraint.

The identity page should load and display current applications instead of only keeping applications created in the current browser session.

### Wallet payment for deposits

Role deposit payment gains a second method: wallet balance.

- Alipay flow remains as the payment-center web-payment path.
- Wallet payment directly debits the applicant wallet balance with an idempotency key tied to the role application.
- After wallet payment succeeds:
  - runner and goods publisher become approved immediately;
  - shop merchant enters pending manual review;
  - admin identity applications do not use deposit payment.
- If balance is insufficient, return a clear message telling the user to recharge or use Alipay.

## Slice 2: Domain admins and management surfaces

## Role model

New roles are added by V13+ migration:

- `ROLE_MASTER_ADMIN`: top-level system administrator, displayed as `master`. It includes all current and new admin capabilities.
- `ROLE_TRADE_ADMIN`: handles running tasks, second-hand goods, and trade-related disputes.
- `ROLE_SHOWCASE_ADMIN`: handles project ads, student shops, shop merchant role applications, and shop-service disputes.

`ROLE_ADMIN` remains for compatibility. Master should also receive `ROLE_ADMIN` so existing route checks and current production behavior do not break during transition.

An authorization helper should centralize checks:

- `requireMasterAdmin()`
- `requireTradeAdmin()` accepts master or trade admin.
- `requireShowcaseAdmin()` accepts master or showcase admin.
- existing `requireAdminId()` should accept master/domain-compatible admin where appropriate, or remain only for legacy global admin paths until replaced.

## Admin identity applications

The identity application system is extended with two admin role types:

- `TRADE_ADMIN`
- `SHOWCASE_ADMIN`

These applications:

- have deposit amount 0;
- start as `PENDING_REVIEW`;
- can only be approved or rejected by master;
- assign `ROLE_TRADE_ADMIN` or `ROLE_SHOWCASE_ADMIN` after approval;
- are visible from a new master-only “管理员申请审核” page.

The identity unlock page shows these two cards to logged-in users. The copy should make clear that admin identities are operational permissions and require master review.

## Trade admin workspace

Trade admin and master can access a new trade-management area with three screens.

### 跑腿管理

Capabilities:

- list tasks by status, publisher, campus zone, and time range where feasible;
- view task details and key participants;
- close/delete invalid or violating tasks;
- add an admin note and notify affected users.

### 商品管理

Capabilities:

- list goods by status, seller, category, and time range where feasible;
- take down/delete invalid or violating goods;
- view linked orders where available;
- add an admin note and notify affected users.

### 跑腿商品售后纠纷

Capabilities:

- aggregate task issues and goods-order dispute states available in the current schema;
- mark disputes as processing/resolved/rejected where supported;
- record admin handling notes;
- avoid balance transfer or escrow mutation unless an existing safe service already supports it.

## Showcase admin workspace

Showcase admin and master can access a new showcase-management area with three screens.

### 广告管理

Capabilities:

- list project ads by pending/approved/rejected/closed/blocked status;
- approve, reject, feature/unfeature, close, or block ads using existing project-ad workflow where possible;
- show rejection notes and status clearly to publishers in their project management page.

### 商店管理

Capabilities:

- review shop merchant role applications;
- review newly submitted shops before public visibility;
- pause/resume/close shops;
- manage service items inside approved shops, including pause/off-shelf/delete or equivalent safe status transitions;
- do not require per-item pre-review for merchants after shop approval.

### 商店服务售后纠纷

Capabilities:

- list service orders with issue/dispute-like states from current shop order workflow;
- support safe status handling and admin notes;
- notify customer and provider when an admin action is taken;
- avoid wallet ledger changes unless covered by existing safe wallet services.

## Navigation and frontend behavior

The old visible “内容审核” entry is removed from navigation. Existing legacy backend APIs may remain until they are no longer referenced.

Navigation should derive admin visibility from the expanded identity model:

- master sees every admin page;
- trade admin sees only trade-management pages;
- showcase admin sees only showcase-management pages;
- legacy `ROLE_ADMIN` users keep access to existing admin pages during transition, but new domain pages should follow domain checks.

Users should not see admin pages they cannot access. Direct URL access must still be protected by backend checks.

## Database and migration strategy

Use only V13+ for schema/data changes.

Expected migration contents:

- insert new roles if missing;
- grant `ROLE_MASTER_ADMIN` and `ROLE_ADMIN` to `yeshenghao@mail.ustc.edu.cn` if the user exists;
- add fields only if required for admin applications or management notes/statuses;
- avoid destructive changes to existing production data;
- do not remove legacy roles or records.

If additional management actions can be represented by existing status fields, prefer service-layer changes over schema expansion.

## Verification plan

### Local

- Frontend targeted tests for auth handling, identity role mapping, navigation, and wallet-payment action helpers.
- Frontend build.
- Add backend tests for:
  - recoverable pending role application behavior;
  - wallet deposit role payment;
  - master/trade/showcase authorization helper behavior;
  - admin role application approval.

Local Maven may be unavailable, so backend tests can be committed and verified through server Docker build when needed.

### Production/server

Use low-impact verification:

- targeted backend Docker build if backend changed;
- targeted frontend Docker build if frontend changed;
- restart only necessary containers;
- server-local API smoke for public routes, protected route 401 behavior, role application endpoints, wallet deposit endpoint, and new admin endpoints with available user-known credentials;
- do not read or reset production passwords.

### Browser/Playwriter

Desktop and mobile checks:

- login, then navigate through profile, wallet, roles, notifications, credit without surprise logout;
- role application card shows existing unpaid application and offers Alipay/balance payment;
- project-ad management shows submitted/pending/approved/rejected state clearly;
- domain admin navigation appears only for authorized admin identities;
- mobile pages have no document-level horizontal overflow.

## Rollout plan

### Deployment slice 1

Ship and verify:

- JWT lifetime and frontend 401 handling;
- recoverable role applications;
- wallet deposit payment;
- identity page application loading/payment choices.

### Deployment slice 2

Ship and verify:

- V13 roles/master grant;
- admin identity applications;
- domain authorization checks;
- trade and showcase admin workspaces;
- navigation cleanup.

This sequencing fixes the most disruptive user-facing issues first while keeping the larger admin expansion isolated.
