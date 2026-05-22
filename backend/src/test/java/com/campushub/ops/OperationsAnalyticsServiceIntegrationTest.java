package com.campushub.ops;

import com.campushub.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationsAnalyticsServiceIntegrationTest {

    private final AnalyticsDateRangeParser parser = new AnalyticsDateRangeParser(
            Clock.fixed(Instant.parse("2026-05-22T04:00:00Z"), ZoneId.of("Asia/Shanghai"))
    );

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
}
