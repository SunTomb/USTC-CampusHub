package com.campushub.payment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceFeeRecordRepository extends JpaRepository<ServiceFeeRecord, Long> {

    List<ServiceFeeRecord> findByPayerIdOrderByCreatedAtDesc(Long payerId);
}
