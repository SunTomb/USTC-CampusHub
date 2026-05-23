package com.campushub.goods;

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
@Table(name = "goods_orders")
public class GoodsOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "service_fee", nullable = false)
    private BigDecimal serviceFee;

    @Column(name = "trade_mode", nullable = false)
    private String tradeMode;

    @Column(name = "escrow_status", nullable = false)
    private String escrowStatus;

    @Column(name = "escrow_amount", nullable = false)
    private BigDecimal escrowAmount;

    @Column(name = "platform_service_fee", nullable = false)
    private BigDecimal platformServiceFee;

    @Column(nullable = false)
    private String status;

    @Column(name = "contact_snapshot", nullable = false)
    private String contactSnapshot;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

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

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Goods getGoods() {
        return goods;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public String getTradeMode() {
        return tradeMode;
    }

    public String getEscrowStatus() {
        return escrowStatus;
    }

    public BigDecimal getEscrowAmount() {
        return escrowAmount;
    }

    public BigDecimal getPlatformServiceFee() {
        return platformServiceFee;
    }

    public String getStatus() {
        return status;
    }

    public String getContactSnapshot() {
        return contactSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public LocalDateTime getEscrowFrozenAt() {
        return escrowFrozenAt;
    }

    public LocalDateTime getEscrowReleasedAt() {
        return escrowReleasedAt;
    }

    public LocalDateTime getEscrowUnfrozenAt() {
        return escrowUnfrozenAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public void enableOnlineEscrow(BigDecimal escrowAmount, BigDecimal platformServiceFee) {
        if (escrowAmount == null || escrowAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("担保交易金额必须大于 0");
        }
        this.tradeMode = "ONLINE_ESCROW";
        this.escrowStatus = "PENDING_FREEZE";
        this.escrowAmount = escrowAmount;
        this.platformServiceFee = platformServiceFee == null ? BigDecimal.ZERO : platformServiceFee;
        this.cancelReason = null;
        this.disputeReason = null;
    }

    public void markEscrowFrozen() {
        this.escrowStatus = "FROZEN";
        this.escrowFrozenAt = LocalDateTime.now();
        this.paidAt = this.escrowFrozenAt;
    }

    public void markEscrowReleased() {
        this.escrowStatus = "RELEASED";
        this.escrowReleasedAt = LocalDateTime.now();
        this.completedAt = this.escrowReleasedAt;
        this.status = "COMPLETED";
    }

    public void markEscrowCanceled(String reason) {
        this.escrowStatus = "CANCELED";
        this.escrowUnfrozenAt = LocalDateTime.now();
        this.canceledAt = this.escrowUnfrozenAt;
        this.cancelReason = reason;
        this.status = "CANCELED";
    }

    public void markEscrowDisputed(String reason) {
        this.escrowStatus = "DISPUTED";
        this.disputeReason = reason;
    }
}
