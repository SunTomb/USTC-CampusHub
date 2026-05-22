package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "report", "admin"})
    List<ViolationRecord> findAll();

    @EntityGraph(attributePaths = {"user", "report", "admin"})
    List<ViolationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "report", "admin"})
    List<ViolationRecord> findBySeverityOrderByCreatedAtDesc(String severity);

    long countBySeverityIn(List<String> severities);
}
