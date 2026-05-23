# CampusHub Phase 9 Wallet Escrow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build CampusHub Phase 9 wallet ledger, recharge, withdrawal, frozen-balance escrow, and offline/online service-fee rules while preserving the API-Transfer-Station payment boundary.

**Architecture:** Add V12 wallet/escrow schema and centralize all balance mutations in `WalletService` with idempotent flows. Extend Phase 8 payment orders with `WALLET_RECHARGE`, add recharge/withdrawal operations, then attach online escrow first to second-hand `GoodsOrder` while keeping runner/shop escrow as future work. Frontend adds a user wallet center and admin wallet operations workspace, reusing the existing Vue/Element Plus patterns and mobile-safe UX primitives.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Data JPA, Flyway, MySQL 8, BigDecimal money arithmetic, Vue 3, Vite, TypeScript, Element Plus, Docker Compose, Playwriter browser verification.

---

## Scope and boundaries

This plan implements `docs/superpowers/specs/2026-05-23-campushub-phase9-wallet-escrow-design.md`.

In scope:

- V12 schema for wallet ledger, recharge orders, withdrawal requests, frozen records, goods escrow fields, and optional fee policy snapshots.
- Central `WalletService` for credit, debit, freeze, unfreeze, frozen transfer, and service-fee deduction.
- Recharge orders: Alipay uses Phase 8 payment order callback; WeChat uses admin manual approval.
- Withdrawal requests: freeze on submit, reject/cancel unfreezes, complete deducts frozen balance.
- Goods online escrow: freeze buyer balance, release to seller, cancel/unfreeze, dispute visibility.
- Fee policy service: Alipay recharge 0.6%, offline trade 1% capped at 2 CNY for amount >= 50, online escrow 1% capped at 3 CNY.
- User wallet UI and admin wallet operations UI.
- README/CLAUDE handoff and server-side Docker/API/Playwriter verification.

Out of scope:

- CampusHub direct Alipay/WeChat SDK integration or key handling.
- API-Transfer-Station internal implementation.
- Automatic withdrawal payout channel.
- Full accounting, settlement, invoices, tax reports, or reconciliation system.
- Full escrow integration for runner tasks and shop service orders.
- Full JWT/RBAC hardening.

## File structure map

### Backend migration

- Create `backend/src/main/resources/db/migration/V12__wallet_escrow_upgrade.sql` — wallet ledger/recharge/withdrawal/frozen record schema and goods escrow columns.

### Backend wallet package

- Modify `backend/src/main/java/com/campushub/wallet/WalletAccount.java` — add mutation methods and `updatedAt` mapping.
- Modify `backend/src/main/java/com/campushub/wallet/WalletAccountRepository.java` — add pessimistic lock lookup.
- Modify `backend/src/main/java/com/campushub/wallet/WalletFlow.java` — add ledger fields and constructors.
- Modify `backend/src/main/java/com/campushub/wallet/WalletFlowRepository.java` — idempotency lookup and filtered list.
- Modify `backend/src/main/java/com/campushub/wallet/WalletAccountSummary.java` — include updated time.
- Modify `backend/src/main/java/com/campushub/wallet/WalletFlowSummary.java` — expose new ledger fields.
- Create `backend/src/main/java/com/campushub/wallet/WalletService.java` — all wallet mutations.
- Create `backend/src/main/java/com/campushub/wallet/FeePolicyService.java` — fee calculation.
- Create `backend/src/main/java/com/campushub/wallet/WalletRechargeOrder.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletRechargeOrderRepository.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletRechargeRequest.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletRechargeSummary.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequest.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequestRepository.java`.
- Create `backend/src/main/java/com/campushub/wallet/CreateWithdrawalRequest.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletWithdrawalSummary.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletFrozenRecord.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletFrozenRecordRepository.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletFrozenRecordSummary.java`.
- Create `backend/src/main/java/com/campushub/wallet/WalletOperationService.java` — recharge/withdrawal user/admin orchestration.
- Modify `backend/src/main/java/com/campushub/wallet/WalletController.java` — user wallet APIs.
- Create `backend/src/main/java/com/campushub/wallet/AdminWalletController.java` — admin wallet operations APIs.

### Backend payment package

- Modify `backend/src/main/java/com/campushub/payment/PaymentService.java` — support `WALLET_RECHARGE` callback and delegate balance mutations to `WalletService`.
- Modify `backend/src/main/java/com/campushub/payment/PaymentOrderSummary.java` only if admin/user needs recharge metadata.

### Backend goods package

- Modify `backend/src/main/java/com/campushub/goods/GoodsOrder.java` — add trade mode and escrow fields.
- Modify `backend/src/main/java/com/campushub/goods/GoodsOrderRepository.java` — list escrow orders.
- Modify `backend/src/main/java/com/campushub/goods/GoodsOrderSummary.java` — expose escrow state.
- Modify `backend/src/main/java/com/campushub/goods/GoodsService.java` — create escrow order, freeze, release, cancel, dispute.
- Modify `backend/src/main/java/com/campushub/goods/GoodsController.java` — escrow endpoints.
- Test `backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java`.
- Test `backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java`.
- Test `backend/src/test/java/com/campushub/goods/GoodsEscrowIntegrationTest.java`.

### Frontend

- Modify `frontend/src/api/campushub.ts` — wallet recharge/withdrawal/frozen/escrow/admin APIs and types.
- Modify `frontend/src/views/WalletView.vue` — wallet center tabs/cards.
- Create `frontend/src/views/AdminWalletView.vue` — admin wallet operations workspace.
- Modify `frontend/src/views/GoodsDetailView.vue` — trade mode and escrow action/status UI.
- Modify `frontend/src/router/index.ts` — add `/admin/wallet` route.
- Modify `frontend/src/layouts/MainLayout.vue` — add admin wallet navigation.
- Modify `frontend/src/styles.css` — wallet/escrow responsive styles.

### Docs/config

- Modify `README.md` — document Phase 9 behavior and payment boundary.
- Modify `CLAUDE.md` — add Phase 9 handoff after verification.

---

## Task 1: Add V12 wallet escrow schema and entity mappings

**Files:**
- Create: `backend/src/main/resources/db/migration/V12__wallet_escrow_upgrade.sql`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletAccount.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletFlow.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletAccountRepository.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletFlowRepository.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsOrder.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsOrderRepository.java`
- Test: `backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java`

- [ ] **Step 1: Write failing schema/entity test**

Create `backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java`:

```java
package com.campushub.wallet;

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
class WalletServiceIntegrationTest {

    @Autowired WalletAccountRepository walletAccountRepository;
    @Autowired WalletFlowRepository walletFlowRepository;
    @Autowired UserRepository userRepository;

    @Test
    void walletAccountAndFlowExposePhase9LedgerFields() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        WalletAccount account = walletAccountRepository.findByUserId(user.getId()).orElseThrow();

        account.credit(new BigDecimal("20.00"));
        account.freeze(new BigDecimal("5.00"));
        WalletAccount savedAccount = walletAccountRepository.saveAndFlush(account);

        WalletFlow flow = new WalletFlow(
                savedAccount,
                user,
                "WF-PHASE9-001",
                "OUT",
                "FREEZE",
                new BigDecimal("5.00"),
                savedAccount.getBalance(),
                savedAccount.getFrozenBalance(),
                "GOODS_ESCROW",
                1L,
                "phase9-schema-test",
                null,
                "SYSTEM",
                null,
                "冻结二手交易托管金额");
        WalletFlow savedFlow = walletFlowRepository.saveAndFlush(flow);

