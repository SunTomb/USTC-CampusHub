package com.campushub.task;

import java.time.LocalDateTime;

public record TaskIssueSummary(
        Long id,
        Long taskId,
        String taskTitle,
        Long reporterId,
        String reporterNickname,
        String issueType,
        String description,
        String status,
        LocalDateTime createdAt) {

    public static TaskIssueSummary from(TaskIssue issue) {
        return new TaskIssueSummary(
                issue.getId(),
                issue.getTask().getId(),
                issue.getTask().getTitle(),
                issue.getReporter().getId(),
                issue.getReporter().getNickname(),
                issue.getIssueType(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getCreatedAt());
    }
}
