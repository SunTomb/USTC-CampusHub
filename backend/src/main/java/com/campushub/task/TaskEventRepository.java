package com.campushub.task;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskEventRepository extends JpaRepository<TaskEvent, Long> {

    @EntityGraph(attributePaths = {"task", "actor"})
    List<TaskEvent> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
