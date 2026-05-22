package com.campushub.ops;

import java.math.BigDecimal;

public record MetricCardSummary(
        String key,
        String label,
        BigDecimal value,
        String unit) {
}
