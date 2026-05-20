package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    @Override
    @EntityGraph(attributePaths = "reviewer")
    List<ReviewRecord> findAll();
}