        assertThat(savedAccount.getFrozenBalance()).isEqualByComparingTo("5.00");
        assertThat(savedAccount.getUpdatedAt()).isNotNull();
        assertThat(savedFlow.getFlowType()).isEqualTo("FREEZE");
        assertThat(savedFlow.getAvailableBalanceAfter()).isEqualByComparingTo("15.00");
        assertThat(savedFlow.getFrozenBalanceAfter()).isEqualByComparingTo("5.00");
        assertThat(savedFlow.getIdempotencyKey()).isEqualTo("phase9-schema-test");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run when Maven is available, otherwise defer to server Docker verification:

```bash
mvn -f backend/pom.xml -Dtest=WalletServiceIntegrationTest#walletAccountAndFlowExposePhase9LedgerFields test
```

Expected: FAIL because V12 migration and new entity fields/methods do not exist.

- [ ] **Step 3: Create V12 migration**

Create `backend/src/main/resources/db/migration/V12__wallet_escrow_upgrade.sql`:

```sql
ALTER TABLE wallet_accounts
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER status;

ALTER TABLE wallet_flows
    ADD COLUMN flow_type VARCHAR(40) NOT NULL DEFAULT 'LEGACY' AFTER direction,
    ADD COLUMN available_balance_after DECIMAL(10,2) NULL AFTER balance_after,
    ADD COLUMN frozen_balance_after DECIMAL(10,2) NULL AFTER available_balance_after,
    ADD COLUMN idempotency_key VARCHAR(120) NULL AFTER business_id,
    ADD COLUMN counterparty_user_id BIGINT NULL AFTER idempotency_key,
    ADD COLUMN created_by VARCHAR(40) NOT NULL DEFAULT 'SYSTEM' AFTER counterparty_user_id,
    ADD COLUMN operator_id BIGINT NULL AFTER created_by,
    ADD UNIQUE KEY uk_wallet_flow_idempotency (idempotency_key),
    ADD INDEX idx_wallet_flow_type_time (flow_type, created_at),
    ADD INDEX idx_wallet_flow_business (business_type, business_id),
    ADD CONSTRAINT fk_wallet_flow_counterparty FOREIGN KEY (counterparty_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_wallet_flow_operator FOREIGN KEY (operator_id) REFERENCES users(id);

CREATE TABLE wallet_recharge_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recharge_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    channel_fee DECIMAL(10,2) NOT NULL,
    pay_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_order_no VARCHAR(64) NULL,
    review_note VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_recharge_no UNIQUE (recharge_no),
    CONSTRAINT fk_wallet_recharge_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wallet_recharge_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_wallet_recharge_user_time (user_id, created_at),
    INDEX idx_wallet_recharge_status_time (status, created_at)
);

CREATE TABLE wallet_withdrawal_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    withdrawal_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    account_snapshot VARCHAR(200) NULL,
    status VARCHAR(30) NOT NULL,
    review_note VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_withdrawal_no UNIQUE (withdrawal_no),
    CONSTRAINT fk_wallet_withdrawal_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wallet_withdrawal_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_wallet_withdrawal_user_time (user_id, created_at),
    INDEX idx_wallet_withdrawal_status_time (status, created_at)
);

CREATE TABLE wallet_frozen_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    freeze_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    business_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    frozen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at DATETIME NULL,
    remark VARCHAR(500) NULL,
    CONSTRAINT uk_wallet_freeze_no UNIQUE (freeze_no),
    CONSTRAINT fk_wallet_freeze_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_wallet_freeze_user_status (user_id, status),
    INDEX idx_wallet_freeze_business (business_type, business_id)
);

ALTER TABLE goods_orders
    ADD COLUMN trade_mode VARCHAR(30) NOT NULL DEFAULT 'OFFLINE' AFTER status,
    ADD COLUMN escrow_status VARCHAR(30) NOT NULL DEFAULT 'NONE' AFTER trade_mode,
    ADD COLUMN buyer_id BIGINT NULL AFTER escrow_status,
    ADD COLUMN seller_id BIGINT NULL AFTER buyer_id,
    ADD COLUMN escrow_amount DECIMAL(10,2) NULL AFTER seller_id,
    ADD COLUMN platform_service_fee DECIMAL(10,2) NULL AFTER escrow_amount,
    ADD COLUMN escrow_frozen_at DATETIME NULL AFTER platform_service_fee,
    ADD COLUMN escrow_released_at DATETIME NULL AFTER escrow_frozen_at,
    ADD COLUMN escrow_unfrozen_at DATETIME NULL AFTER escrow_released_at,
    ADD COLUMN cancel_reason VARCHAR(500) NULL AFTER escrow_unfrozen_at,
    ADD COLUMN dispute_reason VARCHAR(500) NULL AFTER cancel_reason,
    ADD CONSTRAINT fk_goods_order_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    ADD CONSTRAINT fk_goods_order_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    ADD INDEX idx_goods_order_escrow_status (escrow_status, created_at);
```

- [ ] **Step 4: Extend `WalletAccount`**

Modify `backend/src/main/java/com/campushub/wallet/WalletAccount.java`:

```java
package com.campushub.wallet;

import com.campushub.common.BusinessException;
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
@Table(name = "wallet_accounts")
public class WalletAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "frozen_balance", nullable = false)
    private BigDecimal frozenBalance;

    @Column(nullable = false)
    private String status;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected WalletAccount() {
    }

    public WalletAccount(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getFrozenBalance() { return frozenBalance; }
    public String getStatus() { return status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void credit(BigDecimal amount) {
        ensureActive();
        ensurePositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        ensureActive();
        ensurePositive(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException("钱包可用余额不足");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void freeze(BigDecimal amount) {
        debit(amount);
        this.frozenBalance = this.frozenBalance.add(amount);
    }

    public void unfreeze(BigDecimal amount) {
        ensureActive();
        ensurePositive(amount);
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new BusinessException("钱包冻结余额不足");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.balance = this.balance.add(amount);
    }

    public void debitFrozen(BigDecimal amount) {
        ensureActive();
        ensurePositive(amount);
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new BusinessException("钱包冻结余额不足");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
    }

    private void ensureActive() {
        if (!"ACTIVE".equals(status)) {
            throw new BusinessException("钱包账户不可用");
        }
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于 0");
        }
    }
}
```

- [ ] **Step 5: Extend `WalletFlow`**

Modify `backend/src/main/java/com/campushub/wallet/WalletFlow.java` to include the new constructor and getters:

```java
@Column(name = "flow_type", nullable = false)
private String flowType;

@Column(name = "available_balance_after")
private BigDecimal availableBalanceAfter;

@Column(name = "frozen_balance_after")
private BigDecimal frozenBalanceAfter;

@Column(name = "idempotency_key")
private String idempotencyKey;

@Column(name = "counterparty_user_id")
private Long counterpartyUserId;

@Column(name = "created_by", nullable = false)
private String createdBy;

@Column(name = "operator_id")
private Long operatorId;

public WalletFlow(
        WalletAccount walletAccount,
        User user,
        String flowNo,
        String direction,
        String flowType,
        BigDecimal amount,
        BigDecimal availableBalanceAfter,
        BigDecimal frozenBalanceAfter,
        String businessType,
        Long businessId,
        String idempotencyKey,
        Long counterpartyUserId,
        String createdBy,
        Long operatorId,
        String remark) {
    this.walletAccount = walletAccount;
    this.user = user;
    this.flowNo = flowNo;
    this.direction = direction;
    this.flowType = flowType;
    this.amount = amount;
    this.balanceAfter = availableBalanceAfter;
    this.availableBalanceAfter = availableBalanceAfter;
    this.frozenBalanceAfter = frozenBalanceAfter;
    this.businessType = businessType;
    this.businessId = businessId;
    this.idempotencyKey = idempotencyKey;
    this.counterpartyUserId = counterpartyUserId;
    this.createdBy = createdBy;
    this.operatorId = operatorId;
    this.remark = remark;
}

public String getFlowType() { return flowType; }
public BigDecimal getAvailableBalanceAfter() { return availableBalanceAfter; }
public BigDecimal getFrozenBalanceAfter() { return frozenBalanceAfter; }
public String getIdempotencyKey() { return idempotencyKey; }
public Long getCounterpartyUserId() { return counterpartyUserId; }
public String getCreatedBy() { return createdBy; }
public Long getOperatorId() { return operatorId; }
```

Keep the existing constructor for Phase 1-8 compatibility, but set `flowType = "LEGACY"`, `availableBalanceAfter = balanceAfter`, `frozenBalanceAfter = walletAccount.getFrozenBalance()`, `createdBy = "SYSTEM"` inside it.

- [ ] **Step 6: Add repository helpers**

Modify `WalletAccountRepository.java`:

```java
package com.campushub.wallet;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
    Optional<WalletAccount> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletAccount w where w.user.id = :userId")
    Optional<WalletAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
```

Modify `WalletFlowRepository.java`:

```java
package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFlowRepository extends JpaRepository<WalletFlow, Long> {
    List<WalletFlow> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<WalletFlow> findByIdempotencyKey(String idempotencyKey);
    List<WalletFlow> findByBusinessTypeAndBusinessIdOrderByCreatedAtDesc(String businessType, Long businessId);
    List<WalletFlow> findTop300ByOrderByCreatedAtDesc();
}
```

- [ ] **Step 7: Add goods order escrow mappings**

Modify `GoodsOrder.java` with fields and methods:

```java
@Column(name = "trade_mode", nullable = false)
private String tradeMode = "OFFLINE";

@Column(name = "escrow_status", nullable = false)
private String escrowStatus = "NONE";

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "buyer_id")
private User buyer;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "seller_id")
private User seller;

@Column(name = "escrow_amount")
private BigDecimal escrowAmount;

@Column(name = "platform_service_fee")
private BigDecimal platformServiceFee;

@Column(name = "escrow_frozen_at")
private LocalDateTime escrowFrozenAt;

@Column(name = "escrow_released_at")
private LocalDateTime escrowReleasedAt;

@Column(name = "escrow_unfrozen_at")
private LocalDateTime escrowUnfrozenAt;

@Column(name = "cancel_reason")
private String cancelReason;

@Column(name = "dispute_reason")
private String disputeReason;

public String getTradeMode() { return tradeMode; }
public String getEscrowStatus() { return escrowStatus; }
public User getBuyer() { return buyer; }
public User getSeller() { return seller; }
public BigDecimal getEscrowAmount() { return escrowAmount; }
public BigDecimal getPlatformServiceFee() { return platformServiceFee; }
public LocalDateTime getEscrowFrozenAt() { return escrowFrozenAt; }
public LocalDateTime getEscrowReleasedAt() { return escrowReleasedAt; }
public LocalDateTime getEscrowUnfrozenAt() { return escrowUnfrozenAt; }
public String getCancelReason() { return cancelReason; }
public String getDisputeReason() { return disputeReason; }

public void enableOnlineEscrow(User buyer, User seller, BigDecimal escrowAmount, BigDecimal platformServiceFee) {
    this.tradeMode = "ONLINE_ESCROW";
    this.escrowStatus = "PENDING_FREEZE";
    this.buyer = buyer;
    this.seller = seller;
    this.escrowAmount = escrowAmount;
    this.platformServiceFee = platformServiceFee;
}

public void markEscrowFrozen(LocalDateTime time) {
    this.escrowStatus = "FROZEN";
    this.escrowFrozenAt = time;
    this.paidAt = time;
}

public void markEscrowReleased(LocalDateTime time) {
    this.escrowStatus = "RELEASED";
    this.escrowReleasedAt = time;
    this.completedAt = time;
    this.status = "COMPLETED";
}

public void markEscrowCanceled(LocalDateTime time, String reason) {
    this.escrowStatus = "CANCELED";
    this.escrowUnfrozenAt = time;
    this.canceledAt = time;
    this.cancelReason = reason;
    this.status = "CANCELED";
}

public void markEscrowDisputed(String reason) {
    this.escrowStatus = "DISPUTED";
    this.disputeReason = reason;
}
```

Modify `GoodsOrderRepository.java`:

```java
List<GoodsOrder> findTop200ByTradeModeOrderByCreatedAtDesc(String tradeMode);
List<GoodsOrder> findTop200ByEscrowStatusOrderByCreatedAtDesc(String escrowStatus);
```

- [ ] **Step 8: Run schema/entity test**

```bash
mvn -f backend/pom.xml -Dtest=WalletServiceIntegrationTest#walletAccountAndFlowExposePhase9LedgerFields test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/migration/V12__wallet_escrow_upgrade.sql backend/src/main/java/com/campushub/wallet/WalletAccount.java backend/src/main/java/com/campushub/wallet/WalletFlow.java backend/src/main/java/com/campushub/wallet/WalletAccountRepository.java backend/src/main/java/com/campushub/wallet/WalletFlowRepository.java backend/src/main/java/com/campushub/goods/GoodsOrder.java backend/src/main/java/com/campushub/goods/GoodsOrderRepository.java backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java
git commit -m "add wallet escrow schema foundation"
```

## Task 2: Implement WalletService and fee policy

**Files:**
- Create: `backend/src/main/java/com/campushub/wallet/WalletService.java`
- Create: `backend/src/main/java/com/campushub/wallet/FeePolicyService.java`
- Modify: `backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java`

- [ ] **Step 1: Add failing WalletService and fee tests**

Append to `WalletServiceIntegrationTest.java`:

```java
@Autowired WalletService walletService;
@Autowired FeePolicyService feePolicyService;

@Test
void walletServiceCreditsFreezesUnfreezesAndTransfersFrozenFundsIdempotently() {
    User buyer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    User seller = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();

    walletService.credit(buyer.getId(), new BigDecimal("100.00"), "TEST", 1L, "credit-1", "SYSTEM", null, "测试入账");
    walletService.credit(buyer.getId(), new BigDecimal("100.00"), "TEST", 1L, "credit-1", "SYSTEM", null, "测试入账");
    walletService.freeze(buyer.getId(), new BigDecimal("30.00"), "GOODS_ESCROW", 2L, "freeze-1", "SYSTEM", null, "冻结托管");
    walletService.unfreeze(buyer.getId(), new BigDecimal("10.00"), "GOODS_ESCROW", 2L, "unfreeze-1", "SYSTEM", null, "部分解冻");
    walletService.transferFrozen(buyer.getId(), seller.getId(), new BigDecimal("20.00"), "GOODS_ESCROW", 2L, "release-1", "SYSTEM", null, "托管划转");

    WalletAccount buyerWallet = walletAccountRepository.findByUserId(buyer.getId()).orElseThrow();
    WalletAccount sellerWallet = walletAccountRepository.findByUserId(seller.getId()).orElseThrow();

    assertThat(buyerWallet.getBalance()).isEqualByComparingTo("80.00");
    assertThat(buyerWallet.getFrozenBalance()).isEqualByComparingTo("0.00");
    assertThat(sellerWallet.getBalance()).isEqualByComparingTo("20.00");
    assertThat(walletFlowRepository.findByIdempotencyKey("credit-1")).isPresent();
}

@Test
void feePolicyCalculatesRechargeOfflineAndOnlineFees() {
    assertThat(feePolicyService.calculateAlipayRechargeFee(new BigDecimal("100.00"))).isEqualByComparingTo("0.60");
    assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("49.99"))).isEqualByComparingTo("0.00");
    assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("80.00"))).isEqualByComparingTo("0.80");
    assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("500.00"))).isEqualByComparingTo("2.00");
    assertThat(feePolicyService.calculateOnlineEscrowFee(new BigDecimal("80.00"))).isEqualByComparingTo("0.80");
    assertThat(feePolicyService.calculateOnlineEscrowFee(new BigDecimal("500.00"))).isEqualByComparingTo("3.00");
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn -f backend/pom.xml -Dtest=WalletServiceIntegrationTest#walletServiceCreditsFreezesUnfreezesAndTransfersFrozenFundsIdempotently,WalletServiceIntegrationTest#feePolicyCalculatesRechargeOfflineAndOnlineFees test
```

Expected: FAIL because `WalletService` and `FeePolicyService` do not exist.

- [ ] **Step 3: Create `FeePolicyService`**

Create `backend/src/main/java/com/campushub/wallet/FeePolicyService.java`:

```java
package com.campushub.wallet;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FeePolicyService {

    private static final BigDecimal ALIPAY_RECHARGE_RATE = new BigDecimal("0.006");
    private static final BigDecimal TRADE_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal OFFLINE_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal OFFLINE_CAP = new BigDecimal("2.00");
    private static final BigDecimal ONLINE_CAP = new BigDecimal("3.00");

    public BigDecimal calculateAlipayRechargeFee(BigDecimal amount) {
        ensurePositive(amount);
        return amount.multiply(ALIPAY_RECHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOfflineTradeFee(BigDecimal amount) {
        ensurePositive(amount);
        if (amount.compareTo(OFFLINE_THRESHOLD) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(TRADE_FEE_RATE).min(OFFLINE_CAP).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOnlineEscrowFee(BigDecimal amount) {
        ensurePositive(amount);
        return amount.multiply(TRADE_FEE_RATE).min(ONLINE_CAP).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于 0");
        }
    }
}
```

- [ ] **Step 4: Create `WalletService`**

Create `backend/src/main/java/com/campushub/wallet/WalletService.java`:

```java
package com.campushub.wallet;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;

    public WalletService(WalletAccountRepository walletAccountRepository, WalletFlowRepository walletFlowRepository) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
    }

    @Transactional
    public WalletAccount credit(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.credit(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "IN", "RECHARGE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount debit(Long userId, BigDecimal amount, String flowType, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.debit(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "OUT", flowType, amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount freeze(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.freeze(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "OUT", "FREEZE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount unfreeze(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.unfreeze(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "IN", "UNFREEZE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public void transferFrozen(Long fromUserId, Long toUserId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey + ":OUT").isPresent()) {
            return;
        }
        WalletAccount from = findAccountForUpdate(fromUserId);
        WalletAccount to = findAccountForUpdate(toUserId);
        from.debitFrozen(amount);
        to.credit(amount);
        walletAccountRepository.save(from);
        walletAccountRepository.save(to);
        saveFlow(from, "OUT", "ESCROW_TRANSFER_OUT", amount, businessType, businessId, idempotencyKey + ":OUT", toUserId, createdBy, operatorId, remark);
        saveFlow(to, "IN", "ESCROW_TRANSFER_IN", amount, businessType, businessId, idempotencyKey + ":IN", fromUserId, createdBy, operatorId, remark);
    }

    private WalletAccount findAccount(Long userId) {
        return walletAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("钱包账户不存在"));
    }

    private WalletAccount findAccountForUpdate(Long userId) {
        return walletAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("钱包账户不存在"));
    }

    private void saveFlow(WalletAccount account, String direction, String flowType, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, Long counterpartyUserId, String createdBy, Long operatorId, String remark) {
        walletFlowRepository.save(new WalletFlow(
                account,
                account.getUser(),
                "WF-" + System.currentTimeMillis() + "-" + account.getUser().getId(),
                direction,
                flowType,
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                counterpartyUserId,
                createdBy,
                operatorId,
                remark));
    }
}
```

- [ ] **Step 5: Run WalletService tests**

```bash
mvn -f backend/pom.xml -Dtest=WalletServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/campushub/wallet/WalletService.java backend/src/main/java/com/campushub/wallet/FeePolicyService.java backend/src/test/java/com/campushub/wallet/WalletServiceIntegrationTest.java
git commit -m "add wallet ledger service"
```

## Task 3: Add recharge orders and payment callback入账

**Files:**
- Create: `backend/src/main/java/com/campushub/wallet/WalletRechargeOrder.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletRechargeOrderRepository.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletRechargeRequest.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletRechargeSummary.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`
- Modify: `backend/src/main/java/com/campushub/payment/PaymentService.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletController.java`
- Test: `backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java`

- [ ] **Step 1: Write failing recharge tests**

Create `backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java`:

```java
package com.campushub.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.payment.PaymentCallbackHeaders;
import com.campushub.payment.PaymentCenterCallbackRequest;
import com.campushub.payment.PaymentService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class WalletOperationIntegrationTest {

    @Autowired WalletOperationService walletOperationService;
    @Autowired WalletRechargeOrderRepository rechargeOrderRepository;
    @Autowired WalletAccountRepository walletAccountRepository;
    @Autowired UserRepository userRepository;
    @Autowired PaymentService paymentService;

    @Test
    void alipayRechargeCreatesPaymentOrderAndCreditsBalanceAfterCallback() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        WalletRechargeSummary summary = walletOperationService.createRecharge(user.getId(), new WalletRechargeRequest("ALIPAY", new BigDecimal("100.00"), "支付宝充值"));

        assertThat(summary.channelFee()).isEqualByComparingTo("0.60");
        assertThat(summary.payAmount()).isEqualByComparingTo("100.60");
        assertThat(summary.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(summary.paymentOrderNo()).isNotBlank();

        paymentService.handlePaymentCenterCallback(new PaymentCenterCallbackRequest(
                "evt-recharge-phase9-001",
                summary.paymentOrderNo(),
                "MOCK-" + summary.paymentOrderNo(),
                "WALLET_RECHARGE",
                summary.id(),
                new BigDecimal("100.60"),
                "PAID",
                LocalDateTime.now(),
                null), new PaymentCallbackHeaders("internal-token", null, null), "internal-token");

        WalletAccount wallet = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
        WalletRechargeOrder order = rechargeOrderRepository.findById(summary.id()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        assertThat(order.getStatus()).isEqualTo("PAID");
    }

    @Test
    void wechatRechargeWaitsForAdminApprovalAndThenCreditsBalance() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("admin@mail.ustc.edu.cn").orElseThrow();
        WalletRechargeSummary summary = walletOperationService.createRecharge(user.getId(), new WalletRechargeRequest("WECHAT", new BigDecimal("30.00"), "微信人工充值"));

        assertThat(summary.status()).isEqualTo("PENDING_REVIEW");
        walletOperationService.approveWechatRecharge(summary.id(), admin.getId(), "已确认微信收款");

        WalletAccount wallet = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
        WalletRechargeOrder order = rechargeOrderRepository.findById(summary.id()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("30.00");
        assertThat(order.getStatus()).isEqualTo("PAID");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest test
```

Expected: FAIL because recharge entities/services do not exist.

- [ ] **Step 3: Create recharge entity and repository**

Create `WalletRechargeOrder.java`:

```java
package com.campushub.wallet;

import com.campushub.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_recharge_orders")
public class WalletRechargeOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recharge_no", nullable = false)
    private String rechargeNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "channel_fee", nullable = false)
    private BigDecimal channelFee;
    @Column(name = "pay_amount", nullable = false)
    private BigDecimal payAmount;
    @Column(nullable = false)
    private String status;
    @Column(name = "payment_order_no")
    private String paymentOrderNo;
    @Column(name = "review_note")
    private String reviewNote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id")
    private User reviewer;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected WalletRechargeOrder() {}

    public WalletRechargeOrder(String rechargeNo, User user, String channel, BigDecimal amount, BigDecimal channelFee, BigDecimal payAmount, String status) {
        this.rechargeNo = rechargeNo;
        this.user = user;
        this.channel = channel;
        this.amount = amount;
        this.channelFee = channelFee;
        this.payAmount = payAmount;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getRechargeNo() { return rechargeNo; }
    public User getUser() { return user; }
    public String getChannel() { return channel; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getChannelFee() { return channelFee; }
    public BigDecimal getPayAmount() { return payAmount; }
    public String getStatus() { return status; }
    public String getPaymentOrderNo() { return paymentOrderNo; }
    public String getReviewNote() { return reviewNote; }
    public User getReviewer() { return reviewer; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void attachPaymentOrder(String paymentOrderNo) { this.paymentOrderNo = paymentOrderNo; }
    public void markPaid() { this.status = "PAID"; }
    public void approve(User reviewer, String note) { this.status = "PAID"; this.reviewer = reviewer; this.reviewNote = note; this.reviewedAt = LocalDateTime.now(); }
    public void reject(User reviewer, String note) { this.status = "REJECTED"; this.reviewer = reviewer; this.reviewNote = note; this.reviewedAt = LocalDateTime.now(); }
}
```

Create `WalletRechargeOrderRepository.java`:

```java
package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRechargeOrderRepository extends JpaRepository<WalletRechargeOrder, Long> {
    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<WalletRechargeOrder> findByRechargeNo(String rechargeNo);
    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<WalletRechargeOrder> findByPaymentOrderNo(String paymentOrderNo);
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findTop200ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findTop200ByStatusOrderByCreatedAtDesc(String status);
}
```

- [ ] **Step 4: Create request and summary records**

Create `WalletRechargeRequest.java`:

```java
package com.campushub.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record WalletRechargeRequest(
        @NotBlank String channel,
        @DecimalMin("0.01") BigDecimal amount,
        String remark) {
}
```

Create `WalletRechargeSummary.java`:

```java
package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletRechargeSummary(
        Long id,
        String rechargeNo,
        Long userId,
        String userNickname,
        String channel,
        BigDecimal amount,
        BigDecimal channelFee,
        BigDecimal payAmount,
        String status,
        String paymentOrderNo,
        String reviewNote,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {
    public static WalletRechargeSummary from(WalletRechargeOrder order) {
        return new WalletRechargeSummary(
                order.getId(), order.getRechargeNo(), order.getUser().getId(), order.getUser().getNickname(),
                order.getChannel(), order.getAmount(), order.getChannelFee(), order.getPayAmount(), order.getStatus(),
                order.getPaymentOrderNo(), order.getReviewNote(), order.getReviewedAt(), order.getCreatedAt());
    }
}
```

- [ ] **Step 5: Create recharge orchestration in `WalletOperationService`**

Create `backend/src/main/java/com/campushub/wallet/WalletOperationService.java` with recharge methods:

```java
package com.campushub.wallet;

import com.campushub.common.BusinessException;
import com.campushub.payment.PaymentCreation;
import com.campushub.payment.PaymentRequest;
import com.campushub.payment.PaymentProvider;
import com.campushub.payment.PaymentOrder;
import com.campushub.payment.PaymentOrderRepository;
import com.campushub.payment.PaymentCenterProperties;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOperationService {
    private final WalletRechargeOrderRepository rechargeOrderRepository;
    private final UserRepository userRepository;
    private final FeePolicyService feePolicyService;
    private final PaymentProvider paymentProvider;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentCenterProperties paymentCenterProperties;
    private final WalletService walletService;

    public WalletOperationService(WalletRechargeOrderRepository rechargeOrderRepository, UserRepository userRepository, FeePolicyService feePolicyService, PaymentProvider paymentProvider, PaymentOrderRepository paymentOrderRepository, PaymentCenterProperties paymentCenterProperties, WalletService walletService) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.userRepository = userRepository;
        this.feePolicyService = feePolicyService;
        this.paymentProvider = paymentProvider;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentCenterProperties = paymentCenterProperties;
        this.walletService = walletService;
    }

    @Transactional
    public WalletRechargeSummary createRecharge(Long userId, WalletRechargeRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if ("ALIPAY".equals(request.channel())) {
            return createAlipayRecharge(user, request.amount());
        }
        if ("WECHAT".equals(request.channel())) {
            WalletRechargeOrder order = rechargeOrderRepository.save(new WalletRechargeOrder(nextNo("WR"), user, "WECHAT", request.amount(), BigDecimal.ZERO, request.amount(), "PENDING_REVIEW"));
            return WalletRechargeSummary.from(order);
        }
        throw new BusinessException("不支持的充值渠道");
    }

    @Transactional
    public void markRechargePaidByPaymentOrder(String paymentOrderNo) {
        WalletRechargeOrder order = rechargeOrderRepository.findByPaymentOrderNo(paymentOrderNo)
                .orElseThrow(() -> new BusinessException("充值订单不存在"));
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        order.markPaid();
        walletService.credit(order.getUser().getId(), order.getAmount(), "WALLET_RECHARGE", order.getId(), "recharge:" + order.getId(), "PAYMENT_CALLBACK", null, "钱包充值到账");
    }

    @Transactional
    public WalletRechargeSummary approveWechatRecharge(Long rechargeId, Long adminId, String note) {
        WalletRechargeOrder order = rechargeOrderRepository.findById(rechargeId).orElseThrow(() -> new BusinessException("充值订单不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            throw new BusinessException("充值订单状态不可审核通过");
        }
        order.approve(admin, note);
        walletService.credit(order.getUser().getId(), order.getAmount(), "WALLET_RECHARGE", order.getId(), "wechat-recharge:" + order.getId(), "ADMIN", adminId, "微信人工充值到账");
        return WalletRechargeSummary.from(order);
    }

    private WalletRechargeSummary createAlipayRecharge(User user, BigDecimal amount) {
        BigDecimal fee = feePolicyService.calculateAlipayRechargeFee(amount);
        BigDecimal payAmount = amount.add(fee);
        WalletRechargeOrder recharge = rechargeOrderRepository.save(new WalletRechargeOrder(nextNo("WR"), user, "ALIPAY", amount, fee, payAmount, "PENDING_PAYMENT"));
        PaymentOrder order = paymentOrderRepository.save(new PaymentOrder(nextNo("CHP-WR"), "WALLET_RECHARGE", recharge.getId(), user, payAmount, paymentProvider.providerName(), LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes())));
        PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(order.getOrderNo(), "WALLET_RECHARGE", recharge.getId(), user.getId(), recharge.getRechargeNo(), payAmount, "CampusHub 钱包充值 " + recharge.getRechargeNo(), paymentCenterProperties.getCallbackUrl(), paymentCenterProperties.getExpireMinutes()));
        order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
        recharge.attachPaymentOrder(order.getOrderNo());
        return WalletRechargeSummary.from(recharge);
    }

    private String nextNo(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }
}
```

- [ ] **Step 6: Extend `PaymentService` callback for `WALLET_RECHARGE`**

Inject `WalletOperationService` into `PaymentService` and add to `applyBusinessPaymentResult` under `PAID`:

```java
if ("WALLET_RECHARGE".equals(order.getBusinessType())) {
    walletOperationService.markRechargePaidByPaymentOrder(order.getOrderNo());
}
```

Constructor addition:

```java
private final WalletOperationService walletOperationService;
```

- [ ] **Step 7: Add user recharge APIs**

Modify `WalletController.java`:

```java
private final WalletOperationService walletOperationService;

@PostMapping("/users/{userId}/recharges")
public ApiResponse<WalletRechargeSummary> createRecharge(@PathVariable Long userId, @Valid @RequestBody WalletRechargeRequest request) {
    return ApiResponse.ok(walletOperationService.createRecharge(userId, request));
}

@GetMapping("/users/{userId}/recharges")
public ApiResponse<List<WalletRechargeSummary>> listRecharges(@PathVariable Long userId) {
    return ApiResponse.ok(walletOperationService.listUserRecharges(userId));
}
```

Add this method to `WalletOperationService`:

```java
@Transactional(readOnly = true)
public List<WalletRechargeSummary> listUserRecharges(Long userId) {
    return rechargeOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WalletRechargeSummary::from).toList();
}
```

- [ ] **Step 8: Run recharge tests**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest test
```

Expected: PASS for recharge tests.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/campushub/wallet/WalletRechargeOrder.java backend/src/main/java/com/campushub/wallet/WalletRechargeOrderRepository.java backend/src/main/java/com/campushub/wallet/WalletRechargeRequest.java backend/src/main/java/com/campushub/wallet/WalletRechargeSummary.java backend/src/main/java/com/campushub/wallet/WalletOperationService.java backend/src/main/java/com/campushub/payment/PaymentService.java backend/src/main/java/com/campushub/wallet/WalletController.java backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java
git commit -m "add wallet recharge flow"
```

## Task 4: Add withdrawal requests and admin review

**Files:**
- Create: `backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequest.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequestRepository.java`
- Create: `backend/src/main/java/com/campushub/wallet/CreateWithdrawalRequest.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletWithdrawalSummary.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletController.java`
- Create: `backend/src/main/java/com/campushub/wallet/AdminWalletController.java`
- Test: `backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java`

- [ ] **Step 1: Add failing withdrawal test**

Append to `WalletOperationIntegrationTest.java`:

```java
@Autowired WalletWithdrawalRequestRepository withdrawalRequestRepository;

@Test
void withdrawalFreezesBalanceThenRejectUnfreezesAndCompleteDeductsFrozenBalance() {
    User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    User admin = userRepository.findByEmail("admin@mail.ustc.edu.cn").orElseThrow();
    walletService.credit(user.getId(), new BigDecimal("100.00"), "TEST", 10L, "withdraw-credit", "SYSTEM", null, "提现测试入账");

    WalletWithdrawalSummary rejected = walletOperationService.createWithdrawal(user.getId(), new CreateWithdrawalRequest(new BigDecimal("30.00"), "WECHAT", "微信昵称：同学A"));
    walletOperationService.rejectWithdrawal(rejected.id(), admin.getId(), "资料不完整");
    WalletAccount afterReject = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
    assertThat(afterReject.getBalance()).isEqualByComparingTo("100.00");
    assertThat(afterReject.getFrozenBalance()).isEqualByComparingTo("0.00");

    WalletWithdrawalSummary approved = walletOperationService.createWithdrawal(user.getId(), new CreateWithdrawalRequest(new BigDecimal("40.00"), "WECHAT", "微信昵称：同学A"));
    walletOperationService.approveWithdrawal(approved.id(), admin.getId(), "审核通过");
    walletOperationService.completeWithdrawal(approved.id(), admin.getId(), "已人工打款");
    WalletAccount afterComplete = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
    WalletWithdrawalRequest completed = withdrawalRequestRepository.findById(approved.id()).orElseThrow();
    assertThat(afterComplete.getBalance()).isEqualByComparingTo("60.00");
    assertThat(afterComplete.getFrozenBalance()).isEqualByComparingTo("0.00");
    assertThat(completed.getStatus()).isEqualTo("COMPLETED");
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest#withdrawalFreezesBalanceThenRejectUnfreezesAndCompleteDeductsFrozenBalance test
```

Expected: FAIL because withdrawal classes/methods do not exist.

- [ ] **Step 3: Create withdrawal entity and repository**

Create `WalletWithdrawalRequest.java`:

```java
package com.campushub.wallet;

import com.campushub.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_withdrawal_requests")
public class WalletWithdrawalRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "withdrawal_no", nullable = false)
    private String withdrawalNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String channel;
    @Column(name = "account_snapshot")
    private String accountSnapshot;
    @Column(nullable = false)
    private String status;
    @Column(name = "review_note")
    private String reviewNote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id")
    private User reviewer;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected WalletWithdrawalRequest() {}

    public WalletWithdrawalRequest(String withdrawalNo, User user, BigDecimal amount, String channel, String accountSnapshot) {
        this.withdrawalNo = withdrawalNo;
        this.user = user;
        this.amount = amount;
        this.channel = channel;
        this.accountSnapshot = accountSnapshot;
        this.status = "PENDING_REVIEW";
    }

    public Long getId() { return id; }
    public String getWithdrawalNo() { return withdrawalNo; }
    public User getUser() { return user; }
    public BigDecimal getAmount() { return amount; }
    public String getChannel() { return channel; }
    public String getAccountSnapshot() { return accountSnapshot; }
    public String getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public User getReviewer() { return reviewer; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void approve(User reviewer, String note) { this.status = "APPROVED"; this.reviewer = reviewer; this.reviewNote = note; this.reviewedAt = LocalDateTime.now(); }
    public void reject(User reviewer, String note) { this.status = "REJECTED"; this.reviewer = reviewer; this.reviewNote = note; this.reviewedAt = LocalDateTime.now(); }
    public void complete(User reviewer, String note) { this.status = "COMPLETED"; this.reviewer = reviewer; this.reviewNote = note; this.reviewedAt = LocalDateTime.now(); this.completedAt = LocalDateTime.now(); }
}
```

Create `WalletWithdrawalRequestRepository.java`:

```java
package com.campushub.wallet;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletWithdrawalRequestRepository extends JpaRepository<WalletWithdrawalRequest, Long> {
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findTop200ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findTop200ByStatusOrderByCreatedAtDesc(String status);
}
```

- [ ] **Step 4: Create withdrawal request/summary records**

Create `CreateWithdrawalRequest.java`:

```java
package com.campushub.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateWithdrawalRequest(
        @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String channel,
        String accountSnapshot) {
}
```

Create `WalletWithdrawalSummary.java`:

```java
package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletWithdrawalSummary(
        Long id,
        String withdrawalNo,
        Long userId,
        String userNickname,
        BigDecimal amount,
        String channel,
        String accountSnapshot,
        String status,
        String reviewNote,
        LocalDateTime reviewedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt) {
    public static WalletWithdrawalSummary from(WalletWithdrawalRequest request) {
        return new WalletWithdrawalSummary(
                request.getId(), request.getWithdrawalNo(), request.getUser().getId(), request.getUser().getNickname(),
                request.getAmount(), request.getChannel(), request.getAccountSnapshot(), request.getStatus(),
                request.getReviewNote(), request.getReviewedAt(), request.getCompletedAt(), request.getCreatedAt());
    }
}
```

- [ ] **Step 5: Add withdrawal service methods**

Add fields to `WalletOperationService`:

```java
private final WalletWithdrawalRequestRepository withdrawalRequestRepository;
```

Add methods:

```java
@Transactional
public WalletWithdrawalSummary createWithdrawal(Long userId, CreateWithdrawalRequest request) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
    WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.save(new WalletWithdrawalRequest(nextNo("WW"), user, request.amount(), request.channel(), request.accountSnapshot()));
    walletService.freeze(userId, request.amount(), "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-freeze:" + withdrawal.getId(), "USER", userId, "提现申请冻结余额");
    return WalletWithdrawalSummary.from(withdrawal);
}

@Transactional
public WalletWithdrawalSummary approveWithdrawal(Long withdrawalId, Long adminId, String note) {
    WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
    User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
    if (!"PENDING_REVIEW".equals(withdrawal.getStatus())) {
        throw new BusinessException("提现申请状态不可审核通过");
    }
    withdrawal.approve(admin, note);
    return WalletWithdrawalSummary.from(withdrawal);
}

@Transactional
public WalletWithdrawalSummary rejectWithdrawal(Long withdrawalId, Long adminId, String note) {
    WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
    User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
    if (!"PENDING_REVIEW".equals(withdrawal.getStatus())) {
        throw new BusinessException("提现申请状态不可拒绝");
    }
    withdrawal.reject(admin, note);
    walletService.unfreeze(withdrawal.getUser().getId(), withdrawal.getAmount(), "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-reject:" + withdrawal.getId(), "ADMIN", adminId, "提现拒绝解冻余额");
    return WalletWithdrawalSummary.from(withdrawal);
}

@Transactional
public WalletWithdrawalSummary completeWithdrawal(Long withdrawalId, Long adminId, String note) {
    WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
    User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
    if (!"APPROVED".equals(withdrawal.getStatus())) {
        throw new BusinessException("提现申请状态不可完成");
    }
    walletService.debit(withdrawal.getUser().getId(), withdrawal.getAmount(), "WITHDRAW", "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-complete:" + withdrawal.getId(), "ADMIN", adminId, "提现人工打款完成");
    withdrawal.complete(admin, note);
    return WalletWithdrawalSummary.from(withdrawal);
}

@Transactional(readOnly = true)
public List<WalletWithdrawalSummary> listUserWithdrawals(Long userId) {
    return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WalletWithdrawalSummary::from).toList();
}
```

- [ ] **Step 6: Add controller endpoints**

Modify `WalletController.java`:

```java
@PostMapping("/users/{userId}/withdrawals")
public ApiResponse<WalletWithdrawalSummary> createWithdrawal(@PathVariable Long userId, @Valid @RequestBody CreateWithdrawalRequest request) {
    return ApiResponse.ok(walletOperationService.createWithdrawal(userId, request));
}

@GetMapping("/users/{userId}/withdrawals")
public ApiResponse<List<WalletWithdrawalSummary>> listWithdrawals(@PathVariable Long userId) {
    return ApiResponse.ok(walletOperationService.listUserWithdrawals(userId));
}
```

Create `AdminWalletController.java`:

```java
package com.campushub.wallet;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletController {
    private final WalletOperationService walletOperationService;

    public AdminWalletController(WalletOperationService walletOperationService) {
        this.walletOperationService = walletOperationService;
    }

    @GetMapping("/recharges")
    public ApiResponse<List<WalletRechargeSummary>> listRecharges(@RequestParam(required = false) String status) {
        return ApiResponse.ok(walletOperationService.listAdminRecharges(status));
    }

    @PostMapping("/recharges/{id}/approve")
    public ApiResponse<WalletRechargeSummary> approveRecharge(@PathVariable Long id, @RequestParam Long adminId, @RequestParam(defaultValue = "微信充值审核通过") String note) {
        return ApiResponse.ok(walletOperationService.approveWechatRecharge(id, adminId, note));
    }

    @PostMapping("/recharges/{id}/reject")
    public ApiResponse<WalletRechargeSummary> rejectRecharge(@PathVariable Long id, @RequestParam Long adminId, @RequestParam(defaultValue = "微信充值审核拒绝") String note) {
        return ApiResponse.ok(walletOperationService.rejectWechatRecharge(id, adminId, note));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<WalletWithdrawalSummary>> listWithdrawals(@RequestParam(required = false) String status) {
        return ApiResponse.ok(walletOperationService.listAdminWithdrawals(status));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ApiResponse<WalletWithdrawalSummary> approveWithdrawal(@PathVariable Long id, @RequestParam Long adminId, @RequestParam(defaultValue = "提现审核通过") String note) {
        return ApiResponse.ok(walletOperationService.approveWithdrawal(id, adminId, note));
    }

    @PostMapping("/withdrawals/{id}/complete")
    public ApiResponse<WalletWithdrawalSummary> completeWithdrawal(@PathVariable Long id, @RequestParam Long adminId, @RequestParam(defaultValue = "提现已人工打款") String note) {
        return ApiResponse.ok(walletOperationService.completeWithdrawal(id, adminId, note));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ApiResponse<WalletWithdrawalSummary> rejectWithdrawal(@PathVariable Long id, @RequestParam Long adminId, @RequestParam(defaultValue = "提现审核拒绝") String note) {
        return ApiResponse.ok(walletOperationService.rejectWithdrawal(id, adminId, note));
    }
}
```

Add `listAdminRecharges`, `rejectWechatRecharge`, and `listAdminWithdrawals` to `WalletOperationService` using repository `findTop200...` methods.

- [ ] **Step 7: Run withdrawal tests**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest#withdrawalFreezesBalanceThenRejectUnfreezesAndCompleteDeductsFrozenBalance test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequest.java backend/src/main/java/com/campushub/wallet/WalletWithdrawalRequestRepository.java backend/src/main/java/com/campushub/wallet/CreateWithdrawalRequest.java backend/src/main/java/com/campushub/wallet/WalletWithdrawalSummary.java backend/src/main/java/com/campushub/wallet/WalletOperationService.java backend/src/main/java/com/campushub/wallet/WalletController.java backend/src/main/java/com/campushub/wallet/AdminWalletController.java backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java
git commit -m "add wallet withdrawal operations"
```

## Task 5: Add goods online escrow flow

**Files:**
- Create: `backend/src/main/java/com/campushub/wallet/WalletFrozenRecord.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletFrozenRecordRepository.java`
- Create: `backend/src/main/java/com/campushub/wallet/WalletFrozenRecordSummary.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsOrderSummary.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsService.java`
- Modify: `backend/src/main/java/com/campushub/goods/GoodsController.java`
- Test: `backend/src/test/java/com/campushub/goods/GoodsEscrowIntegrationTest.java`

- [ ] **Step 1: Write failing goods escrow test**

Create `backend/src/test/java/com/campushub/goods/GoodsEscrowIntegrationTest.java`:

```java
package com.campushub.goods;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import com.campushub.wallet.WalletService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GoodsEscrowIntegrationTest {
    @Autowired GoodsService goodsService;
    @Autowired GoodsOrderRepository goodsOrderRepository;
    @Autowired GoodsRepository goodsRepository;
    @Autowired UserRepository userRepository;
    @Autowired WalletService walletService;
    @Autowired WalletAccountRepository walletAccountRepository;

    @Test
    void goodsEscrowFreezesBuyerBalanceAndReleasesToSeller() {
        User buyer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        Goods goods = goodsRepository.findById(1L).orElseThrow();
        User seller = goods.getPublisher();
        walletService.credit(buyer.getId(), new BigDecimal("100.00"), "TEST", 99L, "goods-escrow-credit", "SYSTEM", null, "二手托管测试入账");

        GoodsOrderSummary order = goodsService.createOnlineEscrowOrder(goods.getId(), buyer.getId());
        goodsService.freezeGoodsEscrow(order.id(), buyer.getId());
        WalletAccount buyerAfterFreeze = walletAccountRepository.findByUserId(buyer.getId()).orElseThrow();
        assertThat(buyerAfterFreeze.getFrozenBalance()).isEqualByComparingTo(order.amount().add(order.serviceFee()));

        goodsService.confirmGoodsEscrow(order.id(), buyer.getId());
        GoodsOrder completed = goodsOrderRepository.findById(order.id()).orElseThrow();
        WalletAccount sellerWallet = walletAccountRepository.findByUserId(seller.getId()).orElseThrow();
        assertThat(completed.getEscrowStatus()).isEqualTo("RELEASED");
        assertThat(sellerWallet.getBalance()).isEqualByComparingTo(order.amount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=GoodsEscrowIntegrationTest test
```

Expected: FAIL because goods escrow methods and summary fields do not exist.

- [ ] **Step 3: Add frozen record entity and repository**

Create `WalletFrozenRecord.java`, `WalletFrozenRecordRepository.java`, and `WalletFrozenRecordSummary.java` matching `wallet_frozen_records` fields. Use statuses `FROZEN`, `RELEASED`, `UNFROZEN` and business type `GOODS_ESCROW`.

Core constructor for `WalletFrozenRecord`:

```java
public WalletFrozenRecord(String freezeNo, User user, String businessType, Long businessId, BigDecimal amount, String remark) {
    this.freezeNo = freezeNo;
    this.user = user;
    this.businessType = businessType;
    this.businessId = businessId;
    this.amount = amount;
    this.status = "FROZEN";
    this.remark = remark;
}
```

Status methods:

```java
public void markReleased() { this.status = "RELEASED"; this.releasedAt = LocalDateTime.now(); }
public void markUnfrozen() { this.status = "UNFROZEN"; this.releasedAt = LocalDateTime.now(); }
```

Repository:

```java
Optional<WalletFrozenRecord> findByBusinessTypeAndBusinessIdAndStatus(String businessType, Long businessId, String status);
List<WalletFrozenRecord> findByUserIdOrderByFrozenAtDesc(Long userId);
List<WalletFrozenRecord> findTop200ByOrderByFrozenAtDesc();
```

- [ ] **Step 4: Extend `GoodsOrderSummary`**

Add fields to the record and mapper:

```java
String tradeMode,
String escrowStatus,
Long buyerId,
Long sellerId,
BigDecimal escrowAmount,
BigDecimal serviceFee,
LocalDateTime escrowFrozenAt,
LocalDateTime escrowReleasedAt,
LocalDateTime escrowUnfrozenAt,
String cancelReason,
String disputeReason
```

Pass `order.getTradeMode()`, `order.getEscrowStatus()`, buyer/seller IDs when present, `order.getEscrowAmount()`, `order.getPlatformServiceFee()`, and timestamp/reason getters.

- [ ] **Step 5: Implement goods escrow service methods**

Inject `WalletService`, `FeePolicyService`, `WalletFrozenRecordRepository` into `GoodsService`.

Add methods:

```java
@Transactional
public GoodsOrderSummary createOnlineEscrowOrder(Long goodsId, Long buyerId) {
    Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
    User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("买家不存在"));
    BigDecimal amount = goods.getPrice();
    BigDecimal fee = feePolicyService.calculateOnlineEscrowFee(amount);
    GoodsOrder order = new GoodsOrder(goods, buyer, amount, fee);
    order.enableOnlineEscrow(buyer, goods.getPublisher(), amount, fee);
    return GoodsOrderSummary.from(goodsOrderRepository.save(order));
}

@Transactional
public GoodsOrderSummary freezeGoodsEscrow(Long orderId, Long buyerId) {
    GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
    if (!order.getBuyer().getId().equals(buyerId)) {
        throw new BusinessException("只有买家可以冻结托管资金");
    }
    if (!"PENDING_FREEZE".equals(order.getEscrowStatus())) {
        throw new BusinessException("订单状态不可冻结");
    }
    BigDecimal total = order.getEscrowAmount().add(order.getPlatformServiceFee());
    walletService.freeze(buyerId, total, "GOODS_ESCROW", order.getId(), "goods-escrow-freeze:" + order.getId(), "USER", buyerId, "二手线上交易托管冻结");
    walletFrozenRecordRepository.save(new WalletFrozenRecord("FRZ-GOODS-" + order.getId(), order.getBuyer(), "GOODS_ESCROW", order.getId(), total, "二手交易托管冻结"));
    order.markEscrowFrozen(LocalDateTime.now());
    return GoodsOrderSummary.from(order);
}

@Transactional
public GoodsOrderSummary confirmGoodsEscrow(Long orderId, Long buyerId) {
    GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
    if (!order.getBuyer().getId().equals(buyerId)) {
        throw new BusinessException("只有买家可以确认交易完成");
    }
    if (!"FROZEN".equals(order.getEscrowStatus())) {
        throw new BusinessException("订单状态不可确认完成");
    }
    walletService.transferFrozen(order.getBuyer().getId(), order.getSeller().getId(), order.getEscrowAmount(), "GOODS_ESCROW", order.getId(), "goods-escrow-release:" + order.getId(), "USER", buyerId, "二手托管本金划转给卖家");
    walletService.debit(order.getBuyer().getId(), order.getPlatformServiceFee(), "SERVICE_FEE", "GOODS_ESCROW", order.getId(), "goods-escrow-fee:" + order.getId(), "SYSTEM", null, "二手线上托管服务费");
    walletFrozenRecordRepository.findByBusinessTypeAndBusinessIdAndStatus("GOODS_ESCROW", order.getId(), "FROZEN").ifPresent(WalletFrozenRecord::markReleased);
    order.markEscrowReleased(LocalDateTime.now());
    order.getGoods().markSold();
    return GoodsOrderSummary.from(order);
}

@Transactional
public GoodsOrderSummary cancelGoodsEscrow(Long orderId, Long buyerId, String reason) {
    GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
    if (!order.getBuyer().getId().equals(buyerId)) {
        throw new BusinessException("只有买家可以取消托管交易");
    }
    if (!"FROZEN".equals(order.getEscrowStatus()) && !"PENDING_FREEZE".equals(order.getEscrowStatus())) {
        throw new BusinessException("订单状态不可取消");
    }
    if ("FROZEN".equals(order.getEscrowStatus())) {
        walletService.unfreeze(order.getBuyer().getId(), order.getEscrowAmount().add(order.getPlatformServiceFee()), "GOODS_ESCROW", order.getId(), "goods-escrow-cancel:" + order.getId(), "USER", buyerId, "二手托管取消解冻");
    }
    walletFrozenRecordRepository.findByBusinessTypeAndBusinessIdAndStatus("GOODS_ESCROW", order.getId(), "FROZEN").ifPresent(WalletFrozenRecord::markUnfrozen);
    order.markEscrowCanceled(LocalDateTime.now(), reason);
    return GoodsOrderSummary.from(order);
}

@Transactional
public GoodsOrderSummary disputeGoodsEscrow(Long orderId, Long userId, String reason) {
    GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
    if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
        throw new BusinessException("只有交易双方可以发起争议");
    }
    if (!"FROZEN".equals(order.getEscrowStatus())) {
        throw new BusinessException("订单状态不可发起争议");
    }
    order.markEscrowDisputed(reason);
    return GoodsOrderSummary.from(order);
}
```

- [ ] **Step 6: Add goods escrow controller endpoints**

Modify `GoodsController.java`:

```java
@PostMapping("/{goodsId}/orders/escrow")
public ApiResponse<GoodsOrderSummary> createOnlineEscrowOrder(@PathVariable Long goodsId, @RequestParam Long buyerId) {
    return ApiResponse.ok(goodsService.createOnlineEscrowOrder(goodsId, buyerId));
}

@PostMapping("/orders/{orderId}/escrow/freeze")
public ApiResponse<GoodsOrderSummary> freezeEscrow(@PathVariable Long orderId, @RequestParam Long buyerId) {
    return ApiResponse.ok(goodsService.freezeGoodsEscrow(orderId, buyerId));
}

@PostMapping("/orders/{orderId}/escrow/confirm")
public ApiResponse<GoodsOrderSummary> confirmEscrow(@PathVariable Long orderId, @RequestParam Long buyerId) {
    return ApiResponse.ok(goodsService.confirmGoodsEscrow(orderId, buyerId));
}

@PostMapping("/orders/{orderId}/escrow/cancel")
public ApiResponse<GoodsOrderSummary> cancelEscrow(@PathVariable Long orderId, @RequestParam Long buyerId, @RequestParam(defaultValue = "买家取消线上托管交易") String reason) {
    return ApiResponse.ok(goodsService.cancelGoodsEscrow(orderId, buyerId, reason));
}

@PostMapping("/orders/{orderId}/escrow/dispute")
public ApiResponse<GoodsOrderSummary> disputeEscrow(@PathVariable Long orderId, @RequestParam Long userId, @RequestParam String reason) {
    return ApiResponse.ok(goodsService.disputeGoodsEscrow(orderId, userId, reason));
}
```

- [ ] **Step 7: Run goods escrow test**

```bash
mvn -f backend/pom.xml -Dtest=GoodsEscrowIntegrationTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/campushub/wallet/WalletFrozenRecord.java backend/src/main/java/com/campushub/wallet/WalletFrozenRecordRepository.java backend/src/main/java/com/campushub/wallet/WalletFrozenRecordSummary.java backend/src/main/java/com/campushub/goods/GoodsOrderSummary.java backend/src/main/java/com/campushub/goods/GoodsService.java backend/src/main/java/com/campushub/goods/GoodsController.java backend/src/test/java/com/campushub/goods/GoodsEscrowIntegrationTest.java
git commit -m "add goods online escrow flow"
```

## Task 6: Add wallet admin lists and frozen-record APIs

**Files:**
- Modify: `backend/src/main/java/com/campushub/wallet/WalletOperationService.java`
- Modify: `backend/src/main/java/com/campushub/wallet/WalletController.java`
- Modify: `backend/src/main/java/com/campushub/wallet/AdminWalletController.java`
- Test: `backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java`

- [ ] **Step 1: Add failing admin list test**

Append to `WalletOperationIntegrationTest.java`:

```java
@Autowired WalletController walletController;
@Autowired AdminWalletController adminWalletController;

@Test
void adminWalletEndpointsListRechargeWithdrawalFrozenRecordsAndFlows() {
    User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
    walletService.credit(user.getId(), new BigDecimal("20.00"), "TEST", 20L, "admin-wallet-credit", "SYSTEM", null, "管理端钱包列表测试");
    walletOperationService.createRecharge(user.getId(), new WalletRechargeRequest("WECHAT", new BigDecimal("5.00"), "微信充值列表测试"));
    walletOperationService.createWithdrawal(user.getId(), new CreateWithdrawalRequest(new BigDecimal("3.00"), "WECHAT", "微信昵称：同学A"));

    assertThat(walletController.listUserFlows(user.getId()).data()).isNotEmpty();
    assertThat(adminWalletController.listRecharges(null).data()).isNotEmpty();
    assertThat(adminWalletController.listWithdrawals(null).data()).isNotEmpty();
    assertThat(adminWalletController.listFlows().data()).isNotEmpty();
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest#adminWalletEndpointsListRechargeWithdrawalFrozenRecordsAndFlows test
```

Expected: FAIL until admin flow/frozen methods are added.

- [ ] **Step 3: Extend operation service list methods**

Add to `WalletOperationService`:

```java
@Transactional(readOnly = true)
public List<WalletRechargeSummary> listAdminRecharges(String status) {
    return (status == null || status.isBlank()
            ? rechargeOrderRepository.findTop200ByOrderByCreatedAtDesc()
            : rechargeOrderRepository.findTop200ByStatusOrderByCreatedAtDesc(status))
            .stream().map(WalletRechargeSummary::from).toList();
}

@Transactional(readOnly = true)
public List<WalletWithdrawalSummary> listAdminWithdrawals(String status) {
    return (status == null || status.isBlank()
            ? withdrawalRequestRepository.findTop200ByOrderByCreatedAtDesc()
            : withdrawalRequestRepository.findTop200ByStatusOrderByCreatedAtDesc(status))
            .stream().map(WalletWithdrawalSummary::from).toList();
}

@Transactional(readOnly = true)
public List<WalletFlowSummary> listAdminFlows() {
    return walletFlowRepository.findTop300ByOrderByCreatedAtDesc().stream().map(WalletFlowSummary::from).toList();
}

@Transactional(readOnly = true)
public List<WalletFrozenRecordSummary> listAdminFrozenRecords() {
    return walletFrozenRecordRepository.findTop200ByOrderByFrozenAtDesc().stream().map(WalletFrozenRecordSummary::from).toList();
}
```

- [ ] **Step 4: Extend `WalletFlowSummary`**

Modify `WalletFlowSummary.java` to include:

```java
String flowType,
BigDecimal availableBalanceAfter,
BigDecimal frozenBalanceAfter,
String idempotencyKey,
Long counterpartyUserId,
String createdBy,
Long operatorId
```

Update `from(WalletFlow flow)` to pass the new getters.

- [ ] **Step 5: Add admin controller endpoints**

Add to `AdminWalletController.java`:

```java
@GetMapping("/flows")
public ApiResponse<List<WalletFlowSummary>> listFlows() {
    return ApiResponse.ok(walletOperationService.listAdminFlows());
}

@GetMapping("/frozen-records")
public ApiResponse<List<WalletFrozenRecordSummary>> listFrozenRecords() {
    return ApiResponse.ok(walletOperationService.listAdminFrozenRecords());
}
```

Add to `WalletController.java`:

```java
@GetMapping("/users/{userId}/frozen-items")
public ApiResponse<List<WalletFrozenRecordSummary>> listUserFrozenItems(@PathVariable Long userId) {
    return ApiResponse.ok(walletOperationService.listUserFrozenRecords(userId));
}
```

Add `listUserFrozenRecords` to `WalletOperationService` using `walletFrozenRecordRepository.findByUserIdOrderByFrozenAtDesc(userId)`.

- [ ] **Step 6: Run admin list test**

```bash
mvn -f backend/pom.xml -Dtest=WalletOperationIntegrationTest#adminWalletEndpointsListRechargeWithdrawalFrozenRecordsAndFlows test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/campushub/wallet/WalletOperationService.java backend/src/main/java/com/campushub/wallet/WalletController.java backend/src/main/java/com/campushub/wallet/AdminWalletController.java backend/src/main/java/com/campushub/wallet/WalletFlowSummary.java backend/src/test/java/com/campushub/wallet/WalletOperationIntegrationTest.java
git commit -m "add wallet operations monitor APIs"
```

## Task 7: Add frontend API types and wallet user UI

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Modify: `frontend/src/views/WalletView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add frontend wallet types and API functions**

Modify `frontend/src/api/campushub.ts` and add these exports near existing wallet/payment types:

```ts
export interface WalletRechargeSummary {
  id: number
  rechargeNo: string
  userId: number
  userNickname: string
  channel: string
  amount: number
  channelFee: number
  payAmount: number
  status: string
  paymentOrderNo?: string
  reviewNote?: string
  reviewedAt?: string
  createdAt?: string
}

export interface WalletWithdrawalSummary {
  id: number
  withdrawalNo: string
  userId: number
  userNickname: string
  amount: number
  channel: string
  accountSnapshot?: string
  status: string
  reviewNote?: string
  reviewedAt?: string
  completedAt?: string
  createdAt?: string
}

export interface WalletFrozenRecordSummary {
  id: number
  freezeNo: string
  userId: number
  userNickname: string
  businessType: string
  businessId: number
  amount: number
  status: string
  frozenAt: string
  releasedAt?: string
  remark?: string
}

export function createWalletRecharge(userId: number, payload: { channel: string; amount: number; remark?: string }) {
  return api.post<WalletRechargeSummary>(`/wallet/users/${userId}/recharges`, payload)
}

export function listWalletRecharges(userId: number) {
  return api.get<WalletRechargeSummary[]>(`/wallet/users/${userId}/recharges`)
}

export function createWalletWithdrawal(userId: number, payload: { amount: number; channel: string; accountSnapshot?: string }) {
  return api.post<WalletWithdrawalSummary>(`/wallet/users/${userId}/withdrawals`, payload)
}

export function listWalletWithdrawals(userId: number) {
  return api.get<WalletWithdrawalSummary[]>(`/wallet/users/${userId}/withdrawals`)
}

export function listWalletFrozenItems(userId: number) {
  return api.get<WalletFrozenRecordSummary[]>(`/wallet/users/${userId}/frozen-items`)
}
```

- [ ] **Step 2: Update `WalletView.vue` script**

In `WalletView.vue`, import new APIs and add state/functions:

```ts
import { createWalletRecharge, createWalletWithdrawal, listWalletFrozenItems, listWalletRecharges, listWalletWithdrawals, type WalletFrozenRecordSummary, type WalletRechargeSummary, type WalletWithdrawalSummary } from '../api/campushub'

const recharges = ref<WalletRechargeSummary[]>([])
const withdrawals = ref<WalletWithdrawalSummary[]>([])
const frozenItems = ref<WalletFrozenRecordSummary[]>([])
const rechargeDialogVisible = ref(false)
const withdrawalDialogVisible = ref(false)
const rechargeForm = reactive({ channel: 'ALIPAY', amount: 10, remark: '' })
const withdrawalForm = reactive({ channel: 'WECHAT', amount: 10, accountSnapshot: '' })

async function loadWalletOperations() {
  const userId = currentUserId.value
  const [rechargeData, withdrawalData, frozenData] = await Promise.all([
    listWalletRecharges(userId),
    listWalletWithdrawals(userId),
    listWalletFrozenItems(userId)
  ])
  recharges.value = rechargeData
  withdrawals.value = withdrawalData
  frozenItems.value = frozenData
}

async function submitRecharge() {
  const result = await createWalletRecharge(currentUserId.value, rechargeForm)
  ElMessage.success(result.channel === 'WECHAT' ? '微信充值已提交人工审核' : '充值支付单已创建')
  rechargeDialogVisible.value = false
  if (result.paymentOrderNo) {
    ElMessage.info(`支付订单：${result.paymentOrderNo}`)
  }
  await loadAll()
}

async function submitWithdrawal() {
  await createWalletWithdrawal(currentUserId.value, withdrawalForm)
  ElMessage.success('提现申请已提交，金额已冻结等待审核')
  withdrawalDialogVisible.value = false
  await loadAll()
}
```

Ensure the existing `loadAll` also awaits `loadWalletOperations()`.

- [ ] **Step 3: Update `WalletView.vue` template**

Add tabs/sections under the wallet overview:

```vue
<el-tabs class="wallet-tabs">
  <el-tab-pane label="充值">
    <div class="payment-actions">
      <el-button type="primary" @click="rechargeDialogVisible = true">发起充值</el-button>
    </div>
    <div class="responsive-card-grid">
      <article v-for="item in recharges" :key="item.id" class="info-card">
        <strong>{{ item.channel }} 充值 {{ item.amount }} 元</strong>
        <p>实际支付：{{ item.payAmount }} 元，手续费：{{ item.channelFee }} 元</p>
        <p>状态：{{ item.status }}</p>
        <p v-if="item.paymentOrderNo">支付单：{{ item.paymentOrderNo }}</p>
      </article>
    </div>
  </el-tab-pane>
  <el-tab-pane label="提现">
    <div class="payment-actions">
      <el-button @click="withdrawalDialogVisible = true">申请提现</el-button>
    </div>
    <div class="responsive-card-grid">
      <article v-for="item in withdrawals" :key="item.id" class="info-card">
        <strong>{{ item.channel }} 提现 {{ item.amount }} 元</strong>
        <p>状态：{{ item.status }}</p>
        <p v-if="item.reviewNote">审核备注：{{ item.reviewNote }}</p>
      </article>
    </div>
  </el-tab-pane>
  <el-tab-pane label="冻结明细">
    <div class="responsive-card-grid">
      <article v-for="item in frozenItems" :key="item.id" class="info-card">
        <strong>{{ item.businessType }} 冻结 {{ item.amount }} 元</strong>
        <p>状态：{{ item.status }}</p>
        <p>{{ item.remark }}</p>
      </article>
    </div>
  </el-tab-pane>
</el-tabs>

<el-dialog v-model="rechargeDialogVisible" title="钱包充值" width="420px">
  <el-form label-width="90px">
    <el-form-item label="渠道">
      <el-select v-model="rechargeForm.channel">
        <el-option label="支付宝实时到账（0.6% 手续费）" value="ALIPAY" />
        <el-option label="微信人工审核（免手续费）" value="WECHAT" />
      </el-select>
    </el-form-item>
    <el-form-item label="金额">
      <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" />
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="rechargeForm.remark" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="rechargeDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="submitRecharge">提交</el-button>
  </template>
</el-dialog>

<el-dialog v-model="withdrawalDialogVisible" title="申请提现" width="420px">
  <el-alert type="warning" show-icon title="Phase 9 提现为人工审核与人工打款，不会自动调用外部转账通道。" />
  <el-form label-width="90px" class="dialog-form">
    <el-form-item label="渠道">
      <el-select v-model="withdrawalForm.channel">
        <el-option label="微信" value="WECHAT" />
        <el-option label="支付宝" value="ALIPAY" />
        <el-option label="线下" value="OFFLINE" />
      </el-select>
    </el-form-item>
    <el-form-item label="金额">
      <el-input-number v-model="withdrawalForm.amount" :min="0.01" :precision="2" />
    </el-form-item>
    <el-form-item label="账号摘要">
      <el-input v-model="withdrawalForm.accountSnapshot" placeholder="只填写昵称或尾号，不填写敏感完整凭证" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="withdrawalDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="submitWithdrawal">提交</el-button>
  </template>
</el-dialog>
```

- [ ] **Step 4: Add responsive styles**

Modify `frontend/src/styles.css`:

```css
.wallet-tabs {
  margin-top: 18px;
}

.responsive-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.info-card {
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 14px;
  background: var(--surface-color);
  word-break: break-word;
}

.dialog-form {
  margin-top: 12px;
}
```

- [ ] **Step 5: Build frontend**

Use server-side Docker or approved environment. If local execution is approved:

```bash
npm --prefix frontend run build
```

Expected: PASS with only known Vite large chunk and dependency pure-comment warnings.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/campushub.ts frontend/src/views/WalletView.vue frontend/src/styles.css
git commit -m "add wallet recharge withdrawal UI"
```

## Task 8: Add frontend admin wallet and goods escrow UI

**Files:**
- Modify: `frontend/src/api/campushub.ts`
- Create: `frontend/src/views/AdminWalletView.vue`
- Modify: `frontend/src/views/GoodsDetailView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add admin and escrow API functions**

Modify `frontend/src/api/campushub.ts`:

```ts
export function listAdminWalletRecharges(status?: string) {
  return api.get<WalletRechargeSummary[]>('/admin/wallet/recharges', { params: { status } })
}

export function approveAdminWalletRecharge(id: number, adminId: number, note: string) {
  return api.post<WalletRechargeSummary>(`/admin/wallet/recharges/${id}/approve`, null, { params: { adminId, note } })
}

export function rejectAdminWalletRecharge(id: number, adminId: number, note: string) {
  return api.post<WalletRechargeSummary>(`/admin/wallet/recharges/${id}/reject`, null, { params: { adminId, note } })
}

export function listAdminWalletWithdrawals(status?: string) {
  return api.get<WalletWithdrawalSummary[]>('/admin/wallet/withdrawals', { params: { status } })
}

export function approveAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return api.post<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/approve`, null, { params: { adminId, note } })
}

export function completeAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return api.post<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/complete`, null, { params: { adminId, note } })
}

export function rejectAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return api.post<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/reject`, null, { params: { adminId, note } })
}

