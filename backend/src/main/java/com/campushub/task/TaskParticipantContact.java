package com.campushub.task;

import com.campushub.user.User;

public record TaskParticipantContact(
        Long userId,
        String nickname,
        String wechatContact,
        String qqContact) {

    public static TaskParticipantContact from(User user) {
        return new TaskParticipantContact(
                user.getId(),
                user.getNickname(),
                user.getWechatContact(),
                user.getQqContact());
    }
}
