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
    private String description;

    @Column(name = "credit_delta", nullable = false)
    private Integer creditDelta;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public String getDescription() {
        return description;
    }

    public Integer getCreditDelta() {
        return creditDelta;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
