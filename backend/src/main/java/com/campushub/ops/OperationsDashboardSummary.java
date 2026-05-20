package com.campushub.ops;

public record OperationsDashboardSummary(
        long publishedTasks,
        long acceptedTasks,
        long completedTasks,
        long openIssues,
        long pendingRoleApplications) {
}
