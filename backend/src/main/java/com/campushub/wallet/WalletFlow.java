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

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "business_id")
    private Long businessId;

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
        this.walletAccount = walletAccount;
        this.user = user;
        this.flowNo = flowNo;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.businessType = businessType;
        this.businessId = businessId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getBusinessType() {
        return businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
