package com.campushub.review;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<ReviewSummary> create(@RequestParam(required = false) Long reviewerId, @Valid @RequestBody ReviewRequest request) {
        Long effectiveReviewerId = reviewerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(reviewerId);
        return ApiResponse.ok(reviewService.create(effectiveReviewerId, request));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<ReviewSummary>> listForUser(@PathVariable Long userId) {
        return ApiResponse.ok(reviewService.listForUser(userId));
    }
}
