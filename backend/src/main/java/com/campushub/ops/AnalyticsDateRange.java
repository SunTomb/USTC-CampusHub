package com.campushub.ops;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalyticsDateRange(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive) {
}
