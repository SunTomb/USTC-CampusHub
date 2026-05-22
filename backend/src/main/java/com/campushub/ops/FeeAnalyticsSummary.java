package com.campushub.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FeeAnalyticsSummary(
        LocalDate startDate,
        LocalDate endDate,
        long serviceFeeCount,
        BigDecimal paidServiceFeeAmount,
        BigDecimal pendingServiceFeeAmount,
        List<MetricCardSummary> serviceFeesByTargetType,
        long roleApplicationCount,
        BigDecimal roleDepositAmount,
        List<MetricCardSummary> roleDepositsByType) {
}