export function createGoodsEscrowOrder(goodsId: number, buyerId: number) {
  return api.post<GoodsOrderSummary>(`/goods/${goodsId}/orders/escrow`, null, { params: { buyerId } })
}

export function freezeGoodsEscrow(orderId: number, buyerId: number) {
  return api.post<GoodsOrderSummary>(`/goods/orders/${orderId}/escrow/freeze`, null, { params: { buyerId } })
}

export function confirmGoodsEscrow(orderId: number, buyerId: number) {
  return api.post<GoodsOrderSummary>(`/goods/orders/${orderId}/escrow/confirm`, null, { params: { buyerId } })
}
```

Extend `GoodsOrderSummary` with `tradeMode`, `escrowStatus`, `escrowAmount`, `serviceFee`, and escrow timestamps.

- [ ] **Step 2: Create admin wallet view**

Create `frontend/src/views/AdminWalletView.vue`:

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approveAdminWalletRecharge, approveAdminWalletWithdrawal, completeAdminWalletWithdrawal, listAdminWalletRecharges, listAdminWalletWithdrawals, rejectAdminWalletRecharge, rejectAdminWalletWithdrawal, type WalletRechargeSummary, type WalletWithdrawalSummary } from '../api/campushub'
import EmptyState from '../components/EmptyState.vue'
import PageActions from '../components/PageActions.vue'

const loading = ref(false)
const adminId = ref(1)
const rechargeStatus = ref('')
const withdrawalStatus = ref('')
const recharges = ref<WalletRechargeSummary[]>([])
const withdrawals = ref<WalletWithdrawalSummary[]>([])

async function loadAll() {
  loading.value = true
  try {
    const [rechargeData, withdrawalData] = await Promise.all([
      listAdminWalletRecharges(rechargeStatus.value || undefined),
      listAdminWalletWithdrawals(withdrawalStatus.value || undefined)
    ])
    recharges.value = rechargeData
    withdrawals.value = withdrawalData
  } finally {
    loading.value = false
  }
}

async function approveRecharge(id: number) {
  await approveAdminWalletRecharge(id, adminId.value, '微信充值人工审核通过')
  ElMessage.success('充值已审核通过')
  await loadAll()
}

async function rejectRecharge(id: number) {
  await rejectAdminWalletRecharge(id, adminId.value, '微信充值审核拒绝')
  ElMessage.success('充值已拒绝')
  await loadAll()
}

async function approveWithdrawal(id: number) {
  await approveAdminWalletWithdrawal(id, adminId.value, '提现审核通过')
  ElMessage.success('提现已审核通过')
  await loadAll()
}

async function completeWithdrawal(id: number) {
  await completeAdminWalletWithdrawal(id, adminId.value, '提现已人工打款')
  ElMessage.success('提现已完成')
  await loadAll()
}

async function rejectWithdrawal(id: number) {
  await rejectAdminWalletWithdrawal(id, adminId.value, '提现审核拒绝')
  ElMessage.success('提现已拒绝并解冻')
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <section class="page-section">
    <PageActions title="钱包运营" description="处理微信充值审核、提现审核和钱包资金运营；不显示 token、secret 或支付宝密钥。">
      <el-input-number v-model="adminId" :min="1" />
      <el-button @click="loadAll">刷新</el-button>
    </PageActions>

    <el-tabs>
      <el-tab-pane label="充值审核">
        <el-select v-model="rechargeStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已到账" value="PAID" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <EmptyState v-if="!recharges.length && !loading" title="暂无充值订单" description="用户发起充值后会出现在这里。" />
        <div class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card">
            <strong>{{ item.userNickname }} {{ item.channel }} 充值 {{ item.amount }} 元</strong>
            <p>状态：{{ item.status }}；实际支付：{{ item.payAmount }} 元</p>
            <div class="payment-actions" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" @click="approveRecharge(item.id)">通过</el-button>
              <el-button @click="rejectRecharge(item.id)">拒绝</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现审核">
        <el-select v-model="withdrawalStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <EmptyState v-if="!withdrawals.length && !loading" title="暂无提现申请" description="用户申请提现后会出现在这里。" />
        <div class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card">
            <strong>{{ item.userNickname }} {{ item.channel }} 提现 {{ item.amount }} 元</strong>
            <p>状态：{{ item.status }}；账号摘要：{{ item.accountSnapshot }}</p>
            <div class="payment-actions" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" @click="approveWithdrawal(item.id)">通过</el-button>
              <el-button @click="rejectWithdrawal(item.id)">拒绝</el-button>
            </div>
            <div class="payment-actions" v-if="item.status === 'APPROVED'">
              <el-button type="success" @click="completeWithdrawal(item.id)">确认已打款</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
```

