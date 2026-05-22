package com.campushub.ops;

import com.campushub.common.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    private EntityManager entityManager;

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
    void feeSummaryCountsOnlyPaidTargetTypesAndPaidRoleDepositsWithStatusAwareDates() {
        entityManager.createNativeQuery("""
                        INSERT INTO service_fee_records (fee_no, payer_id, target_type, target_id, amount, status, created_at, paid_at)
                        VALUES
                        ('QA-SF-PAID-IN', 1, 'QA_TARGET', 9001, 12.34, 'PAID', TIMESTAMP '2026-04-01 09:00:00', TIMESTAMP '2026-05-10 09:00:00'),
                        ('QA-SF-PAID-OUT', 1, 'QA_TARGET', 9002, 23.45, 'PAID', TIMESTAMP '2026-05-10 09:00:00', TIMESTAMP '2026-06-01 09:00:00'),
                        ('QA-SF-PENDING-IN', 1, 'QA_TARGET', 9003, 34.56, 'PENDING', TIMESTAMP '2026-05-10 09:00:00', NULL),
                        ('QA-SF-PENDING-OUT', 1, 'QA_TARGET', 9004, 45.67, 'PENDING', TIMESTAMP '2026-04-01 09:00:00', NULL)
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO role_applications (user_id, role_type, deposit_amount, deposit_status, review_status, apply_note, created_at, updated_at)
                        VALUES
                        (1, 'QA_PAID_ROLE', 11.00, 'PAID', 'APPROVED', 'qa paid', TIMESTAMP '2026-05-10 09:00:00', TIMESTAMP '2026-05-10 09:00:00'),
                        (2, 'QA_PENDING_ROLE', 22.00, 'PENDING', 'PENDING_REVIEW', 'qa pending', TIMESTAMP '2026-05-10 09:00:00', TIMESTAMP '2026-05-10 09:00:00')
                        """)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        AnalyticsDateRange range = new AnalyticsDateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0)
        );

        FeeAnalyticsSummary fees = analyticsService.fees(range);
        OperationsAnalyticsOverview overview = analyticsService.overview(range);

        assertThat(fees.paidServiceFeeAmount()).isEqualByComparingTo("14.22");
        assertThat(fees.pendingServiceFeeAmount()).isEqualByComparingTo("35.55");
        assertThat(fees.serviceFeesByTargetType()).filteredOn(card -> "QA_TARGET".equals(card.key()))
                .singleElement()
                .extracting(MetricCardSummary::value)
                .isEqualTo(new BigDecimal("12.34"));
        assertThat(fees.roleDepositAmount()).isEqualByComparingTo("11.00");
        assertThat(fees.roleDepositsByType()).extracting(MetricCardSummary::key)
                .contains("RUNNER", "GOODS_PUBLISHER", "SHOP_MERCHANT", "QA_PAID_ROLE");
        assertThat(fees.roleDepositsByType()).filteredOn(card -> "QA_PAID_ROLE".equals(card.key()))
                .singleElement()
                .extracting(MetricCardSummary::value)
                .isEqualTo(new BigDecimal("11.00"));
        assertThat(fees.roleDepositsByType()).noneMatch(card -> "QA_PENDING_ROLE".equals(card.key()));
        assertThat(overview.roleDepositAmount()).isEqualByComparingTo("11.00");
    }

    @Test
    void usesBusinessTimestampsForGoodsAndProjectAdMetrics() {
        entityManager.createNativeQuery("""
                        INSERT INTO goods (id, seller_id, category_id, title, description, price, original_price, condition_level,
                                           trade_location, status, view_count, created_at, updated_at, campus_zone, contact_visibility,
                                           delivery_method, service_fee_policy, published_at)
                        VALUES
                        (9101, 1, 1, 'QA old created goods', 'qa', 10.00, NULL, 'GOOD', '东区', 'PUBLISHED', 0,
                         TIMESTAMP '2026-04-01 09:00:00', TIMESTAMP '2026-04-01 09:00:00', 'EAST', 'INTENT_ONLY', 'OFFLINE_MEETUP', 'NONE', TIMESTAMP '2026-05-10 09:00:00')
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO project_ads (id, publisher_id, title, ad_type, summary, description, tags, campus_zone, cover_file_id,
                                                 link_url, contact_info, contact_visibility, expires_at, featured, featured_priority,
                                                 review_note, reviewed_by, reviewed_at, published_at, closed_at, status, view_count, created_at, updated_at)
                        VALUES
                        (9101, 1, 'QA old created ad', 'OTHER', NULL, 'qa', NULL, NULL, NULL, NULL, 'qa@campus.example', 'LOGIN_ONLY', NULL,
                         FALSE, 0, NULL, 4, TIMESTAMP '2026-04-02 09:00:00', TIMESTAMP '2026-05-10 09:00:00', NULL, 'APPROVED', 123,
                         TIMESTAMP '2026-04-01 09:00:00', TIMESTAMP '2026-04-01 09:00:00')
                        """)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        AnalyticsDateRange range = new AnalyticsDateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0)
        );

        OperationsAnalyticsOverview overview = analyticsService.overview(range);
        OperationsFunnelSummary funnels = analyticsService.funnels(range);

        assertThat(overview.newGoods()).isPositive();
        assertThat(overview.approvedProjectAds()).isPositive();
        assertThat(overview.projectAdViews()).isGreaterThanOrEqualTo(123);
        assertThat(funnels.funnels().stream()
                .filter(funnel -> "projectAds".equals(funnel.businessKey()))
                .findFirst()
                .orElseThrow()
                .steps())
                .filteredOn(card -> "views".equals(card.key()))
                .singleElement()
                .extracting(MetricCardSummary::label)
                .isEqualTo("项目广告累计浏览");
    }

}
