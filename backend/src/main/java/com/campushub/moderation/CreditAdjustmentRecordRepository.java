package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAdjustmentRecordRepository extends JpaRepository<CreditAdjustmentRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<CreditAdjustmentRecord> findAll();

    @EntityGraph(attributePaths = {"user", "violation", "admin"})
    List<CreditAdjustmentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
