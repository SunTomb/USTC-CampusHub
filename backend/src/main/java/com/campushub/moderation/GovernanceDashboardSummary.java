package com.campushub.moderation;

public record GovernanceDashboardSummary(
        long openReports,
        long inReviewReports,
        long handledReports,
        long highSeverityViolations,
        long activeRestrictions) {
}
