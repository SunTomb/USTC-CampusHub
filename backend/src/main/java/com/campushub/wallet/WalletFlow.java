package com.campushub.wallet;

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
@Table(name = "wallet_flows")
public class WalletFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_account_id", nullable = false)
    private WalletAccount walletAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "flow_no", nullable = false)
    private String flowNo;

    @Column(nullable = false)
    private String direction;

    @Column(name = "flow_type", nullable = false)
    private String flowType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "available_balance_after", nullable = false)
    private BigDecimal availableBalanceAfter;

    @Column(name = "frozen_balance_after", nullable = false)
    private BigDecimal frozenBalanceAfter;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_user_id")
    private User counterpartyUser;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    private String remark;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected WalletFlow() {
    }

    public WalletFlow(
            WalletAccount walletAccount,
            User user,
            String flowNo,
            String direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String businessType,
            Long businessId,
            String remark) {
        this(
                walletAccount,
                user,
                flowNo,
                direction,
                "LEGACY",
                amount,
                balanceAfter,
                walletAccount.getFrozenBalance(),
                businessType,
                businessId,
                null,
                null,
                "SYSTEM",
                null,
                remark);
    }

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
            User counterpartyUser,
            String createdBy,
            User operator,
            String remark) {
        this.walletAccount = walletAccount;
        this.user = user;
        this.flowNo = flowNo;
        this.direction = direction;
        this.flowType = flowType == null ? "LEGACY" : flowType;
        this.amount = amount;
        this.balanceAfter = availableBalanceAfter;
        this.availableBalanceAfter = availableBalanceAfter;
        this.frozenBalanceAfter = frozenBalanceAfter == null ? BigDecimal.ZERO : frozenBalanceAfter;
        this.businessType = businessType;
        this.businessId = businessId;
        this.idempotencyKey = idempotencyKey;
        this.counterpartyUser = counterpartyUser;
        this.createdBy = createdBy == null ? "SYSTEM" : createdBy;
        this.operator = operator;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public WalletAccount getWalletAccount() {
        return walletAccount;
    }

    public User getUser() {
        return user;
    }

    public String getFlowNo() {
        return flowNo;
    }

    public String getDirection() {
        return direction;
    }

    public String getFlowType() {
        return flowType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getAvailableBalanceAfter() {
        return availableBalanceAfter;
    }

    public BigDecimal getFrozenBalanceAfter() {
        return frozenBalanceAfter;
    }

    public String getBusinessType() {
        return businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public User getCounterpartyUser() {
        return counterpartyUser;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public User getOperator() {
        return operator;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
