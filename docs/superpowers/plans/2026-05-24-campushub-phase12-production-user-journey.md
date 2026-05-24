# CampusHub Phase 12 Production User Journey Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the new-user production journey real by enabling clear SMTP registration behavior, Alipay recharge payment URL handoff, and WeChat manual QR recharge.

**Architecture:** Keep CampusHub's existing bounded contexts and payment boundary. Registration remains Spring Mail based; Alipay remains delegated to API-Transfer-Station through `PaymentCenterProvider`; WeChat recharge uses a configured manual QR and existing admin review rather than direct WeChat Pay secrets.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway/MySQL, Vue 3, Vite, TypeScript, Element Plus, Vitest, Docker Compose, Playwriter.

---

## File map

- Modify `backend/src/main/java/com/campushub/auth/RegisterMailService.java`: make enabled SMTP delivery failures surface as business failures.
- Modify `backend/src/main/java/com/campushub/wallet/WalletRechargeSummary.java`: add payment-provider/pay-url and WeChat QR/note fields.
- Modify `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`: populate the new summary fields from payment orders and config.
- Create `backend/src/main/java/com/campushub/wallet/WalletRechargeProperties.java`: typed config for WeChat manual QR.
- Modify `backend/src/main/java/com/campushub/CampusHubApplication.java` if configuration properties are not already scanned.
- Modify `backend/src/main/resources/application.yml` and `.env.prod.example`: add manual WeChat QR placeholders and document SMTP must be enabled for real registration.
- Modify `frontend/src/api/campushub.ts`: extend `WalletRechargeSummary` fields.
- Modify `frontend/src/views/WalletView.vue`: open Alipay pay URLs, show QR dialog, and expose retry actions.
- Create `frontend/src/views/walletPaymentActions.ts`: small testable helpers for deciding whether to open payment URL or show QR.
- Create `frontend/src/views/walletPaymentActions.test.ts`: Vitest unit tests for payment UI decisions.
- Modify `README.md` and `CLAUDE.md`: Phase 12 handoff and production boundaries.

## Task 1: Backend mail failure clarity

**Files:**
- Modify: `backend/src/main/java/com/campushub/auth/RegisterMailService.java`
- Test: `backend/src/test/java/com/campushub/auth/RegisterMailServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/campushub/auth/RegisterMailServiceTest.java`:

```java
package com.campushub.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class RegisterMailServiceTest {

    @Test
    void enabledMailWithoutSenderFailsClearly() {
        MailProperties properties = new MailProperties(
                true,
                "smtp",
                new MailProperties.Smtp("smtp.example.com", 587, "user", "secret", "noreply@example.com", "CampusHub"),
                new MailProperties.Code(10, 60));
        ObjectProvider<org.springframework.mail.javamail.JavaMailSender> sender = new EmptyObjectProvider<>();
        RegisterMailService service = new RegisterMailService(sender, properties);

        assertThatThrownBy(() -> service.sendRegisterCode("student@mail.ustc.edu.cn", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件服务未配置");
    }

    private static class EmptyObjectProvider<T> implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            return null;
        }

        @Override
        public T getIfAvailable() {
            return null;
        }

        @Override
        public T getIfUnique() {
            return null;
        }

        @Override
        public T getObject() {
            return null;
        }
    }
}
```

- [ ] **Step 2: Run the test to verify RED**

Run:

```bash
mvn -pl backend -Dtest=RegisterMailServiceTest test
```

Expected: test fails because `RegisterMailService` currently logs mock delivery instead of throwing a clear business failure when mail is enabled and sender is unavailable.

- [ ] **Step 3: Implement minimal backend change**

In `RegisterMailService.sendRegisterCode`, use this behavior:

```java
if (!mailProperties.enabled()) {
    log.info("Registration email verification mock-sent to {}", maskEmail(email));
    return;
}
if (mailSender == null) {
    throw new BusinessException("邮件服务未配置，请联系管理员开启 SMTP");
}
```

Wrap `mailSender.send(message)` failures as:

```java
try {
    mailSender.send(message);
} catch (RuntimeException ex) {
    throw new BusinessException("验证码邮件发送失败，请稍后再试");
}
```

- [ ] **Step 4: Run the test to verify GREEN**

Run:

```bash
mvn -pl backend -Dtest=RegisterMailServiceTest test
```

Expected: PASS.

## Task 2: Backend wallet recharge payment metadata

