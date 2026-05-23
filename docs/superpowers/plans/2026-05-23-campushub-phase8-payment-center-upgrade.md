# CampusHub Phase 8 Payment Center Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden CampusHub role-deposit and service-fee payments by integrating with the API-Transfer-Station payment center while preserving mock mode and the no-Alipay-key boundary.

**Architecture:** Phase 8 adds a unified local payment-order layer, a payment-center provider selected by environment variables, internal callback verification/idempotency, and an admin payment monitor. CampusHub stores only local order mappings, statuses, and callback audit records; API-Transfer-Station remains responsible for real Alipay configuration, signatures, and external channel notifications.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, Spring `RestClient`, Vue 3, Vite, TypeScript, Element Plus, Docker Compose, Playwriter browser verification.

---

## Scope and boundaries

This plan implements the approved Phase 8 design in `docs/superpowers/specs/2026-05-23-campushub-phase8-payment-center-design.md`.

In scope:

- Keep `mock` provider working for local/demo use.
- Add `payment-center` provider backed by API-Transfer-Station internal HTTP APIs.
- Add V11 schema for local payment orders and callback event idempotency.
- Extend service-fee payment flow to use payment orders.
- Add role-deposit payment orders and status transitions.
- Add internal callback endpoint with token/signature verification, amount/order/status checks, and idempotency.
- Add admin payment monitor APIs and frontend page/tab.
- Update `.env.prod.example`, README, and CLAUDE handoff.

Out of scope:

- CampusHub reading or storing Alipay private/public keys.
- Transaction principal escrow.
- Per-order deposit freeze.
- Automatic guarantee-deposit penalty/deduction.
- Refunds, financial settlement, invoices, or full reconciliation.
- Full JWT/RBAC hardening.

## File structure map

### Backend migration

- Create `backend/src/main/resources/db/migration/V11__payment_center_integration.sql` — extend `service_fee_records`, add `payment_orders`, add `payment_callback_events`, and add role-application payment status fields if missing.

### Backend payment package

- Modify `backend/src/main/java/com/campushub/payment/PaymentProvider.java` — provider interface for create/query and callback verification.
- Modify `backend/src/main/java/com/campushub/payment/PaymentRequest.java` — include business type, business id, payer id, callback URL, and expiration minutes.
- Modify `backend/src/main/java/com/campushub/payment/PaymentCreation.java` — include local order number, provider order number, payment URL, status, and expiration.
- Modify `backend/src/main/java/com/campushub/payment/PaymentStatus.java` — include status, paid time, failure reason, and provider order number.
- Modify `backend/src/main/java/com/campushub/payment/MockPaymentProvider.java` — implement the expanded interface while preserving mock success regression.
- Replace or repurpose `backend/src/main/java/com/campushub/payment/AlipayPaymentProvider.java` with `PaymentCenterProvider` semantics; if renaming is easier, create `PaymentCenterProvider.java` and leave `AlipayPaymentProvider` disabled.
- Create `backend/src/main/java/com/campushub/payment/PaymentCenterProperties.java` — `@ConfigurationProperties` for provider URLs, token/signing secret, callback URL, and expiration minutes.
- Create `backend/src/main/java/com/campushub/payment/PaymentOrder.java` — JPA entity for unified local payment orders.
- Create `backend/src/main/java/com/campushub/payment/PaymentOrderRepository.java` — order lookup by order number, provider order number, business target, and status.
- Create `backend/src/main/java/com/campushub/payment/PaymentOrderSummary.java` — DTO for user/admin order views.
- Create `backend/src/main/java/com/campushub/payment/PaymentCallbackEvent.java` — JPA entity for callback idempotency/audit.
- Create `backend/src/main/java/com/campushub/payment/PaymentCallbackEventRepository.java` — lookup by event id and list newest events.
- Create `backend/src/main/java/com/campushub/payment/PaymentCenterCallbackRequest.java` — callback DTO.
- Create `backend/src/main/java/com/campushub/payment/PaymentCallbackHeaders.java` — parsed callback authentication headers.
- Modify `backend/src/main/java/com/campushub/payment/PaymentService.java` — create/reuse payment orders, handle callback, mark business records paid/failed/expired, write wallet flow.
- Modify `backend/src/main/java/com/campushub/payment/PaymentController.java` — user-facing pay/status endpoints plus internal callback endpoint.
- Create `backend/src/main/java/com/campushub/payment/AdminPaymentController.java` — admin order and callback monitor endpoints.

### Backend identity package

- Modify `backend/src/main/java/com/campushub/identity/RoleApplication.java` — support `PENDING`/`PAID` deposit status and payment completion.
- Modify `backend/src/main/java/com/campushub/identity/RoleApplicationSummary.java` — expose deposit status and payment-related state.
- Modify `backend/src/main/java/com/campushub/identity/IdentityService.java` — create role applications as pending payment, approve auto roles after deposit paid, keep shop merchant pending review.
- Modify `backend/src/main/java/com/campushub/identity/IdentityController.java` — add deposit payment endpoint or delegate to `PaymentController` endpoint.

### Backend config/docs

- Modify `backend/src/main/resources/application.yml` — add safe defaults for payment provider and payment-center config keys.
- Modify `backend/src/main/resources/application-prod.yml` — wire payment-center env variables without secret values.
- Modify `.env.prod.example` — add placeholder payment-center variables only.
- Modify `README.md` — document Phase 8 behavior and payment boundary.
- Modify `CLAUDE.md` — add Phase 8 handoff after deployment verification.

### Backend tests

- Create `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java` — service-fee order creation, reuse, callback success, callback idempotency, amount mismatch.
- Create `backend/src/test/java/com/campushub/payment/PaymentCenterProviderTest.java` — provider config behavior and request/response mapping using a mock HTTP server if available; otherwise test configuration failure path directly.
- Create `backend/src/test/java/com/campushub/payment/RoleDepositPaymentIntegrationTest.java` — role deposit order creation and role application status after callback.

### Frontend

- Modify `frontend/src/api/campushub.ts` — add payment order, callback event, admin payment monitor APIs and types.
- Modify `frontend/src/views/WalletView.vue` — replace mock-specific service-fee action with provider-neutral pay/status action.
- Modify `frontend/src/views/RoleApplicationsView.vue` — show deposit payment state and create deposit payment order.
- Modify `frontend/src/views/AdminOperationsView.vue` or create `frontend/src/views/AdminPaymentView.vue` — admin payment monitor with order/callback tabs.
- Modify `frontend/src/router/index.ts` and `frontend/src/layouts/MainLayout.vue` only if a dedicated `/admin/payment` route is chosen.
- Modify `frontend/src/styles.css` only for payment-monitor responsive table/card polish.

---

