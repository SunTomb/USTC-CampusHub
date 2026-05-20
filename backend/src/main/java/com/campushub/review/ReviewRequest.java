package com.campushub.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull Long targetUserId,
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String content) {
}
