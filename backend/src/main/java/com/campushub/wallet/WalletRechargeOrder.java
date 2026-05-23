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
@Table(name = "wallet_recharge_orders")
public class WalletRechargeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recharge_no", nullable = false)
    private String rechargeNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected WalletRechargeOrder() {
    }

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
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void attachPaymentOrder(String paymentOrderNo) {
        this.paymentOrderNo = paymentOrderNo;
    }

    public void markPaid() {
        this.status = "PAID";
    }

    public void approve(User reviewer, String note) {
        this.status = "PAID";
        this.reviewer = reviewer;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User reviewer, String note) {
        this.status = "REJECTED";
        this.reviewer = reviewer;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }
}
