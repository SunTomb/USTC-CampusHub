package com.campushub.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendRegisterCodeRequest(
        @NotBlank
        @Email
        String email) {
}
