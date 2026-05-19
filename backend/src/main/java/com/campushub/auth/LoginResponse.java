package com.campushub.auth;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresInMinutes,
        CurrentUserSummary user) {
}
