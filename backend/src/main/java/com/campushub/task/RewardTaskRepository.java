package com.campushub.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardTaskRepository extends JpaRepository<RewardTask, Long> {

    @Override
    @EntityGraph(attributePaths = "publisher")
    Optional<RewardTask> findById(Long id);

    @EntityGraph(attributePaths = "publisher")
    List<RewardTask> findByStatusOrderByDeadlineAsc(String status);
}
