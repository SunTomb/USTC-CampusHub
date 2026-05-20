package com.campushub.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BindFileRequest(
        @NotNull Long fileId,
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank String usageType,
        @NotNull Integer sortOrder) {
}
