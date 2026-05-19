package com.campushub.user;

public record UserSummary(
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

    public static UserSummary from(User user) {
        return new UserSummary(
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
