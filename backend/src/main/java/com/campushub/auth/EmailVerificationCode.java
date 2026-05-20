package com.campushub.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    protected EmailVerificationCode() {
    }

    public EmailVerificationCode(String email, String codeHash, String purpose, Instant expiresAt, Instant lastSentAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
        this.attemptCount = 0;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void increaseAttemptCount() {
        this.attemptCount = this.attemptCount + 1;
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
