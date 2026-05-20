package com.campushub.review;

import java.time.LocalDateTime;

public record ReviewSummary(
        Long id,
        Long reviewerId,
        String reviewerNickname,
        Long targetUserId,
        String targetUserNickname,
        String targetType,
        Long targetId,
        Integer rating,
        String content,
        LocalDateTime createdAt) {

    public static ReviewSummary from(Review review) {
        return new ReviewSummary(
                review.getId(),
                review.getReviewer().getId(),
                review.getReviewer().getNickname(),
                review.getTargetUser().getId(),
                review.getTargetUser().getNickname(),
                review.getTargetType(),
                review.getTargetId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt());
    }
}
