package com.campushub.interaction;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;

    public InteractionController(CommentRepository commentRepository, FavoriteRepository favoriteRepository) {
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping("/{targetType}/{targetId}/comments")
    public ApiResponse<List<CommentSummary>> listComments(@PathVariable String targetType, @PathVariable Long targetId) {
        List<CommentSummary> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetType, targetId)
                .stream()
                .map(CommentSummary::from)
                .toList();
        return ApiResponse.ok(comments);
    }

    @GetMapping("/{targetType}/{targetId}/favorites")
    public ApiResponse<List<FavoriteSummary>> listFavorites(@PathVariable String targetType, @PathVariable Long targetId) {
        List<FavoriteSummary> favorites = favoriteRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId)
                .stream()
                .map(FavoriteSummary::from)
                .toList();
        return ApiResponse.ok(favorites);
    }

    @GetMapping("/users/{userId}/comments")
    public ApiResponse<List<CommentSummary>> listUserComments(@PathVariable Long userId) {
        List<CommentSummary> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(CommentSummary::from)
                .toList();
        return ApiResponse.ok(comments);
    }

    @GetMapping("/users/{userId}/favorites")
    public ApiResponse<List<FavoriteSummary>> listUserFavorites(@PathVariable Long userId) {
        List<FavoriteSummary> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteSummary::from)
                .toList();
        return ApiResponse.ok(favorites);
    }
}
