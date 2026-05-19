package com.campushub.moderation;

import java.time.LocalDateTime;

public record ReviewRecordSummary(
        Long id,
        Long reviewerId,
        String reviewerNickname,
        String targetType,
        Long targetId,
        String result,
        String reason,
        LocalDateTime createdAt) {

    public static ReviewRecordSummary from(ReviewRecord record) {
        return new ReviewRecordSummary(
                record.getId(),
                record.getReviewer() == null ? null : record.getReviewer().getId(),
                record.getReviewer() == null ? null : record.getReviewer().getNickname(),
                record.getTargetType(),
                record.getTargetId(),
                record.getResult(),
                record.getReason(),
                record.getCreatedAt());
    }
}
