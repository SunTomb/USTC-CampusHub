package com.campushub.identity;

import jakarta.validation.constraints.NotBlank;

public record ApplyRoleRequest(
        @NotBlank String roleType,
        String applyNote,
        String agreementVersion,
        boolean agreementAccepted) {

    public ApplyRoleRequest(String roleType, String applyNote) {
        this(roleType, applyNote, null, false);
    }
}
