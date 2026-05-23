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

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getProviderOrderNo() {
        return providerOrderNo;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isHandled() {
        return handled;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
