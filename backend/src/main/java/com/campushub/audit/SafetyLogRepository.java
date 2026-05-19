package com.campushub.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyLogRepository extends JpaRepository<SafetyLog, Long> {
}
