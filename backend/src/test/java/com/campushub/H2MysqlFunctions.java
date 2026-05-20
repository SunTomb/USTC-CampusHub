package com.campushub;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class H2MysqlFunctions {

    private H2MysqlFunctions() {
    }

    public static Timestamp dateAdd(Timestamp timestamp, String intervalExpression) {
        LocalDateTime value = timestamp.toLocalDateTime();
        String[] parts = intervalExpression.trim().split("\\s+");
        long amount = Long.parseLong(parts[0]);
        String unit = parts[1].toUpperCase();
        LocalDateTime result = switch (unit) {
            case "DAY" -> value.plusDays(amount);
            case "HOUR" -> value.plusHours(amount);
            default -> throw new IllegalArgumentException("Unsupported interval unit: " + unit);
        };
        return Timestamp.valueOf(result);
    }
}
