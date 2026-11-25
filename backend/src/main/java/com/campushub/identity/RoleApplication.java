package com.campushub.identity;

import com.campushub.platform.PlatformProtocolConsent;
import com.campushub.platform.PlatformProtocolVersion;
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
@Table(name = "role_applications")
public class RoleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @Column(name = "deposit_policy", nullable = false)
    private String depositPolicy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id")
    private PlatformProtocolVersion agreementVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_consent_id")
    private PlatformProtocolConsent agreementConsent;

    @Column(name = "agreement_status", nullable = false)
    private String agreementStatus;

    @Column(name = "agreement_consented_at")
    private LocalDateTime agreementConsentedAt;

    @Column(name = "activation_mode", nullable = false)
    private String activationMode;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "deposit_status", nullable = false)
    private String depositStatus;

    @Column(name = "deposit_payment_order_no")
    private String depositPaymentOrderNo;

    @Column(name = "review_status", nullable = false)
    private String reviewStatus;

    @Column(name = "apply_note")
    private String applyNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected RoleApplication() {
    }

    public RoleApplication(User user, PlatformRoleType roleType, String applyNote) {
        this.user = user;
        this.roleType = roleType.name();
        this.depositPolicy = "LEGACY_DEPOSIT";
        this.agreementStatus = "LEGACY";
        this.activationMode = "LEGACY_DEPOSIT";
        this.depositAmount = roleType.depositAmount();
        this.depositStatus = roleType.depositAmount().compareTo(BigDecimal.ZERO) <= 0 ? "NOT_REQUIRED" : "PENDING";
        this.reviewStatus = roleType.depositAmount().compareTo(BigDecimal.ZERO) <= 0 ? "PENDING_REVIEW" : "PENDING_PAYMENT";
        this.applyNote = applyNote;
    }

    public static RoleApplication agreementBased(
            User user,
            PlatformRoleType roleType,
            String applyNote,
            PlatformProtocolVersion agreementVersion,
            PlatformProtocolConsent agreementConsent) {
        RoleApplication application = new RoleApplication();
        application.user = user;
        application.roleType = roleType.name();
        application.depositPolicy = "NO_DEPOSIT";
        application.depositAmount = BigDecimal.ZERO;
        application.depositStatus = "NOT_REQUIRED";
        application.reviewStatus = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "APPROVED";
        application.applyNote = applyNote;
        application.agreementVersion = agreementVersion;
        application.agreementConsent = agreementConsent;
        application.agreementStatus = "CONSENTED";
        application.agreementConsentedAt = agreementConsent.getAcceptedAt();
        application.activationMode = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "AUTO_APPROVE";
        application.reviewedAt = null;
        return application;
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

    public String getDepositPolicy() {
        return depositPolicy;
    }

    public PlatformProtocolVersion getAgreementVersion() {
        return agreementVersion;
    }

    public PlatformProtocolConsent getAgreementConsent() {
        return agreementConsent;
    }

    public String getAgreementStatus() {
        return agreementStatus;
    }

    public LocalDateTime getAgreementConsentedAt() {
        return agreementConsentedAt;
    }

    public String getActivationMode() {
        return activationMode;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public String getDepositStatus() {
        return depositStatus;
    }

    public String getDepositPaymentOrderNo() {
        return depositPaymentOrderNo;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getApplyNote() {
        return applyNote;
    }

    public User getReviewer() {
        return reviewer;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void attachDepositPaymentOrder(String orderNo) {
        this.depositPaymentOrderNo = orderNo;
    }

    public boolean isRecoverableUnpaid() {
        return ("PENDING".equals(depositStatus) && "PENDING_PAYMENT".equals(reviewStatus))
                || "FAILED".equals(depositStatus)
                || "EXPIRED".equals(depositStatus);
    }

    public void convertToAgreement(
            PlatformRoleType roleType,
            String applyNote,
            PlatformProtocolVersion agreementVersion,
            PlatformProtocolConsent agreementConsent) {
        this.depositPolicy = "NO_DEPOSIT";
        if (depositAmount.compareTo(BigDecimal.ZERO) == 0) this.depositStatus = "NOT_REQUIRED";
        this.reviewStatus = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "APPROVED";
        this.applyNote = applyNote;
        this.agreementVersion = agreementVersion;
        this.agreementConsent = agreementConsent;
        this.agreementStatus = "CONSENTED";
        this.agreementConsentedAt = agreementConsent.getAcceptedAt();
        this.activationMode = roleType.manualReviewRequired() ? "PENDING_REVIEW" : "AUTO_APPROVE";
        this.reviewer = null;
        this.reviewedAt = null;
    }

    public boolean isAgreementBased() {
        return "NO_DEPOSIT".equals(depositPolicy);
    }

    public void resetForPayment(String applyNote) {
        boolean noDepositRequired = depositAmount.compareTo(BigDecimal.ZERO) <= 0;
        boolean keepPendingPaymentOrder = "PENDING".equals(depositStatus) && "PENDING_PAYMENT".equals(reviewStatus);
        this.applyNote = applyNote;
        this.depositStatus = noDepositRequired ? "NOT_REQUIRED" : "PENDING";
        this.reviewStatus = noDepositRequired ? "PENDING_REVIEW" : "PENDING_PAYMENT";
        if (!keepPendingPaymentOrder) {
            this.depositPaymentOrderNo = null;
        }
        this.reviewer = null;
        this.reviewedAt = null;
    }

    public void recordHistoricalDepositStatus(String status) {
        if (isAgreementBased()) this.depositStatus = status;
    }

    public void markDepositPaid() {
        this.depositStatus = "PAID";
        PlatformRoleType roleTypeValue = PlatformRoleType.valueOf(roleType);
        if (roleTypeValue.manualReviewRequired()) {
            this.reviewStatus = "PENDING_REVIEW";
        } else {
            this.reviewStatus = "APPROVED";
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public void markDepositFailed(String reason) {
        this.depositStatus = "FAILED";
        this.reviewStatus = "PENDING_PAYMENT";
        this.reviewer = null;
        this.reviewedAt = null;
    }

    public void markDepositExpired() {
        this.depositStatus = "EXPIRED";
        this.reviewStatus = "PENDING_PAYMENT";
        this.reviewer = null;
        this.reviewedAt = null;
    }

    public void markApproved(User reviewer) {
        this.reviewStatus = "APPROVED";
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void markRejected(User reviewer) {
        this.reviewStatus = "REJECTED";
        this.reviewer = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
}
