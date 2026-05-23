package com.campushub.auth;

public record JwtTokenClaims(Long userId, String username) {
}
