package com.campushub.goods;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "goods_intents")
public class GoodsIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    private String message;

    @Column(name = "contact_snapshot", nullable = false)
    private String contactSnapshot;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_fee_id")
    private ServiceFeeRecord serviceFee;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GoodsIntent() {
    }

    public GoodsIntent(Goods goods, User buyer, String message, String contactSnapshot) {
        this.goods = goods;
        this.buyer = buyer;
        this.seller = goods.getSeller();
        this.message = message;
        this.contactSnapshot = contactSnapshot;
        this.status = "OPEN";
    }

    public Long getId() { return id; }
    public Goods getGoods() { return goods; }
    public User getBuyer() { return buyer; }
    public User getSeller() { return seller; }
    public String getMessage() { return message; }
    public String getContactSnapshot() { return contactSnapshot; }
    public String getStatus() { return status; }
    public ServiceFeeRecord getServiceFee() { return serviceFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void attachServiceFee(ServiceFeeRecord serviceFee) {
        this.serviceFee = serviceFee;
    }

    public void complete() {
        this.status = "COMPLETED";
    }
}
