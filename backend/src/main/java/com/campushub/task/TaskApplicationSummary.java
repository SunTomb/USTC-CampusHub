package com.campushub.task;

import java.time.LocalDateTime;

public record TaskApplicationSummary(
        Long id,
        Long taskId,
        String taskTitle,
        Long applicantId,
        String applicantNickname,
        String message,
        String status,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt) {

    public static TaskApplicationSummary from(TaskApplication application) {
        return new TaskApplicationSummary(
                application.getId(),
                application.getTask().getId(),
                application.getTask().getTitle(),
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getMessage(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getAcceptedAt(),
                application.getCompletedAt());
    }
}
