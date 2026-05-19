package com.campushub.auth;

import com.campushub.user.User;

public record RegisterResponse(
        Long userId,
        String username,
        String email) {

    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
