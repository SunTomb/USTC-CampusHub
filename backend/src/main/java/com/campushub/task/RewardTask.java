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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_tasks")
public class RewardTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "reward_amount", nullable = false)
    private BigDecimal rewardAmount;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "task_location", nullable = false)
    private String taskLocation;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private String status;

    @Column(name = "acceptance_mode", nullable = false)
    private String acceptanceMode;

    @Column(name = "origin_zone", nullable = false)
    private String originZone;

    @Column(name = "destination_zone", nullable = false)
    private String destinationZone;

    @Column(name = "origin_detail")
    private String originDetail;

    @Column(name = "destination_detail")
    private String destinationDetail;

    @Column(name = "workflow_status", nullable = false)
    private String workflowStatus;

    @Column(name = "verification_mode", nullable = false)
    private String verificationMode;

    @Column(name = "completion_code_hash")
    private String completionCodeHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_application_id")
    private TaskApplication acceptedApplication;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RewardTask() {
    }

    public RewardTask(User publisher, String title, String description, BigDecimal rewardAmount, BigDecimal depositAmount, String taskLocation, LocalDateTime deadline, String status) {
        this.publisher = publisher;
        this.title = title;
        this.description = description;
        this.rewardAmount = rewardAmount;
        this.depositAmount = depositAmount;
        this.taskLocation = taskLocation;
        this.deadline = deadline;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public User getPublisher() {
        return publisher;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public String getTaskLocation() {
        return taskLocation;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getStatus() {
        return status;
    }

    public String getAcceptanceMode() {
        return acceptanceMode;
    }

    public String getOriginZone() {
        return originZone;
    }

    public String getDestinationZone() {
        return destinationZone;
    }

    public String getOriginDetail() {
        return originDetail;
    }

    public String getDestinationDetail() {
        return destinationDetail;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public String getVerificationMode() {
        return verificationMode;
    }

    public String getCompletionCodeHash() {
        return completionCodeHash;
    }

    public TaskApplication getAcceptedApplication() {
        return acceptedApplication;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void publishWorkflow(String acceptanceMode, String originZone, String destinationZone, String originDetail, String destinationDetail, String verificationMode) {
        this.acceptanceMode = acceptanceMode;
        this.originZone = originZone;
        this.destinationZone = destinationZone;
        this.originDetail = originDetail;
        this.destinationDetail = destinationDetail;
        this.verificationMode = verificationMode;
        this.workflowStatus = TaskWorkflowStatus.PUBLISHED.name();
        this.status = "PUBLISHED";
    }

    public void markAccepted(TaskApplication application) {
        this.acceptedApplication = application;
        this.workflowStatus = TaskWorkflowStatus.ACCEPTED.name();
        this.status = "ACCEPTED";
    }

    public void moveTo(String workflowStatus) {
        this.workflowStatus = workflowStatus;
        this.status = workflowStatus;
    }
}
