package com.campushub.user;

public record UserSummary(
        Long id,
        String username,
        String nickname,
        String email,
        String wechatContact,
        String qqContact,
        String avatarUrl,
        Integer creditScore,
        String status) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getWechatContact(), user.getQqContact(), user.getAvatarUrl(), user.getCreditScore(), user.getStatus());
    }
}
