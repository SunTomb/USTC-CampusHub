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
@Table(name = "service_fee_records")
public class ServiceFeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_no", nullable = false)
    private String feeNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "payment_order_no")
    private String paymentOrderNo;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "payment_center_order_no")
    private String paymentCenterOrderNo;

    @Column(name = "pay_url", length = 4096)
    private String payUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceFeeRecord() {
    }

    public ServiceFeeRecord(String feeNo, User payer, String targetType, Long targetId, BigDecimal amount) {
        this.feeNo = feeNo;
        this.payer = payer;
        this.targetType = targetType;
        this.targetId = targetId;
        this.amount = amount;
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public String getFeeNo() {
        return feeNo;
    }

    public User getPayer() {
        return payer;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
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

    public void markPaid(LocalDateTime paidAt) {
        this.status = "PAID";
        this.paidAt = paidAt;
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
