package com.campushub.moderation;

public record CreateViolationRequest(
        Long userId,
        Long reportId,
        String targetType,
        Long targetId,
        String violationType,
        String severity,
        String penaltyType,
        String description,
        Integer creditDelta,
        String depositImpactNote,
        String restrictionType,
        Integer restrictionDays) {
}
