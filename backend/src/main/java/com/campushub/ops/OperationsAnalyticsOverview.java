package com.campushub.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationsAnalyticsOverview(
        LocalDate startDate,
        LocalDate endDate,
        long newUsers,
        long activeUsers,
        long newTasks,
        long completedTasks,
        long taskIssues,
        long newGoods,
        long activeGoods,
        long goodsIntents,
        long newShopOrders,
        long completedShopOrders,
        long canceledShopOrders,
        long newProjectAds,
        long approvedProjectAds,
        long projectAdViews,
        long openReports,
        long pendingRoleApplications,
        long pendingProjectAds,
        BigDecimal paidServiceFeeAmount,
        BigDecimal roleDepositAmount,
        List<MetricCardSummary> cards) {
}
