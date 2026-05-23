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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected WalletAccount() {
    }

    public WalletAccount(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean hasPositiveBalance() {
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasEnoughBalance(BigDecimal amount) {
        return balance.compareTo(normalizeAmount(amount)) >= 0;
    }

    public void credit(BigDecimal amount) {
        requireActive();
        BigDecimal normalized = normalizePositiveAmount(amount);
        this.balance = this.balance.add(normalized);
        touch();
    }

    public void debit(BigDecimal amount) {
        requireActive();
        BigDecimal normalized = normalizePositiveAmount(amount);
        if (!hasEnoughBalance(normalized)) {
            throw new BusinessException("钱包可用余额不足");
        }
        this.balance = this.balance.subtract(normalized);
        touch();
    }

    public void freeze(BigDecimal amount) {
        requireActive();
        BigDecimal normalized = normalizePositiveAmount(amount);
        if (!hasEnoughBalance(normalized)) {
            throw new BusinessException("钱包可用余额不足，无法冻结");
        }
        this.balance = this.balance.subtract(normalized);
        this.frozenBalance = this.frozenBalance.add(normalized);
        touch();
    }

    public void unfreeze(BigDecimal amount) {
        requireActive();
        BigDecimal normalized = normalizePositiveAmount(amount);
        requireEnoughFrozenBalance(normalized);
        this.frozenBalance = this.frozenBalance.subtract(normalized);
        this.balance = this.balance.add(normalized);
        touch();
    }

    public void debitFrozen(BigDecimal amount) {
        requireActive();
        BigDecimal normalized = normalizePositiveAmount(amount);
        requireEnoughFrozenBalance(normalized);
        this.frozenBalance = this.frozenBalance.subtract(normalized);
        touch();
    }

    private void requireActive() {
        if (!isActive()) {
            throw new BusinessException("钱包账户不可用");
        }
    }

    private void requireEnoughFrozenBalance(BigDecimal amount) {
        if (frozenBalance.compareTo(amount) < 0) {
            throw new BusinessException("钱包冻结余额不足");
        }
    }

    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = normalizeAmount(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("钱包金额必须大于 0");
        }
        return normalized;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException("钱包金额不能为空");
        }
        return amount;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
