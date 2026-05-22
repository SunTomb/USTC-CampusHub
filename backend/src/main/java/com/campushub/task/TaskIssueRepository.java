package com.campushub.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskIssueRepository extends JpaRepository<TaskIssue, Long> {

    @Override
    @EntityGraph(attributePaths = {"task", "reporter", "handler"})
    Optional<TaskIssue> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"task", "reporter", "handler"})
    List<TaskIssue> findAll();

    @EntityGraph(attributePaths = {"task", "reporter", "handler"})
    List<TaskIssue> findByStatusOrderByCreatedAtAsc(String status);
}
