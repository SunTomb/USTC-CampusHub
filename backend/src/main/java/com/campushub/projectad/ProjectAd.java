package com.campushub.projectad;

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
@Table(name = "project_ads")
public class ProjectAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;

    @Column(nullable = false)
    private String title;

    @Column(name = "ad_type", nullable = false)
    private String adType;

    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String tags;

    @Column(name = "campus_zone")
    private String campusZone;

    @Column(name = "cover_file_id")
    private Long coverFileId;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "contact_info", nullable = false)
    private String contactInfo;

    @Column(name = "contact_visibility", nullable = false)
    private String contactVisibility;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean featured;

    @Column(name = "featured_priority", nullable = false)
    private Integer featuredPriority;

    @Column(name = "review_note")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ProjectAd() {
    }

    public ProjectAd(User publisher, ProjectAdRequest request) {
        this.publisher = publisher;
        this.viewCount = 0;
        this.featured = false;
        this.featuredPriority = 0;
        this.status = "PENDING_REVIEW";
        apply(request);
    }

    public void update(ProjectAdRequest request) {
        apply(request);
        if ("APPROVED".equals(status)) {
            this.status = "PENDING_REVIEW";
            this.reviewNote = null;
            this.reviewedBy = null;
            this.reviewedAt = null;
            this.publishedAt = null;
            unfeature();
        }
    }

    public void submit() {
        if (!"CLOSED".equals(status) && !"BLOCKED".equals(status)) {
            this.status = "PENDING_REVIEW";
        }
    }

    public void approve(User reviewer, String note) {
        LocalDateTime now = LocalDateTime.now();
        this.status = "APPROVED";
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.reviewNote = trimToNull(note);
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public void reject(User reviewer, String note) {
        this.status = "REJECTED";
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNote = trimToNull(note);
        unfeature();
    }

    public void feature(int priority) {
        this.featured = true;
        this.featuredPriority = priority;
    }

    public void unfeature() {
        this.featured = false;
        this.featuredPriority = 0;
    }

    public void closeByPublisher() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
        unfeature();
    }

    public void block(User reviewer, String note) {
        LocalDateTime now = LocalDateTime.now();
        this.status = "BLOCKED";
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.reviewNote = trimToNull(note);
        this.closedAt = now;
        unfeature();
    }

    public void increaseViewCount() {
        this.viewCount = this.viewCount == null ? 1 : this.viewCount + 1;
    }

    public boolean isPubliclyVisible(LocalDateTime now) {
        return "APPROVED".equals(status) && (expiresAt == null || expiresAt.isAfter(now));
    }

    private void apply(ProjectAdRequest request) {
        this.title = request.title().trim();
        this.adType = normalize(request.adType(), "OTHER");
        this.summary = trimToNull(request.summary());
        this.description = request.description().trim();
        this.tags = trimToNull(request.tags());
        this.campusZone = trimToNull(request.campusZone());
        this.coverFileId = request.coverFileId();
        this.linkUrl = trimToNull(request.linkUrl());
        this.contactInfo = request.contactInfo().trim();
        this.contactVisibility = normalize(request.contactVisibility(), "LOGIN_ONLY");
        this.expiresAt = request.expiresAt();
    }

    private String normalize(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    public String getAdType() {
        return adType;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String getCampusZone() {
        return campusZone;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public String getContactVisibility() {
        return contactVisibility;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public Integer getFeaturedPriority() {
        return featuredPriority;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getStatus() {
        return status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