- [ ] **Step 3: Add route and nav**

Modify `frontend/src/router/index.ts`:

```ts
{
  path: '/admin/wallet',
  name: 'admin-wallet',
  component: () => import('../views/AdminWalletView.vue')
}
```

Modify `frontend/src/layouts/MainLayout.vue` admin navigation list:

```ts
{ label: '钱包运营', path: '/admin/wallet', icon: 'Wallet' }
```

- [ ] **Step 4: Add goods detail escrow actions**

In `GoodsDetailView.vue`, add buttons around existing contact/intent actions:

```vue
<el-alert type="info" show-icon title="线上托管交易会从买方余额冻结商品金额和平台服务费，确认交易成功后划转给卖方。" />
<div class="payment-actions">
  <el-button type="primary" @click="startEscrowTrade">创建线上托管订单</el-button>
  <el-button v-if="currentEscrowOrder?.escrowStatus === 'PENDING_FREEZE'" @click="freezeEscrowTrade">冻结余额</el-button>
  <el-button v-if="currentEscrowOrder?.escrowStatus === 'FROZEN'" type="success" @click="confirmEscrowTrade">确认完成</el-button>
</div>
```

Add script functions:

```ts
const currentEscrowOrder = ref<GoodsOrderSummary | null>(null)

async function startEscrowTrade() {
  currentEscrowOrder.value = await createGoodsEscrowOrder(Number(route.params.id), currentUserId.value)
  ElMessage.success('线上托管订单已创建')
}

async function freezeEscrowTrade() {
  if (!currentEscrowOrder.value) return
  currentEscrowOrder.value = await freezeGoodsEscrow(currentEscrowOrder.value.id, currentUserId.value)
  ElMessage.success('托管金额已冻结')
}

async function confirmEscrowTrade() {
  if (!currentEscrowOrder.value) return
  currentEscrowOrder.value = await confirmGoodsEscrow(currentEscrowOrder.value.id, currentUserId.value)
  ElMessage.success('交易已确认完成，托管金额已划转')
}
```

