package com.campushub.ops;

import java.util.List;

public record BusinessFunnelSummary(
        String businessKey,
        String businessName,
        List<MetricCardSummary> steps) {
}
