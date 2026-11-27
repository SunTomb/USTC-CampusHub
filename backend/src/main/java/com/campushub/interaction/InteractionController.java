package com.campushub.interaction;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final InteractionService interactionService;
    private final CurrentUserService currentUserService;

    public InteractionController(
            CommentRepository commentRepository,
            FavoriteRepository favoriteRepository,
            InteractionService interactionService,
            CurrentUserService currentUserService) {
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.interactionService = interactionService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{targetType}/{targetId}/comments")
    public ApiResponse<List<CommentSummary>> listComments(@PathVariable String targetType, @PathVariable Long targetId) {
        return ApiResponse.ok(interactionService.listComments(targetType, targetId,
                currentUserService.requireUserId(), currentUserService.isAnyAdmin()));
    }

    @GetMapping("/{targetType}/{targetId}/favorites")
    public ApiResponse<List<FavoriteSummary>> listFavorites(@PathVariable String targetType, @PathVariable Long targetId) {
        return ApiResponse.ok(interactionService.listFavorites(targetType, targetId,
                currentUserService.requireUserId(), currentUserService.isAnyAdmin()));
    }

    @PostMapping("/comments")
    public ApiResponse<CommentSummary> comment(@RequestParam(required = false) Long userId, @Valid @RequestBody CommentRequest request) {
        Long effectiveUserId = requireActingUser(userId);
        return ApiResponse.ok(interactionService.comment(effectiveUserId, request, currentUserService.isAnyAdmin()));
    }

    @PostMapping("/favorites")
    public ApiResponse<Void> favorite(@RequestParam(required = false) Long userId, @Valid @RequestBody FavoriteRequest request) {
        Long effectiveUserId = requireActingUser(userId);
        interactionService.favorite(effectiveUserId, request, currentUserService.isAnyAdmin());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/favorites")
    public ApiResponse<Void> unfavorite(@RequestParam(required = false) Long userId, @Valid @RequestBody FavoriteRequest request) {
        Long effectiveUserId = requireActingUser(userId);
        interactionService.unfavorite(effectiveUserId, request);
        return ApiResponse.ok(null);
    }

    private Long requireActingUser(Long requestedId) {
        Long actorId = currentUserService.requireUserId();
        if (requestedId != null && !actorId.equals(requestedId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
        return actorId;
    }

    @GetMapping("/users/{userId}/comments")
    public ApiResponse<List<CommentSummary>> listUserComments(@PathVariable Long userId) {
        Long effectiveUserId = currentUserService.requireSameUser(userId);
        List<CommentSummary> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId).stream()
                .map(CommentSummary::from)
                .toList();
        return ApiResponse.ok(comments);
    }

    @GetMapping("/users/{userId}/favorites")
    public ApiResponse<List<FavoriteSummary>> listUserFavorites(@PathVariable Long userId) {
        Long effectiveUserId = currentUserService.requireSameUser(userId);
        List<FavoriteSummary> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId).stream()
                .map(FavoriteSummary::from)
                .toList();
        return ApiResponse.ok(favorites);
    }
}
