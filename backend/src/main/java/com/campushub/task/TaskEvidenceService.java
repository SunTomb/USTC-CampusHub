package com.campushub.task;

import com.campushub.common.BusinessException;
import com.campushub.file.StoredTaskEvidence;
import com.campushub.file.TaskDeliveryPhoto;
import com.campushub.file.TaskDeliveryPhotoRepository;
import com.campushub.file.TaskDeliveryPhotoSummary;
import com.campushub.file.TaskEvidenceDownload;
import com.campushub.file.TaskEvidenceStorage;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskEvidenceService {

    private static final long MAX_TASK_BYTES = 15L * 1024 * 1024;
    private static final int MAX_PHOTOS = 3;
    private static final List<String> UPLOADABLE_STAGES = List.of(
            "ACCEPTED", "HEADING_TO_PICKUP", "PICKED_UP", "DELIVERING", "DELIVERED");

    private final RewardTaskRepository rewardTaskRepository;
    private final TaskDeliveryPhotoRepository photoRepository;
    private final TaskEventRepository taskEventRepository;
    private final UserRepository userRepository;
    private final TaskEvidenceStorage storage;

    public TaskEvidenceService(
            RewardTaskRepository rewardTaskRepository,
            TaskDeliveryPhotoRepository photoRepository,
            TaskEventRepository taskEventRepository,
            UserRepository userRepository,
            TaskEvidenceStorage storage) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.photoRepository = photoRepository;
        this.taskEventRepository = taskEventRepository;
        this.userRepository = userRepository;
        this.storage = storage;
    }

    @Transactional
    public TaskDeliveryPhotoSummary upload(Long taskId, Long uploaderId, MultipartFile file,
            String clientRequestId, String note) {
        String idempotencyKey = normalizeClientRequestId(clientRequestId);
        RewardTask task = findTaskForUpdate(taskId);
        TaskDeliveryPhoto existing = photoRepository
                .findByTaskIdAndUploaderIdAndClientRequestId(taskId, uploaderId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return TaskDeliveryPhotoSummary.from(existing);
        }
        TaskApplication application = ensureRunnerCanUpload(task, uploaderId);
        if (photoRepository.countByTaskId(taskId) >= MAX_PHOTOS) {
            throw new BusinessException("每个任务最多上传 3 张凭证图片");
        }
        Long storedBytes = photoRepository.sumSizeBytesByTaskId(taskId);
        long existingBytes = storedBytes == null ? 0 : storedBytes;
        if (file == null || file.getSize() < 0 || existingBytes + file.getSize() > MAX_TASK_BYTES) {
            throw new BusinessException("任务凭证图片总大小不能超过 15 MiB");
        }
        User uploader = findUser(uploaderId);
        StoredTaskEvidence stored = storage.store(file, taskId);
        if (existingBytes + stored.sizeBytes() > MAX_TASK_BYTES) {
            storage.delete(stored.storageKey());
            throw new BusinessException("任务凭证图片总大小不能超过 15 MiB");
        }
        try {
            TaskDeliveryPhoto photo = photoRepository.saveAndFlush(new TaskDeliveryPhoto(
                    task, application, uploader, stored, idempotencyKey, trimToNull(note)));
            taskEventRepository.save(new TaskEvent(task, uploader, "PHOTO_UPLOADED", trimToNull(note)));
            return TaskDeliveryPhotoSummary.from(photo);
        } catch (RuntimeException exception) {
            storage.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<TaskDeliveryPhotoSummary> listForParticipant(Long taskId, Long viewerId) {
        RewardTask task = findTask(taskId);
        ensureParticipant(task, viewerId);
        return photoRepository.findByTaskIdAndStatusOrderByCreatedAtAsc(taskId, "UPLOADED")
                .stream().map(TaskDeliveryPhotoSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskEvidenceDownload download(Long taskId, Long photoId, Long viewerId) {
        TaskDeliveryPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException("凭证图片不存在"));
        if (!photo.getTask().getId().equals(taskId) || !"UPLOADED".equals(photo.getStatus())) {
            throw new BusinessException("凭证图片不存在");
        }
        ensureParticipant(photo.getTask(), viewerId);
        byte[] bytes = storage.read(photo.getStorageKey());
        return new TaskEvidenceDownload(bytes, photo.getContentType(), photo.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public boolean hasEvidence(Long taskId) {
        return photoRepository.countByTaskId(taskId) > 0;
    }

    private TaskApplication ensureRunnerCanUpload(RewardTask task, Long uploaderId) {
        if (!TaskVerificationMode.PHOTO_AND_CONFIRMATION.name().equals(task.getVerificationMode())) {
            throw new BusinessException("该任务不使用图片凭证");
        }
        if (!UPLOADABLE_STAGES.contains(task.getWorkflowStatus())) {
            throw new BusinessException("任务已结束或当前阶段不能上传凭证");
        }
        TaskApplication application = task.getAcceptedApplication();
        if (application == null || !application.getApplicant().getId().equals(uploaderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有已接单的跑腿者可以上传凭证");
        }
        return application;
    }

    private void ensureParticipant(RewardTask task, Long viewerId) {
        boolean publisher = task.getPublisher().getId().equals(viewerId);
        boolean runner = task.getAcceptedApplication() != null
                && task.getAcceptedApplication().getApplicant().getId().equals(viewerId);
        if (!publisher && !runner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有任务参与方可以访问凭证");
        }
    }

    private RewardTask findTask(Long taskId) {
        return rewardTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private RewardTask findTaskForUpdate(Long taskId) {
        return rewardTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private String normalizeClientRequestId(String clientRequestId) {
        String normalized = trimToNull(clientRequestId);
        if (normalized == null) {
            throw new BusinessException("clientRequestId 不能为空");
        }
        if (normalized.length() > 128) {
            throw new BusinessException("clientRequestId 不能超过 128 个字符");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
