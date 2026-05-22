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
@Table(name = "credit_adjustment_records")
public class CreditAdjustmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id")
    private ViolationRecord violation;

    @Column(name = "before_score", nullable = false)
    private Integer beforeScore;

    @Column(name = "delta_score", nullable = false)
    private Integer deltaScore;

    @Column(name = "after_score", nullable = false)
    private Integer afterScore;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CreditAdjustmentRecord() {
    }

    public CreditAdjustmentRecord(User user, ViolationRecord violation, Integer beforeScore, Integer deltaScore, Integer afterScore, String reason, User admin) {
        this.user = user;
        this.violation = violation;
        this.beforeScore = beforeScore;
        this.deltaScore = deltaScore;
        this.afterScore = afterScore;
        this.reason = reason;
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

    public Integer getBeforeScore() {
        return beforeScore;
    }

    public Integer getDeltaScore() {
        return deltaScore;
    }

    public Integer getAfterScore() {
        return afterScore;
    }

    public String getReason() {
        return reason;
    }

    public User getAdmin() {
        return admin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
