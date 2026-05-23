package com.campushub.wallet;

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
@Table(name = "wallet_withdrawal_requests")
public class WalletWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdrawal_no", nullable = false)
    private String withdrawalNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String channel;

    @Column(name = "account_snapshot")
    private String accountSnapshot;

    @Column(nullable = false)
    private String status;

    @Column(name = "review_note")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected WalletWithdrawalRequest() {
    }

    public WalletWithdrawalRequest(String withdrawalNo, User user, BigDecimal amount, String channel, String accountSnapshot) {
        this.withdrawalNo = withdrawalNo;
        this.user = user;
        this.amount = amount;
        this.channel = channel;
        this.accountSnapshot = accountSnapshot;
        this.status = "PENDING_REVIEW";
    }

    public Long getId() { return id; }
    public String getWithdrawalNo() { return withdrawalNo; }
    public User getUser() { return user; }
    public BigDecimal getAmount() { return amount; }
    public String getChannel() { return channel; }
    public String getAccountSnapshot() { return accountSnapshot; }
    public String getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public User getReviewer() { return reviewer; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void approve(User reviewer, String note) {
        this.status = "APPROVED";
        this.reviewer = reviewer;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User reviewer, String note) {
        this.status = "REJECTED";
        this.reviewer = reviewer;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }

    public void complete(User reviewer, String note) {
        this.status = "COMPLETED";
        this.reviewer = reviewer;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
}