- [ ] **Step 5: Build frontend**

```bash
npm --prefix frontend run build
```

Expected: PASS with known warnings only.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/campushub.ts frontend/src/views/AdminWalletView.vue frontend/src/views/GoodsDetailView.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "add wallet operations and escrow UI"
```

## Task 9: Update docs and handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Test: none

- [ ] **Step 1: Update README Phase 9 section**

Add this section to `README.md`:

```markdown
## Phase 9：钱包账本、充值提现与线上托管基础

Phase 9 将 CampusHub 钱包从展示型账户升级为可支撑余额充值、人工提现、冻结余额和二手线上托管交易的资金闭环。真实支付渠道仍由 API-Transfer-Station 支付中心负责；CampusHub 只保存本地钱包账本、充值/提现/托管业务记录和支付订单映射，不读取或保存支付宝密钥。

新增能力：

- 钱包可用余额、冻结余额和幂等账本流水；
- 支付宝充值收取 0.6% 渠道手续费并通过支付中心回调入账；
- 微信充值免手续费但需要管理员人工审核；
- 提现申请在提交时冻结余额，管理员审核、完成或拒绝；
- 二手线上托管交易先冻结买方余额，确认完成后划转给卖方；
- 线下交易金额小于 50 元免服务费，50 元及以上收 1% 且封顶 2 元；
- 线上托管服务费收 1% 且封顶 3 元；
- 新增用户钱包中心和管理端钱包运营工作台。