**Files:**
- Create: `backend/src/main/java/com/campushub/wallet/WalletRechargeProperties.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletRechargeSummary.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/campushub/wallet/WalletRechargeSummaryTest.java`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/campushub/wallet/WalletRechargeSummaryTest.java`:

```java
package com.campushub.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WalletRechargeSummaryTest {

    @Test
    void withPaymentInfoCopiesAlipayPaymentMetadata() {
        WalletRechargeSummary summary = new WalletRechargeSummary(
                1L, "WR-1", 2L, "学生", "ALIPAY", null, null, null,
                "PENDING_PAYMENT", "CHP-WR-1", null, null, null,
                null, null, null, null);

        WalletRechargeSummary updated = summary.withPaymentInfo("PAYMENT_CENTER", "https://pay.example.com/order/1");

        assertThat(updated.paymentProvider()).isEqualTo("PAYMENT_CENTER");
        assertThat(updated.paymentPayUrl()).isEqualTo("https://pay.example.com/order/1");
        assertThat(updated.wechatQrUrl()).isNull();
    }

    @Test
    void withWechatManualInfoCopiesQrAndNote() {
        WalletRechargeSummary summary = new WalletRechargeSummary(
                1L, "WR-1", 2L, "学生", "WECHAT", null, null, null,
                "PENDING_REVIEW", null, null, null, null,
                null, null, null, null);

        WalletRechargeSummary updated = summary.withWechatManualInfo("https://assets.example.com/wechat.png", "扫码后备注订单号");

        assertThat(updated.wechatQrUrl()).isEqualTo("https://assets.example.com/wechat.png");
        assertThat(updated.wechatNote()).isEqualTo("扫码后备注订单号");
        assertThat(updated.paymentPayUrl()).isNull();
    }
}
```

- [ ] **Step 2: Run the test to verify RED**

Run:

```bash
mvn -pl backend -Dtest=WalletRechargeSummaryTest test
```

Expected: compilation fails because `WalletRechargeSummary` does not yet have payment/QR fields or helper methods.

- [ ] **Step 3: Implement minimal backend data shape**

Create `WalletRechargeProperties.java`:

```java
package com.campushub.wallet;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campushub.wallet.recharge")
public class WalletRechargeProperties {

    private final Wechat wechat = new Wechat();

    public Wechat getWechat() {
        return wechat;
    }

    public static class Wechat {
        private String manualQrUrl;
        private String manualNote;

        public String getManualQrUrl() {
            return manualQrUrl;
        }

        public void setManualQrUrl(String manualQrUrl) {
            this.manualQrUrl = manualQrUrl;
        }

        public String getManualNote() {
            return manualNote;
        }

        public void setManualNote(String manualNote) {
            this.manualNote = manualNote;
        }
    }
}
```

Update `WalletRechargeSummary` record to add four fields after `createdAt`:

```java
String paymentProvider,
String paymentPayUrl,
String wechatQrUrl,
String wechatNote
```

Add helper methods:

```java
public WalletRechargeSummary withPaymentInfo(String paymentProvider, String paymentPayUrl) { ... }
public WalletRechargeSummary withWechatManualInfo(String wechatQrUrl, String wechatNote) { ... }
```

Update `from(order)` to pass `null` for the new fields.

- [ ] **Step 4: Wire properties and summary enrichment**

Inject `WalletRechargeProperties` into `WalletOperationService`. Add a private method:

```java
private WalletRechargeSummary enrichRechargeSummary(WalletRechargeOrder order) {
    WalletRechargeSummary summary = WalletRechargeSummary.from(order);
    if (order.getPaymentOrderNo() != null) {
        paymentOrderRepository.findByOrderNo(order.getPaymentOrderNo())
                .ifPresent(payment -> summary = summary.withPaymentInfo(payment.getProvider(), payment.getPayUrl()));
    }
    if ("WECHAT".equals(order.getChannel())) {
        summary = summary.withWechatManualInfo(
                walletRechargeProperties.getWechat().getManualQrUrl(),
                walletRechargeProperties.getWechat().getManualNote());
    }
    return summary;
}
```

Use this helper in create/list/admin recharge methods.

Add to `application.yml`:

```yaml
  wallet:
    recharge:
      wechat:
        manual-qr-url: ${CAMPUSHUB_WECHAT_MANUAL_QR_URL:}
        manual-note: ${CAMPUSHUB_WECHAT_MANUAL_NOTE:扫码支付后请备注充值订单号，管理员审核后入账。}
```

- [ ] **Step 5: Run backend targeted tests**

Run:

```bash
mvn -pl backend -Dtest=WalletRechargeSummaryTest test
```

Expected: PASS.

## Task 3: Frontend payment action helpers

**Files:**
- Create: `frontend/src/views/walletPaymentActions.ts`
- Create: `frontend/src/views/walletPaymentActions.test.ts`

- [ ] **Step 1: Write failing frontend tests**

Create `frontend/src/views/walletPaymentActions.test.ts`:

```ts
import { describe, expect, it } from 'vitest'

import { getRechargePaymentAction } from './walletPaymentActions'

describe('getRechargePaymentAction', () => {
  it('opens non-mock Alipay payment URLs', () => {
    expect(getRechargePaymentAction({ channel: 'ALIPAY', paymentPayUrl: 'https://pay.example.com/1' })).toEqual({
      type: 'open-url',
      url: 'https://pay.example.com/1',
    })
  })

  it('does not open mock payment URLs', () => {
    expect(getRechargePaymentAction({ channel: 'ALIPAY', paymentPayUrl: 'mock://pay/1' })).toEqual({ type: 'none' })
  })

  it('shows WeChat manual QR when configured', () => {
    expect(getRechargePaymentAction({ channel: 'WECHAT', wechatQrUrl: 'https://assets.example.com/wx.png' })).toEqual({
      type: 'show-wechat-qr',
    })
  })
})
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```bash
npm --prefix frontend run test -- src/views/walletPaymentActions.test.ts
```

