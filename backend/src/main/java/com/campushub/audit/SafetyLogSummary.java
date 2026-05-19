package com.campushub.audit;

import java.time.LocalDateTime;

public record SafetyLogSummary(
        Long id,
        Long userId,
        String userNickname,
        String action,
        String ipAddress,
        String userAgent,
        String detail,
        LocalDateTime createdAt) {

    public static SafetyLogSummary from(SafetyLog log) {
        return new SafetyLogSummary(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getUser() == null ? null : log.getUser().getNickname(),
                log.getAction(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetail(),
                log.getCreatedAt());
    }
}
