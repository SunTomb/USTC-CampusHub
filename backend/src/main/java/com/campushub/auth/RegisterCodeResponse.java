package com.campushub.auth;

public record RegisterCodeResponse(
        String email,
        long ttlMinutes,
        long resendSeconds) {
}
