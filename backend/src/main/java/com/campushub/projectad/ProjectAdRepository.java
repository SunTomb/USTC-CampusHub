package com.campushub.projectad;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAdRepository extends JpaRepository<ProjectAd, Long> {

    @Override
    @EntityGraph(attributePaths = {"publisher", "reviewedBy"})
    Optional<ProjectAd> findById(Long id);

    @EntityGraph(attributePaths = {"publisher"})
    List<ProjectAd> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"publisher"})
    List<ProjectAd> findByStatusOrderByFeaturedDescFeaturedPriorityDescCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"publisher"})
    List<ProjectAd> findByFeaturedTrueAndStatusOrderByFeaturedPriorityDescCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"publisher"})
    List<ProjectAd> findByPublisherIdOrderByCreatedAtDesc(Long publisherId);

    @EntityGraph(attributePaths = {"publisher"})
    List<ProjectAd> findAllByOrderByCreatedAtDesc();
}
