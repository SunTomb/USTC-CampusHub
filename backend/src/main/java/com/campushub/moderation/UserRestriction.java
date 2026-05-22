package com.campushub.moderation;

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
@Table(name = "user_restrictions")
public class UserRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id")
    private ViolationRecord violation;

    @Column(name = "restriction_type", nullable = false)
    private String restrictionType;

    @Column(nullable = false)
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserRestriction() {
    }

    public UserRestriction(User user, ViolationRecord violation, String restrictionType, String reason, LocalDateTime startsAt, LocalDateTime endsAt, User admin) {
        this.user = user;
        this.violation = violation;
        this.restrictionType = restrictionType;
        this.reason = reason;
        this.startsAt = startsAt == null ? LocalDateTime.now() : startsAt;
        this.endsAt = endsAt;
        this.active = true;
        this.admin = admin;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ViolationRecord getViolation() {
        return violation;
    }

    public String getRestrictionType() {
        return restrictionType;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public Boolean getActive() {
        return active;
    }

    public User getAdmin() {
        return admin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
