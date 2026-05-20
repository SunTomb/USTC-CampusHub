package com.campushub.interaction;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {

    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public InteractionService(CommentRepository commentRepository, FavoriteRepository favoriteRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentSummary comment(Long userId, CommentRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        Comment parent = request.parentId() == null ? null : commentRepository.findById(request.parentId())
                .orElseThrow(() -> new BusinessException("父评论不存在"));
        Comment comment = new Comment(user, request.targetType(), request.targetId(), parent, request.content().trim());
        return CommentSummary.from(commentRepository.save(comment));
    }

    @Transactional
    public void favorite(Long userId, FavoriteRequest request) {
        if (favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId())) {
            return;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        favoriteRepository.save(new Favorite(user, request.targetType(), request.targetId()));
    }

    @Transactional
    public void unfavorite(Long userId, FavoriteRequest request) {
        favoriteRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId());
    }
}