## Task 1: Add payment-center schema

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__payment_center_integration.sql`
- Modify: `backend/src/main/java/com/campushub/payment/ServiceFeeRecord.java`
- Modify: `backend/src/main/java/com/campushub/payment/ServiceFeeSummary.java`
- Test: `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java`

- [ ] **Step 1: Write failing schema-mapping test**

Create `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java`:

```java
package com.campushub.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PaymentOrderServiceIntegrationTest {

    @Autowired ServiceFeeRecordRepository serviceFeeRecordRepository;
    @Autowired UserRepository userRepository;

    @Test
    void serviceFeeRecordStoresPaymentCenterMappingFields() {
        User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        ServiceFeeRecord fee = new ServiceFeeRecord("SF-PHASE8-001", payer, "GOODS_INTENT", 1L, new BigDecimal("1.00"));

        fee.attachPaymentOrder("CHP-PHASE8-001", "PAYMENT_CENTER", "ATS-PHASE8-001", "https://pay.example/phase8", java.time.LocalDateTime.now().plusMinutes(30));
        ServiceFeeRecord saved = serviceFeeRecordRepository.saveAndFlush(fee);

        assertThat(saved.getPaymentOrderNo()).isEqualTo("CHP-PHASE8-001");
        assertThat(saved.getPaymentProvider()).isEqualTo("PAYMENT_CENTER");
        assertThat(saved.getPaymentCenterOrderNo()).isEqualTo("ATS-PHASE8-001");
        assertThat(saved.getPayUrl()).isEqualTo("https://pay.example/phase8");
        assertThat(saved.getExpiresAt()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run when Maven is available, otherwise defer to server-side Docker build:

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#serviceFeeRecordStoresPaymentCenterMappingFields test
```

Expected: FAIL because `attachPaymentOrder` and mapping fields do not exist.

- [ ] **Step 3: Create V11 migration**

Create `backend/src/main/resources/db/migration/V11__payment_center_integration.sql`:

```sql
ALTER TABLE service_fee_records
    ADD COLUMN payment_order_no VARCHAR(64) NULL AFTER status,
    ADD COLUMN payment_provider VARCHAR(40) NULL AFTER payment_order_no,
    ADD COLUMN payment_center_order_no VARCHAR(80) NULL AFTER payment_provider,
    ADD COLUMN pay_url VARCHAR(1000) NULL AFTER payment_center_order_no,
    ADD COLUMN expires_at DATETIME NULL AFTER paid_at,
    ADD COLUMN failed_at DATETIME NULL AFTER expires_at,
    ADD COLUMN failure_reason VARCHAR(500) NULL AFTER failed_at,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER failure_reason,
    ADD UNIQUE KEY uk_service_fee_payment_order_no (payment_order_no),
    ADD INDEX idx_service_fee_payment_status_time (status, created_at);

CREATE TABLE payment_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    business_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_order_no VARCHAR(80) NULL,
    pay_url VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    expires_at DATETIME NULL,
    paid_at DATETIME NULL,
    failed_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_order_no UNIQUE (order_no),
    CONSTRAINT fk_payment_order_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    INDEX idx_payment_order_business (business_type, business_id),
    INDEX idx_payment_order_provider_order (provider_order_no),
    INDEX idx_payment_order_status_time (status, created_at)
);

CREATE TABLE payment_callback_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    provider_order_no VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    verified BOOLEAN NOT NULL,
    handled BOOLEAN NOT NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_callback_event_id UNIQUE (event_id),
    INDEX idx_payment_callback_order_time (order_no, created_at)
);

ALTER TABLE role_applications
    MODIFY COLUMN deposit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN deposit_payment_order_no VARCHAR(64) NULL AFTER deposit_status,
    ADD UNIQUE KEY uk_role_application_deposit_order (deposit_payment_order_no);
```

- [ ] **Step 4: Extend `ServiceFeeRecord`**

Modify `backend/src/main/java/com/campushub/payment/ServiceFeeRecord.java` by adding fields, getters, and state methods:

```java
@Column(name = "payment_order_no")
private String paymentOrderNo;

@Column(name = "payment_provider")
private String paymentProvider;

@Column(name = "payment_center_order_no")
private String paymentCenterOrderNo;

@Column(name = "pay_url")
private String payUrl;

@Column(name = "expires_at")
private LocalDateTime expiresAt;

@Column(name = "failed_at")
private LocalDateTime failedAt;

@Column(name = "failure_reason")
private String failureReason;

@Column(name = "updated_at", insertable = false, updatable = false)
private LocalDateTime updatedAt;

public String getPaymentOrderNo() {
    return paymentOrderNo;
}

public String getPaymentProvider() {
    return paymentProvider;
}

public String getPaymentCenterOrderNo() {
    return paymentCenterOrderNo;
}

public String getPayUrl() {
    return payUrl;
}

public LocalDateTime getExpiresAt() {
    return expiresAt;
}

public LocalDateTime getFailedAt() {
    return failedAt;
}

public String getFailureReason() {
    return failureReason;
}

public LocalDateTime getUpdatedAt() {
    return updatedAt;
}

public void attachPaymentOrder(String paymentOrderNo, String paymentProvider, String paymentCenterOrderNo, String payUrl, LocalDateTime expiresAt) {
    this.paymentOrderNo = paymentOrderNo;
    this.paymentProvider = paymentProvider;
    this.paymentCenterOrderNo = paymentCenterOrderNo;
    this.payUrl = payUrl;
    this.expiresAt = expiresAt;
}

public void markFailed(LocalDateTime failedAt, String failureReason) {
    this.status = "FAILED";
    this.failedAt = failedAt;
    this.failureReason = failureReason;
}

public void markExpired(LocalDateTime expiredAt) {
    this.status = "EXPIRED";
    this.failedAt = expiredAt;
    this.failureReason = "支付已过期";
}
```

- [ ] **Step 5: Extend `ServiceFeeSummary`**

Modify `backend/src/main/java/com/campushub/payment/ServiceFeeSummary.java` to include payment fields:

```java
public record ServiceFeeSummary(
        Long id,
        String feeNo,
        Long payerId,
        String payerNickname,
        String targetType,
        Long targetId,
        BigDecimal amount,
        String status,
        String paymentOrderNo,
        String paymentProvider,
        String paymentCenterOrderNo,
        String payUrl,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime failedAt,
        String failureReason) {

    public static ServiceFeeSummary from(ServiceFeeRecord fee) {
        return new ServiceFeeSummary(
                fee.getId(),
                fee.getFeeNo(),
                fee.getPayer().getId(),
                fee.getPayer().getNickname(),
                fee.getTargetType(),
                fee.getTargetId(),
                fee.getAmount(),
                fee.getStatus(),
                fee.getPaymentOrderNo(),
                fee.getPaymentProvider(),
                fee.getPaymentCenterOrderNo(),
                fee.getPayUrl(),
                fee.getCreatedAt(),
                fee.getPaidAt(),
                fee.getExpiresAt(),
                fee.getFailedAt(),
                fee.getFailureReason());
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#serviceFeeRecordStoresPaymentCenterMappingFields test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V11__payment_center_integration.sql backend/src/main/java/com/campushub/payment/ServiceFeeRecord.java backend/src/main/java/com/campushub/payment/ServiceFeeSummary.java backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java
git commit -m "add payment center schema foundation"
```

## Task 2: Add payment order and callback entities

**Files:**
- Create: `backend/src/main/java/com/campushub/payment/PaymentOrder.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentOrderRepository.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentOrderSummary.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentCallbackEvent.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentCallbackEventRepository.java`
- Test: `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java`

- [ ] **Step 1: Add failing entity persistence test**

Append to `PaymentOrderServiceIntegrationTest`:

```java
@Autowired PaymentOrderRepository paymentOrderRepository;
@Autowired PaymentCallbackEventRepository paymentCallbackEventRepository;

@Test
void paymentOrderAndCallbackEventPersistForIdempotency() {
    User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    PaymentOrder order = new PaymentOrder(
            "CHP-PHASE8-002",
            "SERVICE_FEE",
            1L,
            payer,
            new BigDecimal("1.00"),
            "MOCK",
            java.time.LocalDateTime.now().plusMinutes(30));
    order.attachProviderOrder("MOCK-PHASE8-002", "mock://pay/CHP-PHASE8-002");
    paymentOrderRepository.saveAndFlush(order);

    PaymentCallbackEvent event = new PaymentCallbackEvent(
            "evt-phase8-002",
            "CHP-PHASE8-002",
            "MOCK-PHASE8-002",
            "PAID",
            new BigDecimal("1.00"),
            true,
            true,
            null);
    paymentCallbackEventRepository.saveAndFlush(event);

    assertThat(paymentOrderRepository.findByOrderNo("CHP-PHASE8-002")).isPresent();
    assertThat(paymentCallbackEventRepository.findByEventId("evt-phase8-002")).isPresent();
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#paymentOrderAndCallbackEventPersistForIdempotency test
```

Expected: FAIL because payment-order entity/repositories do not exist.

- [ ] **Step 3: Create `PaymentOrder`**

Create `backend/src/main/java/com/campushub/payment/PaymentOrder.java`:

```java
package com.campushub.payment;

import com.campushub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_order_no")
    private String providerOrderNo;

    @Column(name = "pay_url")
    private String payUrl;

    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected PaymentOrder() {
    }

    public PaymentOrder(String orderNo, String businessType, Long businessId, User payer, BigDecimal amount, String provider, LocalDateTime expiresAt) {
        this.orderNo = orderNo;
        this.businessType = businessType;
        this.businessId = businessId;
        this.payer = payer;
        this.amount = amount;
        this.provider = provider;
        this.status = "PENDING";
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getBusinessType() { return businessType; }
    public Long getBusinessId() { return businessId; }
    public User getPayer() { return payer; }
    public BigDecimal getAmount() { return amount; }
    public String getProvider() { return provider; }
    public String getProviderOrderNo() { return providerOrderNo; }
    public String getPayUrl() { return payUrl; }
    public String getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void attachProviderOrder(String providerOrderNo, String payUrl) {
        this.providerOrderNo = providerOrderNo;
        this.payUrl = payUrl;
    }

    public void markPaid(LocalDateTime paidAt) {
        this.status = "PAID";
        this.paidAt = paidAt;
        this.failureReason = null;
    }

    public void markFailed(LocalDateTime failedAt, String failureReason) {
        this.status = "FAILED";
        this.failedAt = failedAt;
        this.failureReason = failureReason;
    }

    public void markExpired(LocalDateTime expiredAt) {
        this.status = "EXPIRED";
        this.failedAt = expiredAt;
        this.failureReason = "支付已过期";
    }
}
```

- [ ] **Step 4: Create repositories and DTO**

Create `PaymentOrderRepository.java`:

```java
package com.campushub.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @EntityGraph(attributePaths = "payer")
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    @EntityGraph(attributePaths = "payer")
    Optional<PaymentOrder> findByBusinessTypeAndBusinessIdAndStatus(String businessType, Long businessId, String status);

    @EntityGraph(attributePaths = "payer")
    List<PaymentOrder> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "payer")
    List<PaymentOrder> findTop200ByOrderByCreatedAtDesc();
}
```

Create `PaymentOrderSummary.java`:

```java
package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentOrderSummary(
        Long id,
        String orderNo,
        String businessType,
        Long businessId,
        Long payerId,
        String payerNickname,
        BigDecimal amount,
        String provider,
        String providerOrderNo,
        String payUrl,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime failedAt,
        String failureReason,
        LocalDateTime createdAt) {

    public static PaymentOrderSummary from(PaymentOrder order) {
        return new PaymentOrderSummary(
                order.getId(),
                order.getOrderNo(),
                order.getBusinessType(),
                order.getBusinessId(),
                order.getPayer().getId(),
                order.getPayer().getNickname(),
                order.getAmount(),
                order.getProvider(),
                order.getProviderOrderNo(),
                order.getPayUrl(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getFailedAt(),
                order.getFailureReason(),
                order.getCreatedAt());
    }
}
```

- [ ] **Step 5: Create callback event entity and repository**

Create `PaymentCallbackEvent.java`:

```java
package com.campushub.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_callback_events")
public class PaymentCallbackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "provider_order_no")
    private String providerOrderNo;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private boolean handled;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PaymentCallbackEvent() {
    }

    public PaymentCallbackEvent(String eventId, String orderNo, String providerOrderNo, String status, BigDecimal amount, boolean verified, boolean handled, String failureReason) {
        this.eventId = eventId;
        this.orderNo = orderNo;
        this.providerOrderNo = providerOrderNo;
        this.status = status;
        this.amount = amount;
        this.verified = verified;
        this.handled = handled;
        this.failureReason = failureReason;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getOrderNo() { return orderNo; }
    public String getProviderOrderNo() { return providerOrderNo; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public boolean isVerified() { return verified; }
    public boolean isHandled() { return handled; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

Create `PaymentCallbackEventRepository.java`:

```java
package com.campushub.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackEventRepository extends JpaRepository<PaymentCallbackEvent, Long> {
    Optional<PaymentCallbackEvent> findByEventId(String eventId);
    List<PaymentCallbackEvent> findTop200ByOrderByCreatedAtDesc();
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#paymentOrderAndCallbackEventPersistForIdempotency test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/payment/PaymentOrder.java backend/src/main/java/com/campushub/payment/PaymentOrderRepository.java backend/src/main/java/com/campushub/payment/PaymentOrderSummary.java backend/src/main/java/com/campushub/payment/PaymentCallbackEvent.java backend/src/main/java/com/campushub/payment/PaymentCallbackEventRepository.java backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java
git commit -m "add payment order audit entities"
```

## Task 3: Expand payment provider contract and add payment-center provider

**Files:**
- Modify: `backend/src/main/java/com/campushub/payment/PaymentProvider.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentRequest.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentCreation.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentStatus.java`
- Modify: `backend/src/main/java/com/campushub/payment/MockPaymentProvider.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentCenterProperties.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentCenterProvider.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-prod.yml`
- Test: `backend/src/test/java/com/campushub/payment/PaymentCenterProviderTest.java`

- [ ] **Step 1: Write failing provider configuration test**

Create `backend/src/test/java/com/campushub/payment/PaymentCenterProviderTest.java`:

```java
package com.campushub.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PaymentCenterProviderTest {

    @Test
    void paymentCenterProviderFailsClearlyWhenCreateUrlIsMissing() {
        PaymentCenterProperties properties = new PaymentCenterProperties();
        properties.setBaseUrl("https://payment-center.invalid");
        properties.setCallbackUrl("https://campushub.invalid/api/payment/callbacks/payment-center");
        properties.setCallbackToken("internal-token");
        properties.setSigningSecret("internal-secret");
        properties.setExpireMinutes(30);
        PaymentCenterProvider provider = new PaymentCenterProvider(properties, RestClient.builder());

        PaymentRequest request = new PaymentRequest(
                "CHP-PHASE8-003",
                "SERVICE_FEE",
                1L,
                2L,
                "SF-PHASE8-003",
                new BigDecimal("1.00"),
                "CampusHub 服务费 SF-PHASE8-003",
                "https://campushub.invalid/api/payment/callbacks/payment-center",
                30);

        assertThatThrownBy(() -> provider.createWebPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付中心创建路径未配置");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=PaymentCenterProviderTest test
```

Expected: FAIL because `PaymentCenterProperties`, `PaymentCenterProvider`, and expanded `PaymentRequest` do not exist.

- [ ] **Step 3: Expand request/response records**

Replace `PaymentRequest.java` with:

```java
package com.campushub.payment;

import java.math.BigDecimal;

public record PaymentRequest(
        String orderNo,
        String businessType,
        Long businessId,
        Long payerId,
        String businessNo,
        BigDecimal amount,
        String subject,
        String callbackUrl,
        int expireMinutes) {
}
```

Replace `PaymentCreation.java` with:

```java
package com.campushub.payment;

import java.time.LocalDateTime;

public record PaymentCreation(
        String provider,
        String orderNo,
        String providerOrderNo,
        String payUrl,
        String status,
        LocalDateTime expiresAt,
        String message) {
}
```

Replace `PaymentStatus.java` with:

```java
package com.campushub.payment;

import java.time.LocalDateTime;

public record PaymentStatus(
        String provider,
        String orderNo,
        String providerOrderNo,
        String status,
        LocalDateTime paidAt,
        String failureReason,
        String message) {
}
```

- [ ] **Step 4: Update `PaymentProvider`**

Replace `PaymentProvider.java` with:

```java
package com.campushub.payment;

public interface PaymentProvider {

    String providerName();

    PaymentCreation createWebPayment(PaymentRequest request);

    PaymentStatus queryPaymentStatus(String orderNo);
}
```

- [ ] **Step 5: Update mock provider**

Modify `MockPaymentProvider.java` to match the expanded records:

```java
@Override
public PaymentCreation createWebPayment(PaymentRequest request) {
    String providerOrderNo = "MOCK-" + request.orderNo();
    mockStatuses.put(request.orderNo(), "PENDING");
    return new PaymentCreation(
            providerName(),
            request.orderNo(),
            providerOrderNo,
            "mock://pay/" + request.orderNo(),
            "PENDING",
            java.time.LocalDateTime.now().plusMinutes(request.expireMinutes()),
            "本地模拟支付单已创建");
}

@Override
public PaymentStatus queryPaymentStatus(String orderNo) {
    String status = mockStatuses.getOrDefault(orderNo, "PENDING");
    return new PaymentStatus(providerName(), orderNo, "MOCK-" + orderNo, status, "SUCCESS".equals(status) ? java.time.LocalDateTime.now() : null, null, "本地模拟支付状态");
}

public void markSuccess(String orderNo) {
    mockStatuses.put(orderNo, "SUCCESS");
}
```

Keep existing dependency injection and conditional property annotations.

- [ ] **Step 6: Add payment-center properties**

Create `PaymentCenterProperties.java`:

```java
package com.campushub.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "campushub.payment.center")
public class PaymentCenterProperties {
    private String baseUrl;
    private String createPath;
    private String callbackUrl;
    private String callbackToken;
    private String signingSecret;
    private int expireMinutes = 30;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCreatePath() { return createPath; }
    public void setCreatePath(String createPath) { this.createPath = createPath; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public String getCallbackToken() { return callbackToken; }
    public void setCallbackToken(String callbackToken) { this.callbackToken = callbackToken; }
    public String getSigningSecret() { return signingSecret; }
    public void setSigningSecret(String signingSecret) { this.signingSecret = signingSecret; }
    public int getExpireMinutes() { return expireMinutes; }
    public void setExpireMinutes(int expireMinutes) { this.expireMinutes = expireMinutes; }
}
```

- [ ] **Step 7: Add `PaymentCenterProvider`**

Create `PaymentCenterProvider.java`:

```java
package com.campushub.payment;

import com.campushub.common.BusinessException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "payment-center")
public class PaymentCenterProvider implements PaymentProvider {

    private final PaymentCenterProperties properties;
    private final RestClient restClient;

    public PaymentCenterProvider(PaymentCenterProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public String providerName() {
        return "PAYMENT_CENTER";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        validateCreateConfig();
        PaymentCenterCreateResponse response = restClient.post()
                .uri(properties.getBaseUrl() + properties.getCreatePath())
                .header("X-CampusHub-Payment-Token", properties.getCallbackToken())
                .body(Map.of(
                        "app", "campushub",
                        "orderNo", request.orderNo(),
                        "businessType", request.businessType(),
                        "businessId", request.businessId(),
                        "payerId", request.payerId(),
                        "amount", request.amount().toPlainString(),
                        "subject", request.subject(),
                        "callbackUrl", request.callbackUrl(),
                        "expireMinutes", request.expireMinutes()))
                .retrieve()
                .body(PaymentCenterCreateResponse.class);
        if (response == null || !StringUtils.hasText(response.paymentCenterOrderNo())) {
            throw new BusinessException("支付中心未返回有效支付单");
        }
        return new PaymentCreation(providerName(), request.orderNo(), response.paymentCenterOrderNo(), response.payUrl(), response.status(), response.expiresAt(), "支付中心收款单已创建");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String orderNo) {
        throw new BusinessException("支付中心主动查询暂未启用，请以内部回调为准");
    }

    private void validateCreateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException("支付中心地址未配置");
        }
        if (!StringUtils.hasText(properties.getCreatePath())) {
            throw new BusinessException("支付中心创建路径未配置");
        }
        if (!StringUtils.hasText(properties.getCallbackUrl())) {
            throw new BusinessException("支付中心回调地址未配置");
        }
        if (!StringUtils.hasText(properties.getCallbackToken())) {
            throw new BusinessException("支付中心内部 token 未配置");
        }
    }

    private record PaymentCenterCreateResponse(String paymentCenterOrderNo, String payUrl, String status, java.time.LocalDateTime expiresAt) {
    }
}
```

- [ ] **Step 8: Disable direct Alipay provider path**

Modify `AlipayPaymentProvider.java` conditional so it cannot be accidentally selected by production config:

```java
@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "direct-alipay-disabled")
public class AlipayPaymentProvider implements PaymentProvider {
```

Update its methods to return the expanded `PaymentStatus` signature or leave throwing `BusinessException` after compilation updates.

- [ ] **Step 9: Add safe config keys**

In `backend/src/main/resources/application.yml`, add under `campushub`:

```yaml
payment:
  provider: ${CAMPUSHUB_PAYMENT_PROVIDER:mock}
  center:
    base-url: ${CAMPUSHUB_PAYMENT_CENTER_BASE_URL:}
    create-path: ${CAMPUSHUB_PAYMENT_CENTER_CREATE_PATH:}
    callback-url: ${CAMPUSHUB_PAYMENT_CENTER_CALLBACK_URL:http://localhost:8080/api/payment/callbacks/payment-center}
    callback-token: ${CAMPUSHUB_PAYMENT_CENTER_CALLBACK_TOKEN:}
    signing-secret: ${CAMPUSHUB_PAYMENT_CENTER_SIGNING_SECRET:}
    expire-minutes: ${CAMPUSHUB_PAYMENT_ORDER_EXPIRE_MINUTES:30}
```

In `application-prod.yml`, add the same env-variable-driven block if prod overrides currently define payment separately.

- [ ] **Step 10: Run provider test**

```bash
mvn -f backend/pom.xml -Dtest=PaymentCenterProviderTest test
```

Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/campushub/payment/PaymentProvider.java backend/src/main/java/com/campushub/payment/PaymentRequest.java backend/src/main/java/com/campushub/payment/PaymentCreation.java backend/src/main/java/com/campushub/payment/PaymentStatus.java backend/src/main/java/com/campushub/payment/MockPaymentProvider.java backend/src/main/java/com/campushub/payment/PaymentCenterProperties.java backend/src/main/java/com/campushub/payment/PaymentCenterProvider.java backend/src/main/java/com/campushub/payment/AlipayPaymentProvider.java backend/src/main/resources/application.yml backend/src/main/resources/application-prod.yml backend/src/test/java/com/campushub/payment/PaymentCenterProviderTest.java
git commit -m "add payment center provider"
```

## Task 4: Implement service-fee payment orders and callback handling

**Files:**
- Create: `backend/src/main/java/com/campushub/payment/PaymentCenterCallbackRequest.java`
- Create: `backend/src/main/java/com/campushub/payment/PaymentCallbackHeaders.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentService.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentController.java`
- Test: `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java`

- [ ] **Step 1: Add failing service-fee payment tests**

Append to `PaymentOrderServiceIntegrationTest`:

```java
@Autowired PaymentService paymentService;

@Test
void createsReusableServiceFeePaymentOrder() {
    User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    ServiceFeeRecord fee = serviceFeeRecordRepository.saveAndFlush(new ServiceFeeRecord("SF-PHASE8-004", payer, "GOODS_INTENT", 1L, new BigDecimal("1.00")));

    PaymentCreation first = paymentService.createServiceFeePayment(fee.getId());
    PaymentCreation second = paymentService.createServiceFeePayment(fee.getId());

    assertThat(first.orderNo()).isEqualTo(second.orderNo());
    assertThat(first.status()).isEqualTo("PENDING");
}

@Test
void paidCallbackMarksServiceFeePaidOnce() {
    User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    ServiceFeeRecord fee = serviceFeeRecordRepository.saveAndFlush(new ServiceFeeRecord("SF-PHASE8-005", payer, "GOODS_INTENT", 1L, new BigDecimal("1.00")));
    PaymentCreation creation = paymentService.createServiceFeePayment(fee.getId());

    PaymentCenterCallbackRequest callback = new PaymentCenterCallbackRequest(
            "evt-phase8-005",
            creation.orderNo(),
            creation.providerOrderNo(),
            "SERVICE_FEE",
            fee.getId(),
            new BigDecimal("1.00"),
            "PAID",
            java.time.LocalDateTime.now(),
            null);
    paymentService.handlePaymentCenterCallback(callback, new PaymentCallbackHeaders("internal-token", null, null), "internal-token");
    paymentService.handlePaymentCenterCallback(callback, new PaymentCallbackHeaders("internal-token", null, null), "internal-token");

    ServiceFeeRecord updated = serviceFeeRecordRepository.findById(fee.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo("PAID");
    assertThat(paymentCallbackEventRepository.findByEventId("evt-phase8-005")).isPresent();
}

@Test
void callbackRejectsAmountMismatch() {
    User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    ServiceFeeRecord fee = serviceFeeRecordRepository.saveAndFlush(new ServiceFeeRecord("SF-PHASE8-006", payer, "GOODS_INTENT", 1L, new BigDecimal("1.00")));
    PaymentCreation creation = paymentService.createServiceFeePayment(fee.getId());

    PaymentCenterCallbackRequest callback = new PaymentCenterCallbackRequest(
            "evt-phase8-006",
            creation.orderNo(),
            creation.providerOrderNo(),
            "SERVICE_FEE",
            fee.getId(),
            new BigDecimal("2.00"),
            "PAID",
            java.time.LocalDateTime.now(),
            null);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> paymentService.handlePaymentCenterCallback(callback, new PaymentCallbackHeaders("internal-token", null, null), "internal-token"))
            .isInstanceOf(com.campushub.common.BusinessException.class)
            .hasMessageContaining("支付金额不匹配");
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest test
```

Expected: FAIL because payment-order service methods and callback DTOs do not exist.

- [ ] **Step 3: Create callback DTOs**

Create `PaymentCenterCallbackRequest.java`:

```java
package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCenterCallbackRequest(
        String eventId,
        String orderNo,
        String paymentCenterOrderNo,
        String businessType,
        Long businessId,
        BigDecimal amount,
        String status,
        LocalDateTime paidAt,
        String failureReason) {
}
```

Create `PaymentCallbackHeaders.java`:

```java
package com.campushub.payment;

public record PaymentCallbackHeaders(String token, String signature, String timestamp) {
}
```

- [ ] **Step 4: Rewrite service-fee payment creation in `PaymentService`**

Update `PaymentService` constructor to inject:

```java
private final PaymentOrderRepository paymentOrderRepository;
private final PaymentCallbackEventRepository paymentCallbackEventRepository;
private final PaymentCenterProperties paymentCenterProperties;
```

Implement `createServiceFeePayment`:

```java
@Transactional
public PaymentCreation createServiceFeePayment(Long serviceFeeId) {
    ServiceFeeRecord fee = findServiceFee(serviceFeeId);
    if ("PAID".equals(fee.getStatus())) {
        throw new BusinessException("服务费已支付");
    }
    PaymentOrder order = paymentOrderRepository.findByBusinessTypeAndBusinessIdAndStatus("SERVICE_FEE", fee.getId(), "PENDING")
            .orElseGet(() -> paymentOrderRepository.save(new PaymentOrder(
                    nextOrderNo("SF", fee.getId()),
                    "SERVICE_FEE",
                    fee.getId(),
                    fee.getPayer(),
                    fee.getAmount(),
                    paymentProvider.providerName(),
                    LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes()))));
    if (order.getProviderOrderNo() == null) {
        PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(
                order.getOrderNo(),
                order.getBusinessType(),
                order.getBusinessId(),
                fee.getPayer().getId(),
                fee.getFeeNo(),
                fee.getAmount(),
                "CampusHub 服务费 " + fee.getFeeNo(),
                paymentCenterProperties.getCallbackUrl(),
                paymentCenterProperties.getExpireMinutes()));
        order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
        fee.attachPaymentOrder(order.getOrderNo(), order.getProvider(), creation.providerOrderNo(), creation.payUrl(), creation.expiresAt());
        paymentOrderRepository.save(order);
        serviceFeeRecordRepository.save(fee);
        return creation;
    }
    return PaymentOrderSummary.toCreation(order);
}
```

Add helper methods:

```java
private String nextOrderNo(String prefix, Long id) {
    return "CHP-" + prefix + "-" + id + "-" + System.currentTimeMillis();
}
```

If `PaymentOrderSummary.toCreation` is not desired, add this static method there:

```java
public static PaymentCreation toCreation(PaymentOrder order) {
    return new PaymentCreation(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getPayUrl(), order.getStatus(), order.getExpiresAt(), "复用待支付订单");
}
```

- [ ] **Step 5: Implement callback handling**

Add to `PaymentService`:

```java
@Transactional
public PaymentStatus handlePaymentCenterCallback(PaymentCenterCallbackRequest request, PaymentCallbackHeaders headers, String expectedToken) {
    if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(headers.token())) {
        paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), false, false, "内部 token 校验失败"));
        throw new BusinessException("支付回调鉴权失败");
    }
    if (paymentCallbackEventRepository.findByEventId(request.eventId()).isPresent()) {
        return new PaymentStatus(paymentProvider.providerName(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.paidAt(), null, "重复回调已忽略");
    }
    PaymentOrder order = paymentOrderRepository.findByOrderNo(request.orderNo())
            .orElseThrow(() -> new BusinessException("支付订单不存在"));
    validateCallback(request, order);
    if ("PAID".equals(order.getStatus()) && "PAID".equals(request.status())) {
        paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, true, null));
        return new PaymentStatus(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getStatus(), order.getPaidAt(), null, "支付订单已处理");
    }
    if (!"PENDING".equals(order.getStatus())) {
        paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, false, "订单状态不允许流转"));
        throw new BusinessException("订单状态不允许流转");
    }
    applyBusinessPaymentResult(order, request);
    paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, true, null));
    return new PaymentStatus(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getStatus(), order.getPaidAt(), order.getFailureReason(), "支付回调已处理");
}

