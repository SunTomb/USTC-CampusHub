package com.campushub.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceFeeRecordRepository extends JpaRepository<ServiceFeeRecord, Long> {

    @Override
    @EntityGraph(attributePaths = "payer")
    Optional<ServiceFeeRecord> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "payer")
    List<ServiceFeeRecord> findAll();

    @EntityGraph(attributePaths = "payer")
    List<ServiceFeeRecord> findByPayerIdOrderByCreatedAtDesc(Long payerId);
}
