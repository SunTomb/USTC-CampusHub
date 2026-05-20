package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "report"})
    List<ViolationRecord> findAll();
}
