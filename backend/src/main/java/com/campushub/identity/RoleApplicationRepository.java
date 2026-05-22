package com.campushub.identity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<RoleApplication> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<RoleApplication> findAll();

    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<RoleApplication> findByUserIdAndRoleType(Long userId, String roleType);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<RoleApplication> findByReviewStatusOrderByCreatedAtAsc(String reviewStatus);
}