Phase 9 不做自动提现打款、完整财务清结算、API-Transfer-Station 内部改造、跑腿/店铺全量托管深接入或 CampusHub 直连支付宝密钥。
```

- [ ] **Step 2: Update CLAUDE handoff draft**

Append to `CLAUDE.md` after Phase 8 handoff:

```markdown
## Latest Phase 9 implementation and verification handoff, 2026-05-23

Latest Phase 9 work adds wallet ledger, recharge/withdrawal operations, goods online escrow, and wallet operations UI.

Implemented Phase 9:

- New docs: `docs/superpowers/specs/2026-05-23-campushub-phase9-wallet-escrow-design.md` and `docs/superpowers/plans/2026-05-23-campushub-phase9-wallet-escrow-upgrade.md`.
- `V12__wallet_escrow_upgrade.sql` extends wallet accounts/flows, adds recharge orders, withdrawal requests, frozen records, and goods escrow fields.
- Backend wallet package now centralizes balance mutations in `WalletService` and fee rules in `FeePolicyService`.
- Recharge supports Alipay payment-center callback入账 with 0.6% channel fee and WeChat manual review.
- Withdrawals freeze balance on submit and support admin approve/complete/reject.
- Goods online escrow freezes buyer balance, releases principal to seller, charges capped online service fee, and supports cancellation/dispute state.
- Frontend `/wallet`, `/admin/wallet`, and goods detail pages expose the new wallet and escrow flows.

