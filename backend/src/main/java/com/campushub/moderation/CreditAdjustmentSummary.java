package com.campushub.moderation;

import java.time.LocalDateTime;

public record CreditAdjustmentSummary(
        Long id,
        Long userId,
        String userNickname,
        Long violationId,
        Integer beforeScore,
        Integer deltaScore,
        Integer afterScore,
        String reason,
        Long adminId,
        String adminNickname,
        LocalDateTime createdAt) {

    public static CreditAdjustmentSummary from(CreditAdjustmentRecord record) {
        return new CreditAdjustmentSummary(
                record.getId(),
                record.getUser().getId(),
                record.getUser().getNickname(),
                record.getViolation() == null ? null : record.getViolation().getId(),
                record.getBeforeScore(),
                record.getDeltaScore(),
                record.getAfterScore(),
                record.getReason(),
                record.getAdmin() == null ? null : record.getAdmin().getId(),
                record.getAdmin() == null ? null : record.getAdmin().getNickname(),
                record.getCreatedAt());
    }
}
