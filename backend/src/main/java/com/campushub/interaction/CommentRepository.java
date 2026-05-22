package com.campushub.interaction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "parent"})
    Optional<Comment> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findAll();

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, Long targetId);

    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
