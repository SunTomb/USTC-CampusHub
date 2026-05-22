package com.campushub.ops;

import java.time.LocalDate;
import java.util.List;

public record OperationsZoneSummary(
        LocalDate startDate,
        LocalDate endDate,
        List<ZoneMetricSummary> taskOriginZones,
        List<ZoneMetricSummary> taskDestinationZones,
        List<ZoneMetricSummary> taskRoutes,
        List<ZoneMetricSummary> goodsZones,
        List<ZoneMetricSummary> shopZones,
        List<ZoneMetricSummary> projectAdZones) {
}
