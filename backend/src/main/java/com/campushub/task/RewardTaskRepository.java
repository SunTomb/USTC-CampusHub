package com.campushub.task;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardTaskRepository extends JpaRepository<RewardTask, Long> {

    @Override
    @EntityGraph(attributePaths = "publisher")
    Optional<RewardTask> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "publisher")
    List<RewardTask> findAll();

    @EntityGraph(attributePaths = "publisher")
    List<RewardTask> findByStatusOrderByDeadlineAsc(String status);

    @EntityGraph(attributePaths = "publisher")
    List<RewardTask> findByPublisherIdOrderByDeadlineAsc(Long publisherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RewardTask t join fetch t.publisher where t.id = :id")
    Optional<RewardTask> findByIdForUpdate(@Param("id") Long id);
}
