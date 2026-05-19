package com.campushub.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardTaskRepository extends JpaRepository<RewardTask, Long> {

    List<RewardTask> findByStatusOrderByDeadlineAsc(String status);
}
