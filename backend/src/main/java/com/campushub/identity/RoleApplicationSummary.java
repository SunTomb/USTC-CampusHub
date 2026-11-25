package com.campushub.identity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoleApplicationSummary(
        Long id,
        Long userId,
        String userNickname,
        String roleType,
        BigDecimal depositAmount,
        String depositStatus,
        String depositPaymentOrderNo,
        String reviewStatus,
        String depositPolicy,
        String agreementStatus,
        String agreementVersion,
        LocalDateTime agreementConsentedAt,
        String activationMode,
        String applyNote,
        String reviewerNickname,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt) {

    public static RoleApplicationSummary from(RoleApplication application) {
        return new RoleApplicationSummary(
                application.getId(),
                application.getUser().getId(),
                application.getUser().getNickname(),
                application.getRoleType(),
                application.getDepositAmount(),
                application.getDepositStatus(),
                application.getDepositPaymentOrderNo(),
                application.getReviewStatus(),
                application.getDepositPolicy(),
                application.getAgreementStatus(),
                application.getAgreementVersion() == null ? null : application.getAgreementVersion().getVersion(),
                application.getAgreementConsentedAt(),
                application.getActivationMode(),
                application.getApplyNote(),
                application.getReviewer() == null ? null : application.getReviewer().getNickname(),
                application.getCreatedAt(),
                application.getReviewedAt());
    }
}