private void validateCallback(PaymentCenterCallbackRequest request, PaymentOrder order) {
    if (!order.getBusinessType().equals(request.businessType()) || !order.getBusinessId().equals(request.businessId())) {
        throw new BusinessException("支付业务对象不匹配");
    }
    if (order.getProviderOrderNo() != null && !order.getProviderOrderNo().equals(request.paymentCenterOrderNo())) {
        throw new BusinessException("支付中心订单号不匹配");
    }
    if (order.getAmount().compareTo(request.amount()) != 0) {
        throw new BusinessException("支付金额不匹配");
    }
}
```

Implement `applyBusinessPaymentResult` for service fees:

```java
private void applyBusinessPaymentResult(PaymentOrder order, PaymentCenterCallbackRequest request) {
    if ("PAID".equals(request.status())) {
        order.markPaid(request.paidAt() != null ? request.paidAt() : LocalDateTime.now());
        if ("SERVICE_FEE".equals(order.getBusinessType())) {
            markServiceFeePaid(order);
        }
        return;
    }
    if ("FAILED".equals(request.status())) {
        order.markFailed(LocalDateTime.now(), request.failureReason());
        if ("SERVICE_FEE".equals(order.getBusinessType())) {
            ServiceFeeRecord fee = findServiceFee(order.getBusinessId());
            fee.markFailed(LocalDateTime.now(), request.failureReason());
        }
        return;
    }
    if ("EXPIRED".equals(request.status())) {
        order.markExpired(LocalDateTime.now());
        if ("SERVICE_FEE".equals(order.getBusinessType())) {
            ServiceFeeRecord fee = findServiceFee(order.getBusinessId());
            fee.markExpired(LocalDateTime.now());
        }
        return;
    }
    throw new BusinessException("未知支付状态");
}
```

Move existing paid/wallet-flow logic into `markServiceFeePaid(PaymentOrder order)` and use `order.getOrderNo()` for idempotent flow number:

```java
private void markServiceFeePaid(PaymentOrder order) {
    ServiceFeeRecord fee = findServiceFee(order.getBusinessId());
    if (!"PAID".equals(fee.getStatus())) {
        fee.markPaid(order.getPaidAt());
        serviceFeeRecordRepository.save(fee);
        WalletAccount wallet = walletAccountRepository.findByUserId(fee.getPayer().getId())
                .orElseThrow(() -> new BusinessException("付款用户钱包不存在"));
        walletFlowRepository.save(new WalletFlow(
                wallet,
                fee.getPayer(),
                "WF-FEE-" + order.getOrderNo(),
                "OUT",
                fee.getAmount(),
                wallet.getBalance(),
                "SERVICE_FEE",
                fee.getId(),
                "支付平台服务费"));
    }
}
```

- [ ] **Step 6: Update payment controller endpoints**

Modify `PaymentController.java`:

```java
@PostMapping("/service-fees/{feeId}/pay")
public ApiResponse<PaymentCreation> createServiceFeePayment(@PathVariable Long feeId) {
    return ApiResponse.ok(paymentService.createServiceFeePayment(feeId));
}

