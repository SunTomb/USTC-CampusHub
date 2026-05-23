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
@Table(name = "wallet_frozen_records")
public class WalletFrozenRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freeze_no", nullable = false)
    private String freezeNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "frozen_at", insertable = false, updatable = false)
    private LocalDateTime frozenAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    private String remark;

    protected WalletFrozenRecord() {
    }

    public WalletFrozenRecord(String freezeNo, User user, String businessType, Long businessId, BigDecimal amount, String remark) {
        this.freezeNo = freezeNo;
        this.user = user;
        this.businessType = businessType;
        this.businessId = businessId;
        this.amount = amount;
        this.status = "FROZEN";
        this.remark = remark;
    }

    public Long getId() { return id; }
    public String getFreezeNo() { return freezeNo; }
    public User getUser() { return user; }
    public String getBusinessType() { return businessType; }
    public Long getBusinessId() { return businessId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getFrozenAt() { return frozenAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public String getRemark() { return remark; }

    public void markReleased() {
        this.status = "RELEASED";
        this.releasedAt = LocalDateTime.now();
    }

    public void markUnfrozen() {
        this.status = "UNFROZEN";
        this.releasedAt = LocalDateTime.now();
    }
}
