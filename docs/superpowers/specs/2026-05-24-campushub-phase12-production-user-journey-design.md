# CampusHub Phase 12 Production User Journey Design

## Goal

Phase 12 turns the verified Beta baseline into a more realistic production user journey by fixing the first blockers a new user sees: real email-code delivery, wallet recharge payment handoff, and WeChat manual QR recharge.

## Current baseline

Phase 11 verified that CampusHub can run in production, authenticate demo users, protect private/admin routes, and render desktop/mobile pages. That is necessary but not enough for a real new user. A new user still hits operational gaps before they can become a paying/publishing user:

- Registration email code may silently run in mock mode if production SMTP is not enabled.
- Alipay wallet recharge can create a payment order, but the wallet page does not open or expose the returned payment URL.
- WeChat recharge creates a manual-review order, but the user is not shown a configured collection QR code.

## Non-negotiable production requirements

1. Registration must use real SMTP in production when enabled and must fail clearly when mail delivery is unavailable.
2. Alipay recharge must return a usable payment URL from the existing payment-center provider and the wallet UI must open it like API-Transfer-Station does.
3. WeChat recharge must show a configured manual collection QR image and note, then keep the existing admin manual-review入账 flow.
4. CampusHub must not read, print, copy, commit, or own Alipay key bodies. Real Alipay remains in API-Transfer-Station.
5. CampusHub must not require WeChat Pay API secrets for Phase 12. Manual QR is enough for current campus Beta operations.
6. Existing service-fee and role-deposit payment flows must keep working.

## Architecture

### Email registration

Keep the current Spring Boot registration flow:

- `AuthController` exposes `/api/auth/register/send-code` and `/api/auth/register`.
- `RegistrationService` normalizes campus email, stores only hashed verification codes, and delegates delivery to `RegisterMailService`.
- `RegisterMailService` uses `JavaMailSender` only when `campushub.mail.enabled=true`.

Phase 12 tightens the production behavior: if mail is enabled but delivery fails, the API returns a clear business failure. If mail is disabled, the UI and response should not imply that a real email was sent.

### Wallet recharge payment handoff

Keep `WalletOperationService.createRecharge` as the wallet entry point. Add a response field that exposes the linked payment order `payUrl` when available:

- Alipay recharge creates a `WALLET_RECHARGE` payment order through `PaymentProvider.createWebPayment`.
- The payment order stores provider order number and `payUrl`.
- `WalletRechargeSummary` returns `paymentPayUrl` and optional `paymentProvider` for user-facing action.
- `WalletView.vue` opens non-mock `paymentPayUrl` in a new tab and also shows a fallback button to reopen it from the recharge card.

### WeChat manual QR recharge

Add production configuration for a manual WeChat collection QR:

- `campushub.wallet.recharge.wechat.manual-qr-url`
- `campushub.wallet.recharge.wechat.manual-note`

When a user submits a WeChat recharge, the backend keeps the order in `PENDING_REVIEW` and returns `wechatQrUrl` plus `wechatNote` in `WalletRechargeSummary`. The frontend displays a dialog with the QR image and instructions. Admin approval remains the existing `/admin/wallet/recharges/{id}/approve` flow.

## User journey acceptance matrix

1. New user opens `/auth`, enters an `edu.cn` email, and clicks send code.
   - If SMTP is configured, the request succeeds and the user gets a realistic delivery message.
   - If SMTP is disabled in a production-like environment, the API/UI makes the limitation visible rather than claiming a real email was sent.
2. User registers with code plus WeChat or QQ contact and receives a wallet account.
3. User opens `/wallet`, chooses Alipay recharge, and submits.
   - The frontend opens the payment URL in a new tab when it is not `mock://`.
   - The recharge card keeps a “继续支付” action while pending.
4. User opens `/wallet`, chooses WeChat recharge, and submits.
   - The frontend shows the configured manual QR and note.
   - The admin wallet page still sees the pending review order and can approve/reject it.
5. User can continue to apply for publishing roles and publish/use the platform after wallet/role flows.

## Verification

- Backend targeted tests for registration mail failure clarity, wallet recharge payment URL propagation, and WeChat manual QR response fields.
- Frontend unit tests for wallet payment action helper behavior.
- Server Docker backend/frontend builds before deployment.
- Production smoke: send-code behavior, Alipay recharge URL creation/opening, WeChat QR display, admin recharge review visibility.
- Playwriter desktop/mobile checks for `/auth`, `/wallet`, `/roles`, `/goods/publish`, and admin wallet/payment pages.

## Out of scope

- Direct WeChat Pay API v3 integration.
- Moving Alipay key material into CampusHub.
- Full payment reconciliation or automatic WeChat入账.
- New schema migration unless implementation discovers an unavoidable persistence requirement.
- Real-time chat, maps, recommendation, or mini-program work.
