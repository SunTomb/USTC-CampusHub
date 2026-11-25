package com.campushub.file;

import com.campushub.task.RewardTaskRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TaskEvidenceReconciliationService {
    private final TaskDeliveryPhotoRepository photos;
    private final RewardTaskRepository tasks;
    private final TaskEvidenceStorage storage;
    private final TransactionTemplate transactions;

    public TaskEvidenceReconciliationService(TaskDeliveryPhotoRepository photos, RewardTaskRepository tasks,
            TaskEvidenceStorage storage, TransactionTemplate transactions) {
        this.photos = photos;
        this.tasks = tasks;
        this.storage = storage;
        this.transactions = transactions;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOrphanedFiles() {
        storage.visitStaleFiles(Instant.now().minus(1, ChronoUnit.DAYS), (taskId, key) ->
                transactions.executeWithoutResult(status -> {
                    // Same lock as upload/delete: even an unusually long transaction must finish first.
                    tasks.findByIdForUpdate(taskId);
                    if (!photos.existsByStorageKeyAndStatus(key, "UPLOADED")) storage.delete(key);
                }));
    }
}
