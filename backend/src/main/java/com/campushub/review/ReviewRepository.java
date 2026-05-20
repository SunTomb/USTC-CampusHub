package com.campushub.review;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByReviewerIdAndTargetTypeAndTargetId(Long reviewerId, String targetType, Long targetId);

    @EntityGraph(attributePaths = {"reviewer", "targetUser"})
    List<Review> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    @EntityGraph(attributePaths = {"reviewer", "targetUser"})
    List<Review> findTop5ByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
}
