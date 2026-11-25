package com.campushub.task;

import java.time.LocalDateTime;

public record TaskEventSummary(
        Long id,
        Long actorId,
        String actorNickname,
        String eventType,
        String note,
        LocalDateTime createdAt) {

    public static TaskEventSummary from(TaskEvent event) {
        return new TaskEventSummary(
                event.getId(),
                event.getActor() == null ? null : event.getActor().getId(),
                event.getActor() == null ? null : event.getActor().getNickname(),
                event.getEventType(),
                event.getNote(),
                event.getCreatedAt());
    }
}
