package com.campushub.projectad;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAdRepository extends JpaRepository<ProjectAd, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from ProjectAd p where p.id=:id")
    Optional<ProjectAd> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"publisher", "reviewedBy"})
    Optional<ProjectAd> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"publisher", "reviewedBy"})
    List<ProjectAd> findAll();

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
