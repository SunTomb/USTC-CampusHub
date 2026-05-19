package com.campushub.auth;

import com.campushub.user.User;

public record CurrentUserSummary(
        Long id,
        String studentNo,
        String username,
        String realName,
        String nickname,
        String phone,
        String email,
        String avatarUrl,
        Integer creditScore,
        String status) {

    public static CurrentUserSummary from(User user) {
        return new CurrentUserSummary(
                user.getId(),
                user.getStudentNo(),
                user.getUsername(),
                user.getRealName(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getCreditScore(),
                user.getStatus());
    }
}
