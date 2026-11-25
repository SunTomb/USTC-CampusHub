package com.campushub.platform;

import java.time.LocalDateTime;

public record PlatformProtocolSummary(
        String agreementCode,
        String version,
        String title,
        String content,
        LocalDateTime effectiveAt,
        LocalDateTime acceptedAt) {
}
