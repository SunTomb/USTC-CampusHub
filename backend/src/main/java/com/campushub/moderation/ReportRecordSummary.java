package com.campushub.moderation;

import java.time.LocalDateTime;

public record ReportRecordSummary(
        Long id,
        Long reporterId,
        String reporterNickname,
        String targetType,
        Long targetId,
        String reason,
        String description,
        String status,
        String reviewNote,
        String resolutionType,
        Long handlerId,
        String handlerNickname,
        LocalDateTime handledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ReportRecordSummary from(ReportRecord record) {
        return new ReportRecordSummary(
                record.getId(),
                record.getReporter().getId(),
                record.getReporter().getNickname(),
                record.getTargetType(),
                record.getTargetId(),
                record.getReason(),
                record.getDescription(),
                record.getStatus(),
                record.getReviewNote(),
                record.getResolutionType(),
                record.getHandler() == null ? null : record.getHandler().getId(),
                record.getHandler() == null ? null : record.getHandler().getNickname(),
                record.getHandledAt(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
