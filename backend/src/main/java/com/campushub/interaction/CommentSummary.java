package com.campushub.interaction;

import java.time.LocalDateTime;

public record CommentSummary(
        Long id,
        Long userId,
        String userNickname,
        String targetType,
        Long targetId,
        Long parentId,
        String content,
        String status,
        LocalDateTime createdAt) {

    public static CommentSummary from(Comment comment) {
        return new CommentSummary(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getContent(),
                comment.getStatus(),
                comment.getCreatedAt());
    }
}
