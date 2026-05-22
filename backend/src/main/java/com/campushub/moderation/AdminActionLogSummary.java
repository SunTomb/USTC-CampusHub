package com.campushub.moderation;

import java.time.LocalDateTime;

public record AdminActionLogSummary(
        Long id,
        Long adminId,
        String adminNickname,
        String actionType,
        String targetType,
        Long targetId,
        String note,
        LocalDateTime createdAt) {

    public static AdminActionLogSummary from(AdminActionLog log) {
        return new AdminActionLogSummary(
                log.getId(),
                log.getAdmin() == null ? null : log.getAdmin().getId(),
                log.getAdmin() == null ? null : log.getAdmin().getNickname(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getNote(),
                log.getCreatedAt());
    }
}
