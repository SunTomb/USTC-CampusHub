package com.campushub.shop;

import com.campushub.payment.ServiceFeeRecord;
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
import java.util.UUID;

@Entity
@Table(name = "service_orders")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_item_id", nullable = false)
    private ServiceItem serviceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "service_fee", nullable = false)
    private BigDecimal serviceFee;

    @Column(nullable = false)
    private String status;

    private String note;

    @Column(name = "contact_snapshot")
    private String contactSnapshot;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_fee_id")
    private ServiceFeeRecord serviceFeeRecord;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceOrder() {
    }

    public ServiceOrder(ServiceItem serviceItem, User customer, CreateServiceOrderRequest request, String contactSnapshot) {
        this.orderNo = "SO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
        this.serviceItem = serviceItem;
        this.customer = customer;
        this.provider = serviceItem.getShop().getOwner();
        this.appointmentTime = request.appointmentTime();
        this.amount = request.amount() == null ? serviceItem.getPrice() : request.amount();
        this.serviceFee = BigDecimal.ZERO;
        this.status = "REQUESTED";
        this.note = request.note();
        this.contactSnapshot = contactSnapshot;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public User getCustomer() {
        return customer;
    }

    public User getProvider() {
        return provider;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public String getContactSnapshot() {
        return contactSnapshot;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public ServiceFeeRecord getServiceFeeRecord() {
        return serviceFeeRecord;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void accept() {
        this.status = "ACCEPTED";
    }

    public void reject(String cancelReason) {
        this.status = "REJECTED";
        this.cancelReason = cancelReason;
        this.canceledAt = LocalDateTime.now();
    }

    public void start() {
        this.status = "IN_SERVICE";
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason) {
        this.status = "CANCELED";
        this.cancelReason = cancelReason;
        this.canceledAt = LocalDateTime.now();
    }
}
