package com.campushub.moderation;

import java.time.LocalDateTime;

public record ViolationRecordSummary(
        Long id,
        Long userId,
        String userNickname,
        Long reportId,
        String violationType,
        String description,
        Integer creditDelta,
        LocalDateTime createdAt) {

    public static ViolationRecordSummary from(ViolationRecord record) {
        return new ViolationRecordSummary(
                record.getId(),
                record.getUser().getId(),
                record.getUser().getNickname(),
                record.getReport() == null ? null : record.getReport().getId(),
                record.getViolationType(),
                record.getDescription(),
                record.getCreditDelta(),
                record.getCreatedAt());
    }
}
