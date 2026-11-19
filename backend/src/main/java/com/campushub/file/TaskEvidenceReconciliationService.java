package com.campushub.file;

import java.util.HashSet;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TaskEvidenceReconciliationService {

    private final TaskDeliveryPhotoRepository photoRepository;
    private final TaskEvidenceStorage storage;

    public TaskEvidenceReconciliationService(
            TaskDeliveryPhotoRepository photoRepository,
            TaskEvidenceStorage storage) {
        this.photoRepository = photoRepository;
        this.storage = storage;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOrphanedFiles() {
        storage.reconcile(new HashSet<>(photoRepository.findAllStorageKeys()));
    }
}
