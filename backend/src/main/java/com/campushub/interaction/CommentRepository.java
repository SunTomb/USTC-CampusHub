package com.campushub.interaction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, Long targetId);

    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
