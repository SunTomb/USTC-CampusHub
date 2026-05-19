package com.campushub.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskApplicationRepository extends JpaRepository<TaskApplication, Long> {

    List<TaskApplication> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<TaskApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
}