@GetMapping("/orders/{orderNo}")
public ApiResponse<PaymentOrderSummary> getOrder(@PathVariable String orderNo) {
    return ApiResponse.ok(paymentService.getOrder(orderNo));
}

@PostMapping("/callbacks/payment-center")
public ApiResponse<PaymentStatus> handlePaymentCenterCallback(
        @RequestHeader(name = "X-CampusHub-Payment-Token", required = false) String token,
        @RequestHeader(name = "X-CampusHub-Payment-Signature", required = false) String signature,
        @RequestHeader(name = "X-CampusHub-Payment-Timestamp", required = false) String timestamp,
        @RequestBody PaymentCenterCallbackRequest request) {
    return ApiResponse.ok(paymentService.handlePaymentCenterCallback(
            request,
            new PaymentCallbackHeaders(token, signature, timestamp),
            paymentCenterProperties.getCallbackToken()));
}
```

Inject `PaymentCenterProperties` into the controller. Keep `/mock-pay` and `/mock-success` aliases during Phase 8, but make them delegate to the provider-neutral flow where possible.

- [ ] **Step 7: Add `getOrder` service method**

Add to `PaymentService`:

```java
@Transactional(readOnly = true)
public PaymentOrderSummary getOrder(String orderNo) {
    return paymentOrderRepository.findByOrderNo(orderNo)
            .map(PaymentOrderSummary::from)
            .orElseThrow(() -> new BusinessException("支付订单不存在"));
}
```

- [ ] **Step 8: Run payment service tests**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/campushub/payment/PaymentCenterCallbackRequest.java backend/src/main/java/com/campushub/payment/PaymentCallbackHeaders.java backend/src/main/java/com/campushub/payment/PaymentService.java backend/src/main/java/com/campushub/payment/PaymentController.java backend/src/main/java/com/campushub/payment/PaymentOrderSummary.java backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java
git commit -m "implement payment center callback handling"
```

