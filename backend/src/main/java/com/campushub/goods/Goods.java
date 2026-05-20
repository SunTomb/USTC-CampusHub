package com.campushub.goods;

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
@Table(name = "goods")
public class Goods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "condition_level", nullable = false)
    private String conditionLevel;

    @Column(name = "trade_location", nullable = false)
    private String tradeLocation;

    @Column(name = "campus_zone", nullable = false)
    private String campusZone;

    @Column(name = "contact_visibility", nullable = false)
    private String contactVisibility;

    @Column(name = "delivery_method", nullable = false)
    private String deliveryMethod;

    @Column(name = "service_fee_policy", nullable = false)
    private String serviceFeePolicy;

    @Column(nullable = false)
    private String status;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_to_user_id")
    private User soldToUser;

    protected Goods() {
    }

    public Goods(User seller, CreateGoodsRequest request) {
        this.seller = seller;
        this.categoryId = request.categoryId();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.originalPrice = request.originalPrice();
        this.conditionLevel = request.conditionLevel().trim();
        this.tradeLocation = request.tradeLocation().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.deliveryMethod = request.deliveryMethod().trim();
        this.serviceFeePolicy = "NONE";
        this.status = "PUBLISHED";
        this.viewCount = 0;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = this.publishedAt;
    }

    public Long getId() { return id; }
    public User getSeller() { return seller; }
    public Long getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getConditionLevel() { return conditionLevel; }
    public String getTradeLocation() { return tradeLocation; }
    public String getCampusZone() { return campusZone; }
    public String getContactVisibility() { return contactVisibility; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public String getServiceFeePolicy() { return serviceFeePolicy; }
    public String getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public User getSoldToUser() { return soldToUser; }

    public void update(UpdateGoodsRequest request) {
        this.categoryId = request.categoryId();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.originalPrice = request.originalPrice();
        this.conditionLevel = request.conditionLevel().trim();
        this.tradeLocation = request.tradeLocation().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.deliveryMethod = request.deliveryMethod().trim();
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount = this.viewCount + 1;
    }

    public void markSold(User buyer) {
        this.status = "SOLD";
        this.soldToUser = buyer;
        this.soldAt = LocalDateTime.now();
        this.updatedAt = this.soldAt;
    }

    public void offShelf() {
        this.status = "OFF_SHELF";
        this.updatedAt = LocalDateTime.now();
    }
}
