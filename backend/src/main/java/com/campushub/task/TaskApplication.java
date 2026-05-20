package com.campushub.task;

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
@Table(name = "task_applications")
public class TaskApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private RewardTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    private String message;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected TaskApplication() {
    }

    public TaskApplication(RewardTask task, User applicant, String message) {
        this.task = task;
        this.applicant = applicant;
        this.message = message;
        this.status = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public RewardTask getTask() {
        return task;
    }

    public User getApplicant() {
        return applicant;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void markAccepted() {
        this.status = "ACCEPTED";
        this.acceptedAt = LocalDateTime.now();
    }

    public void markRejected() {
        this.status = "REJECTED";
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
}
