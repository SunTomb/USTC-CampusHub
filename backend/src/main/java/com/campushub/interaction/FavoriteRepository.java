package com.campushub.interaction;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = "user")
    List<Favorite> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    @EntityGraph(attributePaths = "user")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
