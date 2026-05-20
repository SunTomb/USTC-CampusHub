package com.campushub.task;

import com.campushub.common.BusinessException;
import com.campushub.notification.NotificationService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunnerTaskService {

    private final RewardTaskRepository rewardTaskRepository;
    private final TaskApplicationRepository taskApplicationRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RunnerTaskService(
            RewardTaskRepository rewardTaskRepository,
            TaskApplicationRepository taskApplicationRepository,
            TaskEventRepository taskEventRepository,
            TaskIssueRepository taskIssueRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskApplicationRepository = taskApplicationRepository;
        this.taskEventRepository = taskEventRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public RewardTaskSummary publish(Long publisherId, CreateRunnerTaskRequest request) {
        User publisher = findUser(publisherId);
        validateEnum(TaskAcceptanceMode.class, request.acceptanceMode(), "不支持的接单模式");
        validateEnum(CampusZone.class, request.originZone(), "不支持的起点校区");
        validateEnum(CampusZone.class, request.destinationZone(), "不支持的终点校区");
        validateEnum(TaskVerificationMode.class, request.verificationMode(), "不支持的完成确认方式");

        RewardTask task = new RewardTask(
                publisher,
                request.title(),
                request.description(),
                request.rewardAmount(),
                request.depositAmount() == null ? BigDecimal.ZERO : request.depositAmount(),
                buildTaskLocation(request),
                request.deadline(),
                "PUBLISHED");
        task.publishWorkflow(
                normalizeEnum(request.acceptanceMode()),
                normalizeEnum(request.originZone()),
                normalizeEnum(request.destinationZone()),
                trimToNull(request.originDetail()),
                trimToNull(request.destinationDetail()),
                normalizeEnum(request.verificationMode()));
        RewardTask saved = rewardTaskRepository.save(task);
        recordEvent(saved, publisher, "PUBLISHED", "发布跑腿任务");
        return RewardTaskSummary.from(saved);
    }

    @Transactional
    public RewardTaskSummary grab(Long taskId, Long runnerId) {
        RewardTask task = findTask(taskId);
        User runner = findUser(runnerId);
        ensureTaskMode(task, TaskAcceptanceMode.GRAB.name());
        ensureTaskPublished(task);

        TaskApplication application = taskApplicationRepository.save(new TaskApplication(task, runner, "抢单"));
        application.markAccepted();
        task.markAccepted(application);
        recordEvent(task, runner, "GRABBED", "抢单成功");
        notificationService.notify(task.getPublisher(), "任务已被接单", runner.getNickname() + " 已接下你的任务", "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public TaskApplicationSummary apply(Long taskId, Long applicantId, ApplyTaskRequest request) {
        RewardTask task = findTask(taskId);
        User applicant = findUser(applicantId);
        ensureTaskMode(task, TaskAcceptanceMode.APPLICATION.name());
        ensureTaskPublished(task);

        TaskApplication application = taskApplicationRepository.save(new TaskApplication(task, applicant, trimToNull(request.message())));
        recordEvent(task, applicant, "APPLIED", application.getMessage());
        notificationService.notify(task.getPublisher(), "收到接单申请", applicant.getNickname() + " 申请接你的任务", "TASK", task.getId());
        return TaskApplicationSummary.from(application);
    }

    @Transactional
    public TaskApplicationSummary acceptApplication(Long taskId, Long applicationId, Long publisherId) {
        RewardTask task = findTask(taskId);
        User publisher = findUser(publisherId);
        if (!task.getPublisher().getId().equals(publisher.getId())) {
            throw new BusinessException("只有发布者可以选择申请人");
        }
        ensureTaskMode(task, TaskAcceptanceMode.APPLICATION.name());
        ensureTaskPublished(task);

        TaskApplication application = taskApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("申请不存在"));
        if (!application.getTask().getId().equals(task.getId())) {
            throw new BusinessException("申请不属于该任务");
        }
        application.markAccepted();
        task.markAccepted(application);
        recordEvent(task, publisher, "APPLICATION_ACCEPTED", application.getApplicant().getNickname());
        notificationService.notify(application.getApplicant(), "接单申请已通过", "发布者已选择你接单", "TASK", task.getId());
        return TaskApplicationSummary.from(application);
    }

    @Transactional
    public RewardTaskSummary advance(Long taskId, Long actorId, String nextStatus, TaskActionRequest request) {
        RewardTask task = findTask(taskId);
        User actor = findUser(actorId);
        TaskWorkflowStatus status = validateEnum(TaskWorkflowStatus.class, nextStatus, "不支持的任务状态");
        task.moveTo(status.name());
        recordEvent(task, actor, "STATUS_CHANGED", request == null ? null : request.note());
        notificationService.notify(task.getPublisher(), "任务状态已更新", "任务状态更新为 " + status.name(), "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary completeWithCode(Long taskId, Long runnerId, TaskActionRequest request) {
        RewardTask task = findTask(taskId);
        User runner = findUser(runnerId);
        task.moveTo(TaskWorkflowStatus.COMPLETED.name());
        if (task.getAcceptedApplication() != null) {
            task.getAcceptedApplication().markCompleted();
        }
        recordEvent(task, runner, "COMPLETED_WITH_CODE", request == null ? null : request.note());
        notificationService.notify(task.getPublisher(), "任务已完成", "接单者已提交完成码并完成任务", "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary submitDeliveryPhoto(Long taskId, Long runnerId, TaskActionRequest request) {
        RewardTask task = findTask(taskId);
        User runner = findUser(runnerId);
        task.moveTo(TaskWorkflowStatus.PENDING_CONFIRMATION.name());
        recordEvent(task, runner, "PHOTO_SUBMITTED", request == null ? null : request.note());
        notificationService.notify(task.getPublisher(), "任务等待确认", "接单者已提交图片凭证", "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary confirmCompletion(Long taskId, Long publisherId, TaskActionRequest request) {
        RewardTask task = findTask(taskId);
        User publisher = findUser(publisherId);
        if (!task.getPublisher().getId().equals(publisher.getId())) {
            throw new BusinessException("只有发布者可以确认完成");
        }
        task.moveTo(TaskWorkflowStatus.COMPLETED.name());
        if (task.getAcceptedApplication() != null) {
            task.getAcceptedApplication().markCompleted();
        }
        recordEvent(task, publisher, "COMPLETION_CONFIRMED", request == null ? null : request.note());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary reportIssue(Long taskId, Long reporterId, ReportTaskIssueRequest request) {
        RewardTask task = findTask(taskId);
        User reporter = findUser(reporterId);
        taskIssueRepository.save(new TaskIssue(task, reporter, request.issueType(), request.description()));
        task.moveTo(TaskWorkflowStatus.ISSUE_HANDLING.name());
        recordEvent(task, reporter, "ISSUE_REPORTED", request.description());
        notificationService.notify(task.getPublisher(), "任务异常上报", "任务收到异常上报：" + request.issueType(), "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    private RewardTask findTask(Long taskId) {
        return rewardTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private void ensureTaskMode(RewardTask task, String mode) {
        if (!mode.equals(task.getAcceptanceMode())) {
            throw new BusinessException("任务接单模式不匹配");
        }
    }

    private void ensureTaskPublished(RewardTask task) {
        if (!TaskWorkflowStatus.PUBLISHED.name().equals(task.getWorkflowStatus())) {
            throw new BusinessException("任务当前不可接单");
        }
    }

    private void recordEvent(RewardTask task, User actor, String eventType, String note) {
        taskEventRepository.save(new TaskEvent(task, actor, eventType, note));
    }

    private String buildTaskLocation(CreateRunnerTaskRequest request) {
        return normalizeEnum(request.originZone()) + " -> " + normalizeEnum(request.destinationZone());
    }

    private <T extends Enum<T>> T validateEnum(Class<T> enumType, String value, String message) {
        try {
            return Enum.valueOf(enumType, normalizeEnum(value));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(message);
        }
    }

    private String normalizeEnum(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
