package com.campushub.identity;

import jakarta.validation.constraints.NotBlank;

public record ApplyRoleRequest(@NotBlank String roleType, String applyNote) {
}