Expected: FAIL because helper file does not exist.

- [ ] **Step 3: Implement helper**

Create `frontend/src/views/walletPaymentActions.ts`:

```ts
export type RechargePaymentInput = {
  channel: string
  paymentPayUrl?: string | null
  wechatQrUrl?: string | null
}

export type RechargePaymentAction =
  | { type: 'open-url'; url: string }
  | { type: 'show-wechat-qr' }
  | { type: 'none' }

export function getRechargePaymentAction(recharge: RechargePaymentInput): RechargePaymentAction {
  if (recharge.channel === 'ALIPAY' && recharge.paymentPayUrl && !recharge.paymentPayUrl.startsWith('mock://')) {
    return { type: 'open-url', url: recharge.paymentPayUrl }
  }
  if (recharge.channel === 'WECHAT' && recharge.wechatQrUrl) {
    return { type: 'show-wechat-qr' }
  }
  return { type: 'none' }
}
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```bash
npm --prefix frontend run test -- src/views/walletPaymentActions.test.ts
```

Expected: PASS.

## Task 4: Frontend wallet recharge UX

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/WalletView.vue`
- Test: `frontend/src/views/walletPaymentActions.test.ts`

- [ ] **Step 1: Extend API type**

Add to `WalletRechargeSummary`:

```ts
paymentProvider: string | null
paymentPayUrl: string | null
wechatQrUrl: string | null
wechatNote: string | null
```

- [ ] **Step 2: Update wallet view behavior**

In `WalletView.vue`:

- Import `getRechargePaymentAction`.
- Track `wechatRecharge` and `wechatQrDialogVisible`.
- After `createWalletRecharge`, call helper:
  - `open-url`: `window.open(action.url, '_blank', 'noopener,noreferrer')`
  - `show-wechat-qr`: set `wechatRecharge` and show dialog
- On each recharge card:
  - show payment provider and payment order
  - show `继续支付` for pending Alipay with pay URL
  - show `查看微信收款码` for pending WeChat with QR URL
- Add a WeChat QR dialog that renders `<img :src="wechatRecharge.wechatQrUrl">` and displays `wechatRecharge.rechargeNo` plus `wechatRecharge.wechatNote`.

- [ ] **Step 3: Re-run frontend helper tests**

Run:

```bash
npm --prefix frontend run test -- src/views/walletPaymentActions.test.ts
```

Expected: PASS.

## Task 5: Docs, env, and verification

**Files:**
- Modify: `.env.prod.example`
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Modify: `docs/superpowers/specs/2026-05-24-campushub-phase12-production-user-journey-design.md`

- [ ] **Step 1: Document env placeholders**

Add placeholder-only envs to `.env.prod.example`:

```env
CAMPUSHUB_MAIL_ENABLED=true
CAMPUSHUB_WECHAT_MANUAL_QR_URL=https://example.com/path/to/wechat-qr.png
CAMPUSHUB_WECHAT_MANUAL_NOTE=扫码支付后请备注充值订单号，管理员审核后入账。
```

Do not add real credentials or QR image content.

- [ ] **Step 2: Update README Phase 12 section**

Add a concise Phase 12 section documenting:

- real SMTP registration requirement;
- Alipay recharge opens payment-center URL;
- WeChat recharge shows configured manual QR and remains admin-reviewed;
- no CampusHub Alipay key handling.

- [ ] **Step 3: Update CLAUDE handoff**

Add a Phase 12 handoff section after Phase 11 with implemented files, verification commands, and constraints.

- [ ] **Step 4: Run verification**

Run targeted commands when available:

```bash
npm --prefix frontend run test -- src/views/walletPaymentActions.test.ts
npm --prefix frontend run build
```

For backend, if local Maven is unavailable, defer to server Docker build. If Maven is available, run:

```bash
mvn -pl backend -Dtest=RegisterMailServiceTest,WalletRechargeSummaryTest test
```

- [ ] **Step 5: Production verification after deployment**

Use low-impact checks only:

- `/auth` send-code with a controlled campus email.
- `/wallet` Alipay recharge opens payment URL when payment-center env is configured.
- `/wallet` WeChat recharge shows QR image and recharge number.
- `/admin/wallet` sees pending WeChat recharge and can approve it.
- Mobile 390x844 wallet dialog has no document-level horizontal overflow.

## Self-review

- Spec coverage: covers SMTP registration, Alipay URL handoff, WeChat manual QR, admin review continuity, docs, and verification.
- Placeholder scan: plan includes placeholder-only env values, not implementation placeholders.
- Type consistency: `WalletRechargeSummary` adds `paymentProvider`, `paymentPayUrl`, `wechatQrUrl`, and `wechatNote`; frontend helper consumes those exact names.
