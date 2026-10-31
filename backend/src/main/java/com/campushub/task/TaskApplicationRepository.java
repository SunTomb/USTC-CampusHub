package com.campushub.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskApplicationRepository extends JpaRepository<TaskApplication, Long> {

    @Override
    @EntityGraph(attributePaths = {"task", "task.publisher", "applicant"})
    Optional<TaskApplication> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"task", "task.publisher", "applicant"})
    List<TaskApplication> findAll();

    @EntityGraph(attributePaths = {"task", "task.publisher", "applicant"})
    List<TaskApplication> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    @EntityGraph(attributePaths = {"task", "task.publisher", "applicant"})
    List<TaskApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
}
