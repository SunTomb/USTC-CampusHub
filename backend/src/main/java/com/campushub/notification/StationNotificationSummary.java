package com.campushub.notification;

import java.time.LocalDateTime;

public record StationNotificationSummary(
        Long id,
        Long recipientId,
        String title,
        String content,
        String targetType,
        Long targetId,
        LocalDateTime readAt,
        LocalDateTime createdAt) {

    public static StationNotificationSummary from(StationNotification notification) {
        return new StationNotificationSummary(
                notification.getId(),
                notification.getRecipient().getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