## Task 5: Add role-deposit payment flow

**Files:**
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplication.java`
- Modify: `backend/src/main/java/com/campushub/identity/RoleApplicationSummary.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityService.java`
- Modify: `backend/src/main/java/com/campushub/identity/IdentityController.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentService.java`
- Test: `backend/src/test/java/com/campushub/payment/RoleDepositPaymentIntegrationTest.java`

- [ ] **Step 1: Write failing role-deposit test**

Create `backend/src/test/java/com/campushub/payment/RoleDepositPaymentIntegrationTest.java`:

```java
package com.campushub.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoleDepositPaymentIntegrationTest {

    @Autowired IdentityService identityService;
    @Autowired PaymentService paymentService;
    @Autowired RoleApplicationRepository roleApplicationRepository;
    @Autowired UserRepository userRepository;

    @Test
    void runnerRoleApprovesAfterDepositPaymentCallback() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        var application = identityService.apply(user.getId(), new ApplyRoleRequest("RUNNER", "愿意跑腿"));

        assertThat(application.depositStatus()).isEqualTo("PENDING");
        assertThat(application.reviewStatus()).isEqualTo("PENDING_PAYMENT");

        PaymentCreation creation = paymentService.createRoleDepositPayment(application.id());
        paymentService.handlePaymentCenterCallback(new PaymentCenterCallbackRequest(
                "evt-role-phase8-001",
                creation.orderNo(),
                creation.providerOrderNo(),
                "ROLE_DEPOSIT",
                application.id(),
                new BigDecimal("5.00"),
                "PAID",
                java.time.LocalDateTime.now(),
                null), new PaymentCallbackHeaders("internal-token", null, null), "internal-token");

        var updated = roleApplicationRepository.findById(application.id()).orElseThrow();
        assertThat(updated.getDepositStatus()).isEqualTo("PAID");
        assertThat(updated.getReviewStatus()).isEqualTo("APPROVED");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=RoleDepositPaymentIntegrationTest test
```

Expected: FAIL because role deposits are still auto-paid and `createRoleDepositPayment` does not exist.

- [ ] **Step 3: Update `RoleApplication`**

Add field mapping and methods:

```java
@Column(name = "deposit_payment_order_no")
private String depositPaymentOrderNo;

public String getDepositPaymentOrderNo() {
    return depositPaymentOrderNo;
}

public void attachDepositPaymentOrder(String orderNo) {
    this.depositPaymentOrderNo = orderNo;
}

public void markDepositPending() {
    this.depositStatus = "PENDING";
    this.reviewStatus = "PENDING_PAYMENT";
}

public void markDepositPaid() {
    this.depositStatus = "PAID";
    if ("RUNNER".equals(roleType) || "GOODS_PUBLISHER".equals(roleType)) {
        this.reviewStatus = "APPROVED";
        this.reviewedAt = LocalDateTime.now();
    } else if ("SHOP_MERCHANT".equals(roleType)) {
        this.reviewStatus = "PENDING_REVIEW";
    }
}
```

- [ ] **Step 4: Update `RoleApplicationSummary`**

Ensure the record includes `depositPaymentOrderNo`:

```java
String depositPaymentOrderNo
```

and `from(RoleApplication application)` passes `application.getDepositPaymentOrderNo()`.

- [ ] **Step 5: Change role application creation to pending payment**

Modify `IdentityService.apply` so new applications are not auto-paid:

```java
RoleApplication application = new RoleApplication(user, roleType.name(), roleType.depositAmount(), "PENDING", "PENDING_PAYMENT", request.applyNote());
return RoleApplicationSummary.from(roleApplicationRepository.save(application));
```

If constructor currently enforces status names, add an overload or adjust the existing constructor.

- [ ] **Step 6: Add role-deposit payment creation**

Add to `PaymentService`:

```java
@Transactional
public PaymentCreation createRoleDepositPayment(Long applicationId) {
    RoleApplication application = roleApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("身份申请不存在"));
    if ("PAID".equals(application.getDepositStatus())) {
        throw new BusinessException("身份保证金已支付");
    }
    PaymentOrder order = paymentOrderRepository.findByBusinessTypeAndBusinessIdAndStatus("ROLE_DEPOSIT", application.getId(), "PENDING")
            .orElseGet(() -> paymentOrderRepository.save(new PaymentOrder(
                    nextOrderNo("RD", application.getId()),
                    "ROLE_DEPOSIT",
                    application.getId(),
                    application.getUser(),
                    application.getDepositAmount(),
                    paymentProvider.providerName(),
                    LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes()))));
    if (order.getProviderOrderNo() == null) {
        PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(
                order.getOrderNo(),
                order.getBusinessType(),
                order.getBusinessId(),
                application.getUser().getId(),
                application.getRoleType(),
                application.getDepositAmount(),
                "CampusHub 身份保证金 " + application.getRoleType(),
                paymentCenterProperties.getCallbackUrl(),
                paymentCenterProperties.getExpireMinutes()));
        order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
        application.attachDepositPaymentOrder(order.getOrderNo());
        paymentOrderRepository.save(order);
        roleApplicationRepository.save(application);
        return creation;
    }
    return PaymentOrderSummary.toCreation(order);
}
```

Inject `RoleApplicationRepository` into `PaymentService`.

- [ ] **Step 7: Extend callback business application**

In `applyBusinessPaymentResult`, add:

```java
if ("ROLE_DEPOSIT".equals(order.getBusinessType())) {
    markRoleDepositPaid(order);
}
```

Implement:

```java
private void markRoleDepositPaid(PaymentOrder order) {
    RoleApplication application = roleApplicationRepository.findById(order.getBusinessId())
            .orElseThrow(() -> new BusinessException("身份申请不存在"));
    if (!"PAID".equals(application.getDepositStatus())) {
        application.markDepositPaid();
        roleApplicationRepository.save(application);
    }
}
```

- [ ] **Step 8: Add role deposit endpoint**

In `IdentityController.java`, add:

```java
@PostMapping("/roles/{applicationId}/deposit-pay")
public ApiResponse<PaymentCreation> createDepositPayment(@PathVariable Long applicationId) {
    return ApiResponse.ok(paymentService.createRoleDepositPayment(applicationId));
}
```

Inject `PaymentService` into `IdentityController`.

- [ ] **Step 9: Run role-deposit tests**

```bash
mvn -f backend/pom.xml -Dtest=RoleDepositPaymentIntegrationTest test
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/campushub/identity/RoleApplication.java backend/src/main/java/com/campushub/identity/RoleApplicationSummary.java backend/src/main/java/com/campushub/identity/IdentityService.java backend/src/main/java/com/campushub/identity/IdentityController.java backend/src/main/java/com/campushub/payment/PaymentService.java backend/src/test/java/com/campushub/payment/RoleDepositPaymentIntegrationTest.java
git commit -m "add role deposit payment flow"
```

## Task 6: Add admin payment monitor APIs

**Files:**
- Create: `backend/src/main/java/com/campushub/payment/PaymentCallbackEventSummary.java`
- Create: `backend/src/main/java/com/campushub/payment/AdminPaymentController.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentOrderRepository.java`
- Test: `backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java`

- [ ] **Step 1: Add failing admin-list test**

Append to `PaymentOrderServiceIntegrationTest`:

```java
@Autowired AdminPaymentController adminPaymentController;

