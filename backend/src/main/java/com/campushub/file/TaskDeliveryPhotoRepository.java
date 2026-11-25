package com.campushub.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDeliveryPhotoRepository extends JpaRepository<TaskDeliveryPhoto, Long> {
    Optional<TaskDeliveryPhoto> findByTaskIdAndEvidenceTypeAndUploaderIdAndClientRequestId(
            Long taskId, String evidenceType, Long uploaderId, String clientRequestId);
    Optional<TaskDeliveryPhoto> findByTaskIdAndIdAndStatus(Long taskId, Long id, String status);
    List<TaskDeliveryPhoto> findByTaskIdAndStatusOrderByIdAsc(Long taskId, String status);
    long countByTaskIdAndEvidenceTypeAndStatus(Long taskId, String evidenceType, String status);
    boolean existsByStorageKeyAndStatus(String storageKey, String status);
    long countByTaskId(Long taskId);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.sizeBytes), 0) from TaskDeliveryPhoto p where p.task.id = :taskId and p.status = 'UPLOADED'")
    Long sumSizeBytesByTaskId(Long taskId);
}
