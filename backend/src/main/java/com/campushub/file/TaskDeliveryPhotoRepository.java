package com.campushub.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaskDeliveryPhotoRepository extends JpaRepository<TaskDeliveryPhoto, Long> {

    @EntityGraph(attributePaths = {"task", "acceptedApplication", "uploader"})
    Optional<TaskDeliveryPhoto> findById(Long id);

    @EntityGraph(attributePaths = {"task", "acceptedApplication", "uploader"})
    Optional<TaskDeliveryPhoto> findByTaskIdAndUploaderIdAndClientRequestId(Long taskId, Long uploaderId, String clientRequestId);

    @EntityGraph(attributePaths = {"task", "acceptedApplication", "uploader"})
    List<TaskDeliveryPhoto> findByTaskIdAndStatusOrderByCreatedAtAsc(Long taskId, String status);

    long countByTaskIdAndStatus(Long taskId, String status);

    default long countByTaskId(Long taskId) {
        return countByTaskIdAndStatus(taskId, "UPLOADED");
    }

    @Query("select p.storageKey from TaskDeliveryPhoto p")
    List<String> findAllStorageKeys();
    @Query("select coalesce(sum(p.sizeBytes), 0) from TaskDeliveryPhoto p where p.task.id = :taskId and p.status = 'UPLOADED'")
    Long sumSizeBytesByTaskId(Long taskId);
}
