package com.campushub.moderation;

import java.time.LocalDateTime;

public record UserRestrictionSummary(
        Long id,
        Long userId,
        String userNickname,
        Long violationId,
        String restrictionType,
        String reason,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean active,
        Long adminId,
        String adminNickname,
        LocalDateTime createdAt) {

    public static UserRestrictionSummary from(UserRestriction restriction) {
        return new UserRestrictionSummary(
                restriction.getId(),
                restriction.getUser().getId(),
                restriction.getUser().getNickname(),
                restriction.getViolation() == null ? null : restriction.getViolation().getId(),
                restriction.getRestrictionType(),
                restriction.getReason(),
                restriction.getStartsAt(),
                restriction.getEndsAt(),
                restriction.getActive(),
                restriction.getAdmin() == null ? null : restriction.getAdmin().getId(),
                restriction.getAdmin() == null ? null : restriction.getAdmin().getNickname(),
                restriction.getCreatedAt());
    }
}
