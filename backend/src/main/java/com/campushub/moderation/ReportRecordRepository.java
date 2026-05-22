package com.campushub.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"reporter", "handler"})
    List<ReportRecord> findAll();

    @EntityGraph(attributePaths = {"reporter", "handler"})
    List<ReportRecord> findByStatusOrderByCreatedAtAsc(String status);

    @EntityGraph(attributePaths = {"reporter", "handler"})
    List<ReportRecord> findByTargetTypeOrderByCreatedAtDesc(String targetType);

    @EntityGraph(attributePaths = {"reporter", "handler"})
    List<ReportRecord> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    long countByStatus(String status);
}