@Test
void adminPaymentMonitorListsOrdersAndCallbackEvents() {
    User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    PaymentOrder order = paymentOrderRepository.saveAndFlush(new PaymentOrder(
            "CHP-PHASE8-ADMIN",
            "SERVICE_FEE",
            1L,
            payer,
            new BigDecimal("1.00"),
            "MOCK",
            java.time.LocalDateTime.now().plusMinutes(30)));
    paymentCallbackEventRepository.saveAndFlush(new PaymentCallbackEvent(
            "evt-phase8-admin",
            order.getOrderNo(),
            "MOCK-ADMIN",
            "PENDING",
            new BigDecimal("1.00"),
            true,
            true,
            null));

    assertThat(adminPaymentController.listOrders(null).data()).isNotEmpty();
    assertThat(adminPaymentController.listCallbackEvents().data()).isNotEmpty();
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#adminPaymentMonitorListsOrdersAndCallbackEvents test
```

Expected: FAIL because admin controller and callback summary do not exist.

- [ ] **Step 3: Add callback event summary**

Create `PaymentCallbackEventSummary.java`:

```java
package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCallbackEventSummary(
        Long id,
        String eventId,
        String orderNo,
        String providerOrderNo,
        String status,
        BigDecimal amount,
        boolean verified,
        boolean handled,
        String failureReason,
        LocalDateTime createdAt) {

    public static PaymentCallbackEventSummary from(PaymentCallbackEvent event) {
        return new PaymentCallbackEventSummary(
                event.getId(),
                event.getEventId(),
                event.getOrderNo(),
                event.getProviderOrderNo(),
                event.getStatus(),
                event.getAmount(),
                event.isVerified(),
                event.isHandled(),
                event.getFailureReason(),
                event.getCreatedAt());
    }
}
```

- [ ] **Step 4: Add repository status filter helper**

Ensure `PaymentOrderRepository` has:

```java
List<PaymentOrder> findByStatusOrderByCreatedAtDesc(String status);
List<PaymentOrder> findTop200ByOrderByCreatedAtDesc();
```

- [ ] **Step 5: Add admin controller**

Create `AdminPaymentController.java`:

```java
package com.campushub.payment;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payment")
public class AdminPaymentController {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentCallbackEventRepository callbackEventRepository;

    public AdminPaymentController(PaymentOrderRepository paymentOrderRepository, PaymentCallbackEventRepository callbackEventRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.callbackEventRepository = callbackEventRepository;
    }

    @GetMapping("/orders")
    public ApiResponse<List<PaymentOrderSummary>> listOrders(@RequestParam(required = false) String status) {
        List<PaymentOrder> orders = status == null || status.isBlank()
                ? paymentOrderRepository.findTop200ByOrderByCreatedAtDesc()
                : paymentOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        return ApiResponse.ok(orders.stream().map(PaymentOrderSummary::from).toList());
    }

    @GetMapping("/callback-events")
    public ApiResponse<List<PaymentCallbackEventSummary>> listCallbackEvents() {
        return ApiResponse.ok(callbackEventRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(PaymentCallbackEventSummary::from)
                .toList());
    }
}
```

- [ ] **Step 6: Run admin API test**

```bash
mvn -f backend/pom.xml -Dtest=PaymentOrderServiceIntegrationTest#adminPaymentMonitorListsOrdersAndCallbackEvents test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/payment/PaymentCallbackEventSummary.java backend/src/main/java/com/campushub/payment/AdminPaymentController.java backend/src/main/java/com/campushub/payment/PaymentOrderRepository.java backend/src/test/java/com/campushub/payment/PaymentOrderServiceIntegrationTest.java
git commit -m "add admin payment monitor APIs"
```

## Task 7: Add frontend payment APIs and user payment UX

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/WalletView.vue`
- Modify: `frontend/src/views/RoleApplicationsView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add frontend payment types and API functions**

In `frontend/src/api/campushub.ts`, add:

```ts
export interface PaymentCreation {
  provider: string
  orderNo: string
  providerOrderNo: string
  payUrl: string
  status: string
  expiresAt: string
  message: string
}

export interface PaymentOrderSummary {
  id: number
  orderNo: string
  businessType: string
  businessId: number
  payerId: number
  payerNickname: string
  amount: number
  provider: string
  providerOrderNo?: string
  payUrl?: string
  status: string
  expiresAt?: string
  paidAt?: string
  failedAt?: string
  failureReason?: string
  createdAt?: string
}

export interface PaymentCallbackEventSummary {
  id: number
  eventId: string
  orderNo: string
  providerOrderNo?: string
  status: string
  amount: number
  verified: boolean
  handled: boolean
  failureReason?: string
  createdAt?: string
}

export function createServiceFeePayment(feeId: number) {
  return api.post<PaymentCreation>(`/payment/service-fees/${feeId}/pay`)
}

export function getPaymentOrder(orderNo: string) {
  return api.get<PaymentOrderSummary>(`/payment/orders/${orderNo}`)
}

export function createRoleDepositPayment(applicationId: number) {
  return api.post<PaymentCreation>(`/identity/roles/${applicationId}/deposit-pay`)
}

export function listAdminPaymentOrders(status?: string) {
  return api.get<PaymentOrderSummary[]>('/admin/payment/orders', { params: { status } })
}

export function listAdminPaymentCallbackEvents() {
  return api.get<PaymentCallbackEventSummary[]>('/admin/payment/callback-events')
}
```

- [ ] **Step 2: Update wallet payment action**

In `WalletView.vue`, replace calls to mock-only service-fee payment endpoints with `createServiceFeePayment`. On success, show:

```ts
ElMessage.success(result.message || '支付单已创建')
if (result.payUrl) {
  window.open(result.payUrl, '_blank', 'noopener,noreferrer')
}
```

Display each service fee's `paymentOrderNo`, `paymentProvider`, `expiresAt`, and `failureReason` when present. Keep mock-specific success action visible only if `paymentProvider === 'MOCK'` or provider text indicates mock mode.

- [ ] **Step 3: Update role application page**

In `RoleApplicationsView.vue`, after applying for a role with `depositStatus === 'PENDING'`, show a “支付保证金” button:

```ts
async function payDeposit(applicationId: number) {
  const result = await createRoleDepositPayment(applicationId)
  ElMessage.success(result.message || '保证金支付单已创建')
  if (result.payUrl) {
    window.open(result.payUrl, '_blank', 'noopener,noreferrer')
  }
  await loadApplications()
}
```

Add status text:

```ts
const depositStatusText: Record<string, string> = {
  PENDING: '待支付',
  PAID: '已支付',
  FAILED: '支付失败',
  EXPIRED: '已过期'
}
```

- [ ] **Step 4: Add small responsive styles**

In `frontend/src/styles.css`, add reusable payment metadata style:

```css
.payment-meta {
  display: grid;
  gap: 6px;
  color: var(--muted-text);
  font-size: 13px;
  word-break: break-word;
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
```

- [ ] **Step 5: Build frontend**

Use server-side Docker or approved environment. If running locally is approved:

```bash
npm --prefix frontend run build
```

Expected: PASS with known large chunk / dependency pure-comment warnings only.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/campushub.ts frontend/src/views/WalletView.vue frontend/src/views/RoleApplicationsView.vue frontend/src/styles.css
git commit -m "add payment order user flows"
```

## Task 8: Add admin payment monitor UI

**Files:**
- Create: `frontend/src/views/AdminPaymentView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Create admin payment view**

Create `frontend/src/views/AdminPaymentView.vue`:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminPaymentCallbackEvents, listAdminPaymentOrders, type PaymentCallbackEventSummary, type PaymentOrderSummary } from '../api/campushub'
import EmptyState from '../components/EmptyState.vue'
import PageActions from '../components/PageActions.vue'

const loading = ref(false)
const status = ref('')
const orders = ref<PaymentOrderSummary[]>([])
const events = ref<PaymentCallbackEventSummary[]>([])

async function loadPayments() {
  loading.value = true
  try {
    const [orderData, eventData] = await Promise.all([
      listAdminPaymentOrders(status.value || undefined),
      listAdminPaymentCallbackEvents()
    ])
    orders.value = orderData
    events.value = eventData
  } catch (error) {
    ElMessage.error('支付监控数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadPayments)
</script>

<template>
  <section class="page-section">
    <PageActions title="支付监控" description="查看服务费与身份保证金支付订单，不显示内部 token、签名 secret 或支付宝密钥。">
      <el-select v-model="status" clearable placeholder="全部状态" style="width: 180px" @change="loadPayments">
        <el-option label="待支付" value="PENDING" />
        <el-option label="已支付" value="PAID" />
        <el-option label="失败" value="FAILED" />
        <el-option label="已过期" value="EXPIRED" />
      </el-select>
      <el-button @click="loadPayments">刷新</el-button>
    </PageActions>

    <el-tabs>
      <el-tab-pane label="支付订单">
        <EmptyState v-if="!orders.length && !loading" title="暂无支付订单" description="创建服务费或身份保证金支付单后会出现在这里。" />
        <el-table v-else v-loading="loading" :data="orders" class="mobile-safe-table">
          <el-table-column prop="orderNo" label="本地订单号" min-width="180" />
          <el-table-column prop="businessType" label="业务类型" min-width="120" />
          <el-table-column prop="payerNickname" label="付款用户" min-width="120" />
          <el-table-column prop="amount" label="金额" min-width="90" />
          <el-table-column prop="provider" label="Provider" min-width="130" />
          <el-table-column prop="status" label="状态" min-width="100" />
          <el-table-column prop="expiresAt" label="过期时间" min-width="170" />
          <el-table-column prop="failureReason" label="失败原因" min-width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="回调事件">
        <EmptyState v-if="!events.length && !loading" title="暂无回调事件" description="支付中心回调会记录幂等事件和处理结果。" />
        <el-table v-else v-loading="loading" :data="events" class="mobile-safe-table">
          <el-table-column prop="eventId" label="事件 ID" min-width="170" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="status" label="状态" min-width="100" />
          <el-table-column prop="amount" label="金额" min-width="90" />
          <el-table-column prop="verified" label="已校验" min-width="90" />
          <el-table-column prop="handled" label="已处理" min-width="90" />
          <el-table-column prop="failureReason" label="失败原因" min-width="180" />
          <el-table-column prop="createdAt" label="时间" min-width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
```

- [ ] **Step 2: Add route**

In `frontend/src/router/index.ts`, add route:

```ts
{
  path: '/admin/payment',
  name: 'admin-payment',
  component: () => import('../views/AdminPaymentView.vue')
}
```

- [ ] **Step 3: Add navigation entry**

In `MainLayout.vue`, add admin nav item:

```ts
{ label: '支付监控', path: '/admin/payment', icon: 'Wallet' }
```

Use the existing navigation array pattern and imported Element Plus icon names.

- [ ] **Step 4: Add responsive monitor styles if needed**

If `mobile-safe-table` already exists, do not duplicate it. Otherwise add to `styles.css`:

```css
.mobile-safe-table {
  width: 100%;
  overflow-x: auto;
}
```

- [ ] **Step 5: Build frontend**

```bash
npm --prefix frontend run build
```

Expected: PASS with known warnings only.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/AdminPaymentView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add admin payment monitor UI"
```

## Task 9: Update docs and production configuration examples

**Files:**
- Modify: `.env.prod.example`
- Modify: `README.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update `.env.prod.example` with placeholders**

Add placeholder-only variables:

```dotenv
CAMPUSHUB_PAYMENT_PROVIDER=payment-center
CAMPUSHUB_PAYMENT_CENTER_BASE_URL=http://api-transfer-station-internal:8080
CAMPUSHUB_PAYMENT_CENTER_CREATE_PATH=/internal/payments/campushub/orders
CAMPUSHUB_PAYMENT_CENTER_CALLBACK_URL=https://ustc.suntomb.qzz.io/api/payment/callbacks/payment-center
CAMPUSHUB_PAYMENT_CENTER_CALLBACK_TOKEN=replace-with-internal-token
CAMPUSHUB_PAYMENT_CENTER_SIGNING_SECRET=replace-with-internal-signing-secret
CAMPUSHUB_PAYMENT_ORDER_EXPIRE_MINUTES=30
```

Do not add real secret values.

- [ ] **Step 2: Update README Phase 8 section**

Add section:

```markdown
## Phase 8：支付中心集成强化与服务费运营

Phase 8 将本地 mock 支付升级为可对接 API-Transfer-Station 支付中心的内部支付链路。CampusHub 保存本地支付订单、服务费/身份保证金状态和回调审计；API-Transfer-Station 负责真实支付宝应用配置、支付宝签名验签和外部渠道通知。

新增能力：

- `mock` 与 `payment-center` provider 由环境变量选择；
- 服务费和角色保证金创建统一支付订单；
- 支付中心内部回调支持 token/签名配置、幂等、金额、订单号和状态校验；
- 管理端新增支付订单和回调事件监控；
- `.env.prod.example` 只记录占位符，不包含真实 token、secret 或支付宝密钥。

Phase 8 仍不做交易本金托管、逐单保证金冻结、自动扣罚保证金、退款结算、发票、完整对账或 CampusHub 直连支付宝密钥。
```

- [ ] **Step 3: Update CLAUDE handoff**

Append to `CLAUDE.md` after Phase 7 handoff:

```markdown
## Latest Phase 8 deployment and Phase 9 handoff, 2026-05-23

Latest Phase 8 work adds payment-center integration hardening and service-fee operations. CampusHub now keeps mock mode while supporting a `payment-center` provider for API-Transfer-Station internal payment order creation and callbacks.

Implemented Phase 8:

- New docs: `docs/superpowers/specs/2026-05-23-campushub-phase8-payment-center-design.md` and `docs/superpowers/plans/2026-05-23-campushub-phase8-payment-center-upgrade.md`.
- `V11__payment_center_integration.sql` adds payment orders, callback events, and payment mapping fields.
- Backend payment flow supports service-fee and role-deposit payment order creation, callback idempotency, amount/order/status validation, and admin payment monitor APIs.
- Frontend wallet, role applications, and admin payment monitor display payment order status.
- `.env.prod.example` documents payment-center placeholder variables only.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V11; add V12+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.
```

After actual verification, replace the generic verification wording with concrete commit/build/API/Playwriter results before final commit.

- [ ] **Step 4: Commit docs**

```bash
git add .env.prod.example README.md CLAUDE.md docs/superpowers/specs/2026-05-23-campushub-phase8-payment-center-design.md docs/superpowers/plans/2026-05-23-campushub-phase8-payment-center-upgrade.md
git commit -m "document phase 8 payment center integration"
```

## Task 10: Verification and deployment checkpoint

**Files:**
- Modify: `CLAUDE.md` only if verification results differ from the draft handoff.

- [ ] **Step 1: Check git status**

```bash
git status --short
```

Expected: only intentional uncommitted docs/handoff changes, or clean if all prior commits succeeded.

- [ ] **Step 2: Backend verification**

Preferred local command if Maven is available:

```bash
mvn -f backend/pom.xml test
```

If local Maven is unavailable, push the branch and run a low-impact server Docker backend build instead:

```bash
docker compose -f docker-compose.prod.yml build campushub-backend
```

Expected: Maven package completes with `BUILD SUCCESS` inside Docker. Do not print `.env` or any secret values.

- [ ] **Step 3: Frontend verification**

Preferred server/Docker verification:

```bash
docker compose -f docker-compose.prod.yml build campushub-web
```

Expected: Vite build succeeds. Known large chunk and dependency pure-comment warnings are acceptable.

- [ ] **Step 4: API smoke after deploy**

After an approved low-impact deploy, run server-local non-secret checks:

```bash
curl -sS http://127.0.0.1:18080/api/payment/service-fees
curl -sS http://127.0.0.1:18080/api/admin/payment/orders
curl -sS http://127.0.0.1:18080/api/admin/payment/callback-events
curl -sS http://127.0.0.1:18080/api/goods
curl -sS http://127.0.0.1:18080/api/tasks
curl -sS http://127.0.0.1:18080/api/shops
curl -sS http://127.0.0.1:18080/api/project-ads
```

Expected: HTTP 200 JSON responses for public/admin prototype endpoints, no HTTP 500. Do not include token/secret values in commands.

- [ ] **Step 5: Browser verification**

Use Playwriter against `https://ustc.suntomb.qzz.io`:

- `/wallet` shows service-fee payment state without white screen.
- `/roles` shows role deposit payment status/action without white screen.
- `/admin/payment` shows payment orders and callback event tabs without white screen.
- `/admin/ops`, `/admin/governance`, `/goods`, `/tasks`, `/shops`, and `/project-ads` still render.
- Mobile viewport 390x844 on `/admin/payment` has no document-level horizontal overflow.

- [ ] **Step 6: Update CLAUDE handoff with actual verification**

Edit `CLAUDE.md` Phase 8 section to include exact commit hash, server build result, smoke endpoints, and Playwriter routes. Keep all secret-handling constraints.

- [ ] **Step 7: Final commit if handoff changed**

```bash
git add CLAUDE.md
git commit -m "document phase 8 deployment handoff"
```

## Self-review

- Spec coverage: Tasks cover provider selection, payment-center contract, V11 schema, service-fee payments, role-deposit payments, callback idempotency, admin payment monitor APIs/UI, environment docs, README/CLAUDE handoff, server Docker verification, API smoke, and Playwriter checks.
- Scope: The plan stays within one focused payment subsystem and does not include escrow, per-order deposit freezes, refunds, settlement, direct Alipay SDK/key handling, or auth/RBAC hardening.
- Placeholder scan: No TBD/TODO placeholders are used. Deferred implementation choices are explicit non-goals or conditional verification paths.
- Type consistency: `PaymentOrder`, `PaymentCreation`, `PaymentStatus`, `PaymentCenterCallbackRequest`, `PaymentOrderSummary`, and API route names are used consistently across backend, frontend, and tests.
- Safety: `.env.prod.example` uses placeholders only; verification commands avoid reading or printing secrets; production build guidance is low-impact.
