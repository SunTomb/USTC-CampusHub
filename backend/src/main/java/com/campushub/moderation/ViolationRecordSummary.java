package com.campushub.moderation;

import java.time.LocalDateTime;

public record ViolationRecordSummary(
        Long id,
        Long userId,
        String userNickname,
        Long reportId,
        String targetType,
        Long targetId,
        String violationType,
        String severity,
        String penaltyType,
        String description,
        Integer creditDelta,
        Long adminId,
        String adminNickname,
        String depositImpactNote,
        LocalDateTime createdAt) {

    public static ViolationRecordSummary from(ViolationRecord record) {
        return new ViolationRecordSummary(
                record.getId(),
                record.getUser().getId(),
                record.getUser().getNickname(),
                record.getReport() == null ? null : record.getReport().getId(),
                record.getTargetType(),
                record.getTargetId(),
                record.getViolationType(),
                record.getSeverity(),
                record.getPenaltyType(),
                record.getDescription(),
                record.getCreditDelta(),
                record.getAdmin() == null ? null : record.getAdmin().getId(),
                record.getAdmin() == null ? null : record.getAdmin().getNickname(),
                record.getDepositImpactNote(),
                record.getCreatedAt());
    }
}
