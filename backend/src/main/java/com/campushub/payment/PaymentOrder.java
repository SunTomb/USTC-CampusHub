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

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getBusinessType() {
        return businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public User getPayer() {
        return payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderOrderNo() {
        return providerOrderNo;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

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
