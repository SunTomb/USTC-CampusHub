package com.campushub.audit;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyLogRepository extends JpaRepository<SafetyLog, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    List<SafetyLog> findAll();
}
