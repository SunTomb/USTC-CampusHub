package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    @EntityGraph(attributePaths = {"admin"})
    List<AdminActionLog> findTop100ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"admin"})
    List<AdminActionLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
