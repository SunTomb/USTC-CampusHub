package com.campushub.interaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        Long parentId,
        @NotBlank @Size(max = 1000) String content) {
}
