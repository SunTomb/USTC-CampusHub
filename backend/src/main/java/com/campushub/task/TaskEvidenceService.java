package com.campushub.task;

import com.campushub.common.BusinessException;
import com.campushub.file.*;
import com.campushub.auth.UserRoleService;
import com.campushub.moderation.GovernanceService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskEvidenceService {
    private static final String REQUEST = "REQUEST_IMAGE", DELIVERY = "DELIVERY_PHOTO", ACTIVE = "UPLOADED";
    private final RewardTaskRepository tasks;
    private final TaskDeliveryPhotoRepository photos;
    private final TaskEventRepository events;
    private final TaskEvidenceStorage storage;
    private final GovernanceService governance;
    private final UserRoleService roles;

    public TaskEvidenceService(RewardTaskRepository tasks, TaskDeliveryPhotoRepository photos,
            TaskEventRepository events, TaskEvidenceStorage storage, GovernanceService governance, UserRoleService roles) {
        this.tasks = tasks;
        this.photos = photos;
        this.events = events;
        this.storage = storage;
        this.governance = governance;
        this.roles = roles;
    }

    @Transactional
    public TaskDeliveryPhotoSummary uploadRequest(Long taskId, Long actor, MultipartFile file, String key) {
        return add(taskId, actor, file, key, null, REQUEST);
    }

    @Transactional
    public TaskDeliveryPhotoSummary upload(Long taskId, Long actor, MultipartFile file, String key, String note) {
        return add(taskId, actor, file, key, note, DELIVERY);
    }

    private TaskDeliveryPhotoSummary add(Long taskId, Long actor, MultipartFile file, String key, String note, String type) {
        RewardTask task = locked(taskId);
        boolean delivery = DELIVERY.equals(type);
        requireEditable(task, actor, delivery);
        if (key == null || key.isBlank() || key.length() > 128 || (note != null && note.length() > 1000)) {
            throw new BusinessException("请提供有效的 clientRequestId（最多 128 字符）和备注（最多 1000 字符）");
        }
        key = key.trim();
        var existing = photos.findByTaskIdAndEvidenceTypeAndUploaderIdAndClientRequestId(taskId, type, actor, key);
        if (existing.isPresent()) {
            if (!ACTIVE.equals(existing.get().getStatus())) throw new BusinessException("图片已删除，请使用新的请求编号");
            return TaskDeliveryPhotoSummary.from(existing.get());
        }
        // Independent quotas: 3 files per evidence type, plus 15 MiB across both types.
        if (photos.countByTaskIdAndEvidenceTypeAndStatus(taskId, type, ACTIVE) >= 3) {
            throw new BusinessException("每类图片最多上传 3 张");
        }
        Long storedBytes = photos.sumSizeBytesByTaskId(taskId);
        long existingBytes = storedBytes == null ? 0L : storedBytes;
        if (file == null || file.getSize() < 0 || existingBytes + file.getSize() > 15L * 1024 * 1024) {
            throw new BusinessException("任务图片总大小不能超过 15 MiB");
        }
        StoredTaskEvidence stored;
        try { stored = storage.store(file, taskId); }
        catch (IllegalArgumentException e) { throw new BusinessException(e.getMessage()); }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) storage.delete(stored.storageKey());
            }
        });
        var application = delivery ? task.getAcceptedApplication() : null;
        var uploader = delivery ? application.getApplicant() : task.getPublisher();
        var photo = photos.saveAndFlush(new TaskDeliveryPhoto(task, application, uploader, stored, type, key, note));
        if (delivery) events.save(new TaskEvent(task, uploader, "PHOTO_UPLOADED", null));
        return TaskDeliveryPhotoSummary.from(photo);
    }

    @Transactional
    public void remove(Long taskId, Long photoId, Long actor) {
        RewardTask task = locked(taskId);
        requireParticipant(task, actor);
        var photo = activePhoto(taskId, photoId);
        if (!actor.equals(photo.getUploader().getId())) throw forbidden();
        requireEditable(task, actor, DELIVERY.equals(photo.getEvidenceType()));
        photo.markDeleted();
        String key = photo.getStorageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(key); }
        });
    }

    @Transactional(readOnly = true)
    public List<TaskDeliveryPhotoSummary> listForParticipant(Long taskId, Long viewer) {
        requireParticipant(task(taskId), viewer);
        return photos.findByTaskIdAndStatusOrderByIdAsc(taskId, ACTIVE).stream().map(TaskDeliveryPhotoSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskEvidenceDownload download(Long taskId, Long photoId, Long viewer) {
        requireParticipant(task(taskId), viewer);
        var photo = activePhoto(taskId, photoId);
        return new TaskEvidenceDownload(storage.read(photo.getStorageKey()), photo.getContentType(), photo.getSizeBytes());
    }

    public boolean hasEvidence(Long taskId) {
        return photos.countByTaskIdAndEvidenceTypeAndStatus(taskId, DELIVERY, ACTIVE) > 0;
    }

    private TaskDeliveryPhoto activePhoto(Long taskId, Long id) {
        return photos.findByTaskIdAndIdAndStatus(taskId, id, ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在"));
    }

    private void requireEditable(RewardTask task, Long actor, boolean delivery) {
        if (delivery ? !isRunner(task, actor) : !task.getPublisher().getId().equals(actor)) throw forbidden();
        var state = task.getWorkflowStatus();
        if (delivery) {
            if (!TaskVerificationMode.PHOTO_AND_CONFIRMATION.name().equals(task.getVerificationMode()) || !List.of("DELIVERING", "DELIVERED").contains(state)) {
                throw new BusinessException("当前任务模式或阶段不能上传凭证");
            }
            if (!roles.hasRole(actor, "ROLE_RUNNER")) throw forbidden();
            governance.ensureCanProvideService(actor);
        } else if (!List.of("PUBLISHED", "ACCEPTED", "HEADING_TO_PICKUP", "PICKED_UP", "DELIVERING", "DELIVERED", "PENDING_CONFIRMATION").contains(state)) {
            throw new BusinessException("任务当前不可更改物品图片");
        }
    }

    private static boolean isRunner(RewardTask task, Long actor) {
        return task.getAcceptedApplication() != null && actor.equals(task.getAcceptedApplication().getApplicant().getId());
    }
    private static void requireParticipant(RewardTask task, Long actor) {
        if (!actor.equals(task.getPublisher().getId()) && !isRunner(task, actor)) throw forbidden();
    }
    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "只有任务参与方可访问，图片只能由本人更改");
    }
    private RewardTask locked(Long id) { return tasks.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("任务不存在")); }
    private RewardTask task(Long id) { return tasks.findById(id).orElseThrow(() -> new BusinessException("任务不存在")); }
}
