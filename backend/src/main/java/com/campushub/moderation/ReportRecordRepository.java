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

    java.util.Optional<ReportRecord> findFirstByReporterIdAndTargetTypeAndTargetIdOrderByIdDesc(Long reporterId,String targetType,Long targetId);

    @EntityGraph(attributePaths = {"reporter", "handler"})
    List<ReportRecord> findByStatusAndTargetTypeOrderByCreatedAtAsc(String status,String targetType);

    long countByStatus(String status);
}
