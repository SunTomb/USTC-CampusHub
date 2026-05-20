package com.campushub.identity;

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
@Table(name = "role_applications")
public class RoleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "deposit_status", nullable = false)
    private String depositStatus;

    @Column(name = "review_status", nullable = false)
    private String reviewStatus;

    @Column(name = "apply_note")
    private String applyNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected RoleApplication() {
    }

    public RoleApplication(User user, PlatformRoleType roleType, String applyNote) {
        this.user = user;
        this.roleType = roleType.name();
        this.depositAmount = roleType.depositAmount();
        this.depositStatus = "PAID";
        this.reviewStatus = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "APPROVED";
        this.applyNote = applyNote;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRoleType() {
        return roleType;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public String getDepositStatus() {
        return depositStatus;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getApplyNote() {
        return applyNote;
    }

    public User getReviewer() {
        return reviewer;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markApproved(User reviewer) {
        this.reviewStatus = "APPROVED";
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void markRejected(User reviewer) {
        this.reviewStatus = "REJECTED";
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
}
