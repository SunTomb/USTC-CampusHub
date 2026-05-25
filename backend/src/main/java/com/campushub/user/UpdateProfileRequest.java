package com.campushub.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 3, max = 64)
        String username,
        @NotBlank
        @Size(max = 64)
        String nickname) {
}
