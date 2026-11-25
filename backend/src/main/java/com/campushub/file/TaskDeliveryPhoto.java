package com.campushub.file;

import com.campushub.task.RewardTask;
import com.campushub.task.TaskApplication;
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
@Table(name = "task_delivery_photos")
public class TaskDeliveryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private RewardTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_application_id")
    private TaskApplication acceptedApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "evidence_type", nullable = false)
    private String evidenceType;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "client_request_id", nullable = false, length = 128)
    private String clientRequestId;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TaskDeliveryPhoto() {
    }

    public TaskDeliveryPhoto(RewardTask task, TaskApplication acceptedApplication, User uploader,
            StoredTaskEvidence stored, String evidenceType, String clientRequestId, String note) {
        this.task = task;
        this.evidenceType = evidenceType;
        this.createdAt = LocalDateTime.now();
        this.acceptedApplication = acceptedApplication;
        this.uploader = uploader;
        this.storageKey = stored.storageKey();
        this.originalName = stored.originalName();
        this.contentType = stored.contentType();
        this.sizeBytes = stored.sizeBytes();
        this.sha256 = stored.sha256();
        this.clientRequestId = clientRequestId;
        this.note = note;
        this.status = "UPLOADED";
    }

    public String getEvidenceType() { return evidenceType; }
    public void markDeleted() { status = "DELETED"; }
    public Long getId() { return id; }
    public RewardTask getTask() { return task; }
    public TaskApplication getAcceptedApplication() { return acceptedApplication; }
    public User getUploader() { return uploader; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getClientRequestId() { return clientRequestId; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
