package com.campushub.interaction;

import java.time.LocalDateTime;

public record FavoriteSummary(
        Long id,
        Long userId,
        String userNickname,
        String targetType,
        Long targetId,
        LocalDateTime createdAt) {

    public static FavoriteSummary from(Favorite favorite) {
        return new FavoriteSummary(
                favorite.getId(),
                favorite.getUser().getId(),
                favorite.getUser().getNickname(),
                favorite.getTargetType(),
                favorite.getTargetId(),
                favorite.getCreatedAt());
    }
}
