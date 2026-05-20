package com.campushub.review;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewSummary create(Long reviewerId, ReviewRequest request) {
        if (reviewRepository.existsByReviewerIdAndTargetTypeAndTargetId(reviewerId, request.targetType(), request.targetId())) {
            throw new BusinessException("已评价过该交易");
        }
        User reviewer = userRepository.findById(reviewerId).orElseThrow(() -> new BusinessException("评价人不存在"));
        User target = userRepository.findById(request.targetUserId()).orElseThrow(() -> new BusinessException("被评价用户不存在"));
        Review review = new Review(reviewer, target, request.targetType(), request.targetId(), request.rating(), request.content());
        return ReviewSummary.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewSummary> listForUser(Long userId) {
        return reviewRepository.findByTargetUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReviewSummary::from)
                .toList();
    }
}
