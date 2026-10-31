package com.campushub.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_agreement_versions")
public class PlatformProtocolVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agreement_code", nullable = false)
    private String agreementCode;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR)
    private String content;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PlatformProtocolVersion() {
    }

    public Long getId() {
        return id;
    }

    public String getAgreementCode() {
        return agreementCode;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public String getStatus() {
        return status;
    }
}
