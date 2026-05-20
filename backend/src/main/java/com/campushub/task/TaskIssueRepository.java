package com.campushub.task;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskIssueRepository extends JpaRepository<TaskIssue, Long> {

    @EntityGraph(attributePaths = {"task", "reporter", "handler"})
    List<TaskIssue> findByStatusOrderByCreatedAtAsc(String status);
}