Important constraints remain:

- Never read, print, copy, or commit real `.env`, SMTP password, JWT secret, payment token, or Alipay key contents.
- Production payment continues through API-Transfer-Station; CampusHub must not read or store Alipay key bodies.
- Do not edit already-applied migrations V1-V12; add V13+ only for future schema changes.
- Deploy carefully on the small shared server with low-frequency checks and targeted rebuilds.
- Avoid PowerShell under CC Switch + Codex Provider.
- Prefer server-side Docker build/API smoke/Playwriter for full verification; do not install local dependencies unless explicitly approved.
```

After verification, replace the generic header with exact commit/build/API/Playwriter results.

- [ ] **Step 3: Commit docs**

```bash
git add README.md CLAUDE.md docs/superpowers/specs/2026-05-23-campushub-phase9-wallet-escrow-design.md docs/superpowers/plans/2026-05-23-campushub-phase9-wallet-escrow-upgrade.md
git commit -m "document phase 9 wallet escrow design"
```

## Task 10: Server verification and deployment checkpoint

**Files:**
- Modify: `CLAUDE.md` only if verification results need exact handoff updates.

- [ ] **Step 1: Check git status**

```bash
git status --short
```

Expected: only intentional untracked local backups and no unstaged implementation changes.

- [ ] **Step 2: Backend build verification**

Preferred because local Maven may be unavailable and local dependency installation is not desired: push committed branch/master, then run one low-impact server build:

```bash
docker compose -f docker-compose.prod.yml build campushub-backend
```

Expected: Maven package inside Docker ends with `BUILD SUCCESS`. Do not print `.env` or any secret values.

- [ ] **Step 3: Frontend build verification**

```bash
docker compose -f docker-compose.prod.yml build campushub-web
```

Expected: Vite build succeeds. Known large chunk and dependency pure-comment warnings are acceptable.

- [ ] **Step 4: Deploy with minimal restart**

After successful builds and explicit approval for production deployment:

```bash
docker compose -f docker-compose.prod.yml up -d campushub-backend campushub-web
```

Expected: backend and web containers restart; MySQL remains healthy. Avoid repeated restarts.

- [ ] **Step 5: API smoke**

Run server-local non-secret checks:

```bash
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/wallet/users/1
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/wallet/users/1/flows
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/wallet/recharges
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/wallet/withdrawals
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/wallet/frozen-records
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/payment/orders
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/goods
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/tasks
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/shops
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/project-ads
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18080/api/admin/ops/analytics/overview
```

Expected: all return HTTP 200. If any endpoint returns 500, inspect application logs without printing secrets.

- [ ] **Step 6: Playwriter desktop verification**

Use Playwriter against `https://ustc.suntomb.qzz.io`:

