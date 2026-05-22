package com.campushub.moderation;

import java.util.List;

public record CreditCenterSummary(
        Long userId,
        String nickname,
        Integer creditScore,
        List<UserRestrictionSummary> activeRestrictions,
        List<ViolationRecordSummary> violations,
        List<CreditAdjustmentSummary> creditAdjustments,
        List<ReportRecordSummary> myReports) {
}
