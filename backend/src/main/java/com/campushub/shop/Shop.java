package com.campushub.shop;

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
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_area", nullable = false)
    private String serviceArea;

    @Column(name = "campus_zone", nullable = false)
    private String campusZone;

    @Column(name = "contact_visibility", nullable = false)
    private String contactVisibility;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "cover_file_id")
    private Long coverFileId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal rating;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Shop() {
    }

    public Shop(User owner, CreateShopRequest request) {
        this.owner = owner;
        this.name = request.name().trim();
        this.description = request.description().trim();
        this.serviceArea = request.serviceArea().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.openingHours = trimToNull(request.openingHours());
        this.coverFileId = request.coverFileId();
        this.status = "APPROVED";
        this.rating = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public String getCampusZone() {
        return campusZone;
    }

    public String getContactVisibility() {
        return contactVisibility;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(UpdateShopRequest request) {
        this.name = request.name().trim();
        this.description = request.description().trim();
        this.serviceArea = request.serviceArea().trim();
        this.campusZone = request.campusZone().trim();
        this.contactVisibility = request.contactVisibility().trim();
        this.openingHours = trimToNull(request.openingHours());
        this.coverFileId = request.coverFileId();
    }

    public void pause() {
        this.status = "PAUSED";
    }

    public void resume() {
        this.status = "APPROVED";
    }

    public void close() {
        this.status = "CLOSED";
    }

    public void block() {
        this.status = "BLOCKED";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
