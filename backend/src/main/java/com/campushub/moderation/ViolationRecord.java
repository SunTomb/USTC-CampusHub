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
@Table(name = "violation_records")
public class ViolationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private ReportRecord report;

    @Column(name = "violation_type", nullable = false)
    private String violationType;

    @Column(nullable = false)
    private String severity;

    @Column(name = "penalty_type", nullable = false)
    private String penaltyType;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(nullable = false)
    private String description;

    @Column(name = "credit_delta", nullable = false)
    private Integer creditDelta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "deposit_impact_note")
    private String depositImpactNote;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ViolationRecord() {
    }

    public ViolationRecord(
            User user,
            ReportRecord report,
            String targetType,
            Long targetId,
            String violationType,
            String severity,
            String penaltyType,
            String description,
            Integer creditDelta,
            User admin,
            String depositImpactNote) {
        this.user = user;
        this.report = report;
        this.targetType = targetType;
        this.targetId = targetId;
        this.violationType = violationType;
        this.severity = severity;
        this.penaltyType = penaltyType;
        this.description = description;
        this.creditDelta = creditDelta;
        this.admin = admin;
        this.depositImpactNote = depositImpactNote;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ReportRecord getReport() {
        return report;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getPenaltyType() {
        return penaltyType;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCreditDelta() {
        return creditDelta;
    }

    public User getAdmin() {
        return admin;
    }

    public String getDepositImpactNote() {
        return depositImpactNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
