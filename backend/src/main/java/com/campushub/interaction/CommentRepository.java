package com.campushub.interaction;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, Long targetId);

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
