package com.campushub.interaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotBlank String targetType, @NotNull Long targetId) {
}
