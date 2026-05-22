package com.campushub.ops;

import java.time.LocalDate;
import java.util.List;

public record OperationsFunnelSummary(
        LocalDate startDate,
        LocalDate endDate,
        List<BusinessFunnelSummary> funnels) {
}
