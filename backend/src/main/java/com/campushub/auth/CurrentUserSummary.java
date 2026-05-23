package com.campushub.auth;

import com.campushub.user.User;
import java.util.List;

public record CurrentUserSummary(
        Long id,
        String studentNo,
        String username,
        String realName,
        String nickname,
        String phone,
        String email,
        String wechatContact,
        String qqContact,
        String avatarUrl,
        Integer creditScore,
        String status,
        List<String> roles) {

    public static CurrentUserSummary from(User user, List<String> roles) {
        return new CurrentUserSummary(
                user.getId(),
                user.getStudentNo(),
                user.getUsername(),
                user.getRealName(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getWechatContact(),
                user.getQqContact(),
                user.getAvatarUrl(),
                user.getCreditScore(),
                user.getStatus(),
                roles);
    }
}
