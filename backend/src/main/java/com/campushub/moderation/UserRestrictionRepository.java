package com.campushub.moderation;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRestrictionRepository extends JpaRepository<UserRestriction, Long> {

    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<UserRestriction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<UserRestriction> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndActiveTrueAndRestrictionTypeIn(Long userId, Collection<String> restrictionTypes);

    long countByActiveTrue();
}
