package com.campushub.ops;

import com.campushub.common.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Component
public class AnalyticsDateRangeParser {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Clock clock;

    public AnalyticsDateRangeParser() {
        this(Clock.systemDefaultZone());
    }

    AnalyticsDateRangeParser(Clock clock) {
        this.clock = clock;
    }

    public AnalyticsDateRange parse(String startDateValue, String endDateValue) {
        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = parseDate(endDateValue, today);
        LocalDate startDate = parseDate(startDateValue, endDate.minusDays(DEFAULT_DAYS - 1L));

        if (endDate.isBefore(startDate)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_DAYS) {
            throw new BusinessException("运营分析最多支持 366 天范围");
        }

        return new AnalyticsDateRange(
                startDate,
                endDate,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException("日期格式必须为 yyyy-MM-dd");
        }
    }
}
