package com.campushub.review;

import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewSummary create(Long reviewerId, ReviewRequest request) {
        throw new BusinessException("请从已完成订单进入评价");
    }

    @Transactional(readOnly = true)
    public List<ReviewSummary> listForUser(Long userId) {
        return reviewRepository.findByTargetUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReviewSummary::from)
                .toList();
    }
}
