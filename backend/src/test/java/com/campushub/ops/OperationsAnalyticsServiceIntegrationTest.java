package com.campushub.ops;

import com.campushub.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class OperationsAnalyticsServiceIntegrationTest {

    private final AnalyticsDateRangeParser parser = new AnalyticsDateRangeParser(
            Clock.fixed(Instant.parse("2026-05-22T04:00:00Z"), ZoneId.of("Asia/Shanghai"))
    );

    @Autowired
    private OperationsAnalyticsService analyticsService;

    @Test
    void defaultsToRecentThirtyDaysWhenDatesAreMissing() {
        AnalyticsDateRange range = parser.parse(null, null);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 23));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(range.startInclusive()).isEqualTo(LocalDateTime.of(2026, 4, 23, 0, 0));
        assertThat(range.endExclusive()).isEqualTo(LocalDateTime.of(2026, 5, 23, 0, 0));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> parser.parse("2026-05-22", "2026-05-21"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("结束日期不能早于开始日期");
    }

    @Test
    void rejectsRangesLongerThanOneYear() {
        assertThatThrownBy(() -> parser.parse("2025-01-01", "2026-05-22"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("运营分析最多支持 366 天范围");
    }

    @Test
    void buildsOverviewFromExistingSeedData() {
        AnalyticsDateRange range = new AnalyticsDateRange(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 5, 22),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2026, 5, 23, 0, 0)
        );

        OperationsAnalyticsOverview overview = analyticsService.overview(range);

        assertThat(overview.newUsers()).isPositive();
        assertThat(overview.activeUsers()).isPositive();
        assertThat(overview.newTasks()).isGreaterThanOrEqualTo(0);
        assertThat(overview.newGoods()).isGreaterThanOrEqualTo(0);
        assertThat(overview.paidServiceFeeAmount()).isNotNull();
        assertThat(overview.roleDepositAmount()).isNotNull();
        assertThat(overview.cards()).extracting(MetricCardSummary::key)
                .contains("newUsers", "activeUsers", "newTasks", "newGoods", "openReports");
    }

    @Test
    void buildsBusinessFunnelsWithStableKeys() {
        AnalyticsDateRange range = new AnalyticsDateRange(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 5, 22),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2026, 5, 23, 0, 0)
        );

        OperationsFunnelSummary funnels = analyticsService.funnels(range);

        assertThat(funnels.funnels()).extracting(BusinessFunnelSummary::businessKey)
                .containsExactly("tasks", "goods", "shops", "projectAds");
        assertThat(funnels.funnels().get(0).steps()).extracting(MetricCardSummary::key)
                .contains("published", "grabMode", "applicationMode", "accepted", "completed", "issues");
    }

    @Test
    void buildsFeeSummaryWithoutChangingPaymentBoundary() {
        AnalyticsDateRange range = new AnalyticsDateRange(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 5, 22),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2026, 5, 23, 0, 0)
        );

        FeeAnalyticsSummary fees = analyticsService.fees(range);

        assertThat(fees.paidServiceFeeAmount()).isNotNull();
        assertThat(fees.pendingServiceFeeAmount()).isNotNull();
        assertThat(fees.roleDepositAmount()).isNotNull();
        assertThat(fees.roleDepositsByType()).extracting(MetricCardSummary::key)
                .contains("RUNNER", "GOODS_PUBLISHER", "SHOP_MERCHANT");
    }
}
