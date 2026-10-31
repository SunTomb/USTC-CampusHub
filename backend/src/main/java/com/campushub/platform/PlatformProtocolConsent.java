package com.campushub.platform;

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
@Table(name = "role_agreement_consents")
public class PlatformProtocolConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private PlatformProtocolVersion agreementVersion;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(nullable = false)
    private String source;

    protected PlatformProtocolConsent() {
    }

    public PlatformProtocolConsent(User user, String roleType, PlatformProtocolVersion agreementVersion, String source) {
        this.user = user;
        this.roleType = roleType;
        this.agreementVersion = agreementVersion;
        this.acceptedAt = LocalDateTime.now();
        this.source = source;
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

    public PlatformProtocolVersion getAgreementVersion() {
        return agreementVersion;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getSource() {
        return source;
    }
}
