package com.campushub.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, Long> {
}