- `/wallet` loads balance, recharge, withdrawal, frozen items, and flow sections without white screen.
- `/admin/wallet` loads recharge and withdrawal tabs without white screen.
- `/admin/payment` still loads Phase 8 payment orders/callback tabs.
- `/goods` and a goods detail page render online/offline transaction messaging.
- `/admin/ops`, `/admin/governance`, `/tasks`, `/shops`, and `/project-ads` still render.

Expected: no visible Element Plus fatal errors or blank screens.

- [ ] **Step 7: Playwriter mobile verification**

Set viewport 390x844 and verify:

- `/wallet` has no document-level horizontal overflow.
- `/admin/wallet` has no document-level horizontal overflow.
- Recharge and withdrawal dialogs fit narrow screen and can be closed.

Expected: `document.documentElement.scrollWidth <= document.documentElement.clientWidth` for key pages, or only intentional table-level scroll containers.

- [ ] **Step 8: Update CLAUDE handoff with actual verification**

Edit the Phase 9 section in `CLAUDE.md` with exact commit hash, build result, API smoke endpoint list, and Playwriter routes. Preserve the secret-handling constraints.

- [ ] **Step 9: Final handoff commit**

```bash
git add CLAUDE.md
git commit -m "document phase 9 deployment handoff"
```

## Self-review

- Spec coverage: Tasks cover V12 schema, centralized wallet ledger, fee policy, Alipay/WeChat recharge, withdrawal freeze/review/complete, goods online escrow, offline/online fee rules, admin wallet operations, user wallet UI, docs, server Docker verification, API smoke, and Playwriter checks.
- Scope control: Although Phase 9 is large, implementation is limited to one wallet subsystem plus first escrow attachment to goods orders. Runner and shop escrow deep integration remain out of scope.
- Placeholder scan: The plan avoids TBD/TODO/FIXME and defines concrete file paths, method names, endpoints, expected statuses, test commands, and commit points.
- Type consistency: `WalletService`, `FeePolicyService`, `WalletOperationService`, `WalletRechargeSummary`, `WalletWithdrawalSummary`, `WalletFrozenRecordSummary`, and goods escrow method names are used consistently across tasks.
- Safety: The plan keeps CampusHub away from direct payment-channel secrets, uses V12 instead of editing V1-V11, and reserves production verification for low-impact server Docker/API/Playwriter steps.
