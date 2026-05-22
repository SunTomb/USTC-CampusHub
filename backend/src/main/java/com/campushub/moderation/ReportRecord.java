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
@Table(name = "report_records")
public class ReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private String reason;

    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "resolution_type")
    private String resolutionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    private User handler;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ReportRecord() {
    }

    public ReportRecord(User reporter, String targetType, Long targetId, String reason, String description) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.description = description;
        this.status = "OPEN";
    }

    public Long getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public String getResolutionType() {
        return resolutionType;
    }

    public User getHandler() {
        return handler;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void startReview(User handler, String note) {
        this.handler = handler;
        this.status = "IN_REVIEW";
        this.reviewNote = note;
    }

    public void reject(User handler, String note) {
        this.handler = handler;
        this.status = "REJECTED";
        this.reviewNote = note;
        this.resolutionType = "NO_ACTION";
        this.handledAt = LocalDateTime.now();
    }

    public void resolve(User handler, String resolutionType, String note) {
        this.handler = handler;
        this.status = "RESOLVED";
        this.resolutionType = resolutionType;
        this.reviewNote = note;
        this.handledAt = LocalDateTime.now();
    }

    public void escalate(User handler, String note) {
        this.handler = handler;
        this.status = "ESCALATED";
        this.resolutionType = "ESCALATED";
        this.reviewNote = note;
        this.handledAt = LocalDateTime.now();
    }
}
