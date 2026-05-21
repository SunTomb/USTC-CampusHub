package com.campushub.shop;

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
@Table(name = "service_items")
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "min_price")
    private BigDecimal minPrice;

    @Column(name = "max_price")
    private BigDecimal maxPrice;

    @Column(name = "price_unit", nullable = false)
    private String priceUnit;

    @Column(name = "cover_file_id")
    private Long coverFileId;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceItem() {
    }

    public ServiceItem(Shop shop, CreateServiceItemRequest request) {
        this.shop = shop;
        this.category = request.category().trim();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.minPrice = request.minPrice();
        this.maxPrice = request.maxPrice();
        this.priceUnit = request.priceUnit().trim();
        this.coverFileId = request.coverFileId();
        this.durationMinutes = request.durationMinutes();
        this.status = "PUBLISHED";
    }

    public Long getId() {
        return id;
    }

    public Shop getShop() {
        return shop;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(UpdateServiceItemRequest request) {
        this.category = request.category().trim();
        this.title = request.title().trim();
        this.description = request.description().trim();
        this.price = request.price();
        this.minPrice = request.minPrice();
        this.maxPrice = request.maxPrice();
        this.priceUnit = request.priceUnit().trim();
        this.coverFileId = request.coverFileId();
        this.durationMinutes = request.durationMinutes();
    }

    public void publish() {
        this.status = "PUBLISHED";
    }

    public void pause() {
        this.status = "PAUSED";
    }

    public void offShelf() {
        this.status = "OFF_SHELF";
    }
}
