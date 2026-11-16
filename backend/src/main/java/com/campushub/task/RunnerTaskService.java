package com.campushub.task;

import com.campushub.common.BusinessException;
import com.campushub.auth.UserRoleService;
import com.campushub.moderation.GovernanceService;
import com.campushub.notification.NotificationService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunnerTaskService {

    private static final int CODE_TTL_MINUTES = 30;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RewardTaskRepository rewardTaskRepository;
    private final TaskApplicationRepository taskApplicationRepository;
    private final TaskEventRepository taskEventRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GovernanceService governanceService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    public RunnerTaskService(
            RewardTaskRepository rewardTaskRepository,
            TaskApplicationRepository taskApplicationRepository,
            TaskEventRepository taskEventRepository,
            TaskIssueRepository taskIssueRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            GovernanceService governanceService,
            UserRoleService userRoleService,
            PasswordEncoder passwordEncoder) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskApplicationRepository = taskApplicationRepository;
        this.taskEventRepository = taskEventRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.governanceService = governanceService;
        this.userRoleService = userRoleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<RewardTaskSummary> listByPublisher(Long publisherId) {
        return rewardTaskRepository.findByPublisherIdOrderByDeadlineAsc(publisherId).stream()
                .map(RewardTaskSummary::from)
                .toList();
    }
    @Transactional
    public RewardTaskSummary publish(Long publisherId, CreateRunnerTaskRequest request) {
        User publisher = findUser(publisherId);
        governanceService.ensureCanPost(publisherId);
        if (request.rewardAmount() == null || request.rewardAmount().signum() < 0) {
            throw new BusinessException("任务报酬不能为负数");
        }
        if (request.depositAmount() != null && request.depositAmount().signum() != 0) {
            throw new BusinessException("跑腿任务不支持收取或冻结保证金");
        }
        if (request.deadline() == null || !request.deadline().isAfter(LocalDateTime.now())) {
            throw new BusinessException("任务截止时间必须晚于当前时间");
        }
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
        ensureRunner(runnerId);
        RewardTask task = findTaskForUpdate(taskId);
        User runner = findUser(runnerId);
        if (task.getPublisher().getId().equals(runnerId)) throw new BusinessException("不能接取自己的任务");
        governanceService.ensureCanProvideService(runnerId);
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
        RewardTask task = findTaskForUpdate(taskId);
        User applicant = findUser(applicantId);
        ensureRunner(applicantId);
        governanceService.ensureCanProvideService(applicantId);
        ensureTaskMode(task, TaskAcceptanceMode.APPLICATION.name());
        ensureTaskPublished(task);
        if (task.getPublisher().getId().equals(applicantId)) {
            throw new BusinessException("不能申请自己的任务");
        }
        var existing = taskApplicationRepository.findByTaskIdAndApplicantId(taskId, applicantId);
        if (existing.isPresent()) return TaskApplicationSummary.from(existing.get());

        TaskApplication application = taskApplicationRepository.save(new TaskApplication(task, applicant, trimToNull(request.message())));
        recordEvent(task, applicant, "APPLIED", application.getMessage());
        notificationService.notify(task.getPublisher(), "收到接单申请", applicant.getNickname() + " 申请接你的任务", "TASK", task.getId());
        return TaskApplicationSummary.from(application);
    }

    @Transactional
    public TaskApplicationSummary acceptApplication(Long taskId, Long applicationId, Long publisherId) {
        RewardTask task = findTaskForUpdate(taskId);
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
        if (!"PENDING".equals(application.getStatus()) && !"APPLIED".equals(application.getStatus())) {
            throw new BusinessException("该申请当前不可接受");
        }
        Long applicantId = application.getApplicant().getId();
        if (task.getPublisher().getId().equals(applicantId)) {
            throw new BusinessException("不能选择发布者本人");
        }
        ensureRunner(applicantId);
        governanceService.ensureCanProvideService(applicantId);
        taskApplicationRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .filter(other -> !other.getId().equals(applicationId))
                .filter(other -> "PENDING".equals(other.getStatus()) || "APPLIED".equals(other.getStatus()))
                .forEach(TaskApplication::markRejected);
        application.markAccepted();
        task.markAccepted(application);
        recordEvent(task, publisher, "APPLICATION_ACCEPTED", application.getApplicant().getNickname());
        notificationService.notify(application.getApplicant(), "接单申请已通过", "发布者已选择你接单", "TASK", task.getId());
        return TaskApplicationSummary.from(application);
    }

    @Transactional
    public RewardTaskSummary advance(Long taskId, Long actorId, String nextStatus, TaskActionRequest request) {
        RewardTask task = findTaskForUpdate(taskId);
        ensureAssignedRunner(task, actorId);
        User actor = findUser(actorId);
        TaskWorkflowStatus status = validateEnum(TaskWorkflowStatus.class, nextStatus, "不支持的任务状态");
        ensureLegalTransition(task.getWorkflowStatus(), status);
        task.moveTo(status.name());
        recordEvent(task, actor, "STATUS_CHANGED", request == null ? null : request.note());
        notificationService.notify(task.getPublisher(), "任务状态已更新", "任务状态更新为 " + status.name(), "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    // Rejected codes must commit their attempt counter, without committing other business failures.
    @Transactional(noRollbackFor = CompletionCodeRejectedException.class)
    public RewardTaskSummary completeWithCode(Long taskId, Long runnerId, TaskActionRequest request) {
        RewardTask task = findTaskForUpdate(taskId);
        ensureAssignedRunner(task, runnerId);
        ensurePendingConfirmation(task);
        ensureCodeMode(task);
        validateCompletionCode(task, request == null ? null : request.completionCode());
        User runner = findUser(runnerId);
        task.clearCompletionCode();
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
        RewardTask task = findTaskForUpdate(taskId);
        ensureAssignedRunner(task, runnerId);
        ensureLegalTransition(task.getWorkflowStatus(), TaskWorkflowStatus.PENDING_CONFIRMATION);
        User runner = findUser(runnerId);
        task.moveTo(TaskWorkflowStatus.PENDING_CONFIRMATION.name());
        recordEvent(task, runner, "PHOTO_SUBMITTED", request == null ? null : request.note());
        notificationService.notify(task.getPublisher(), "任务等待确认", "接单者已提交图片凭证", "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary confirmCompletion(Long taskId, Long publisherId, TaskActionRequest request) {
        RewardTask task = findTaskForUpdate(taskId);
        User publisher = findUser(publisherId);
        if (!task.getPublisher().getId().equals(publisher.getId())) {
            throw new BusinessException("只有发布者可以确认完成");
        }
        ensurePendingConfirmation(task);
        task.clearCompletionCode();
        task.moveTo(TaskWorkflowStatus.COMPLETED.name());
        if (task.getAcceptedApplication() != null) {
            task.getAcceptedApplication().markCompleted();
        }
        recordEvent(task, publisher, "COMPLETION_CONFIRMED", request == null ? null : request.note());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary reportIssue(Long taskId, Long reporterId, ReportTaskIssueRequest request) {
        RewardTask task = findTaskForUpdate(taskId);
        ensureParticipant(task, reporterId);
        ensureActiveAssignment(task);
        User reporter = findUser(reporterId);
        taskIssueRepository.save(new TaskIssue(task, reporter, request.issueType(), request.description()));
        task.clearCompletionCode();
        task.moveTo(TaskWorkflowStatus.ISSUE_HANDLING.name());
        recordEvent(task, reporter, "ISSUE_REPORTED", request.description());
        notificationService.notify(task.getPublisher(), "任务异常上报", "任务收到异常上报：" + request.issueType(), "TASK", task.getId());
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public RewardTaskSummary cancelByAdmin(Long taskId, Long adminId, String note) {
        RewardTask task = findTaskForUpdate(taskId);
        if (TaskWorkflowStatus.COMPLETED.name().equals(task.getWorkflowStatus())
                || TaskWorkflowStatus.CANCELLED.name().equals(task.getWorkflowStatus())) {
            throw new BusinessException("任务已处于终态，不能重复关闭");
        }
        User admin = findUser(adminId);
        task.clearCompletionCode();
        task.moveTo(TaskWorkflowStatus.CANCELLED.name());
        recordEvent(task, admin, "CANCELLED_BY_ADMIN", trimToNull(note));
        return RewardTaskSummary.from(task);
    }

    @Transactional
    public CompletionCodeResponse issueCompletionCode(Long taskId, Long publisherId) {
        RewardTask task = findTaskForUpdate(taskId);
        if (!task.getPublisher().getId().equals(publisherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有发布者可以生成完成码");
        }
        ensureActiveAssignment(task);
        ensureCodeMode(task);
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime issuedAt = LocalDateTime.now();
        task.issueCompletionCode(passwordEncoder.encode(code), issuedAt);
        recordEvent(task, task.getPublisher(), "COMPLETION_CODE_ISSUED", "发布者生成新的完成码");
        return new CompletionCodeResponse(code, issuedAt.plusMinutes(CODE_TTL_MINUTES));
    }

    @Transactional(readOnly = true)
    public TaskWorkspaceSummary workspace(Long taskId, Long viewerId) {
        RewardTask task = rewardTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        ensureParticipant(task, viewerId);
        TaskParticipantContact runner = task.getAcceptedApplication() == null ? null
                : TaskParticipantContact.from(task.getAcceptedApplication().getApplicant());
        List<TaskEventSummary> events = taskEventRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .filter(event -> isEventVisibleTo(event, task, viewerId))
                .map(TaskEventSummary::from)
                .toList();
        return new TaskWorkspaceSummary(RewardTaskSummary.from(task),
                TaskParticipantContact.from(task.getPublisher()), runner, events);
    }

    private boolean isEventVisibleTo(TaskEvent event, RewardTask task, Long viewerId) {
        if (task.getPublisher().getId().equals(viewerId)) return true;
        if ("APPLIED".equals(event.getEventType())) {
            return event.getActor() != null && viewerId.equals(event.getActor().getId());
        }
        return true;
    }

    private void ensureParticipant(RewardTask task, Long userId) {
        if (!task.getPublisher().getId().equals(userId) && (task.getAcceptedApplication() == null
                || !task.getAcceptedApplication().getApplicant().getId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有任务参与方可以访问工作区");
        }
    }

    private void ensureActiveAssignment(RewardTask task) {
        if (task.getAcceptedApplication() == null || !List.of("ACCEPTED", "HEADING_TO_PICKUP",
                "PICKED_UP", "DELIVERING", "DELIVERED", "PENDING_CONFIRMATION").contains(task.getWorkflowStatus())) {
            throw new BusinessException("任务当前不在履约中");
        }
    }

    private void ensurePendingConfirmation(RewardTask task) {
        if (!TaskWorkflowStatus.PENDING_CONFIRMATION.name().equals(task.getWorkflowStatus())
                || task.getAcceptedApplication() == null) {
            throw new BusinessException("任务尚未进入待确认状态");
        }
    }

    private void ensureCodeMode(RewardTask task) {
        if (!TaskVerificationMode.COMPLETION_CODE.name().equals(task.getVerificationMode())) {
            throw new BusinessException("该任务不使用完成码核销");
        }
    }

    private void ensureLegalTransition(String current, TaskWorkflowStatus next) {
        boolean legal = switch (current) {
            case "ACCEPTED" -> next == TaskWorkflowStatus.HEADING_TO_PICKUP;
            case "HEADING_TO_PICKUP" -> next == TaskWorkflowStatus.PICKED_UP;
            case "PICKED_UP" -> next == TaskWorkflowStatus.DELIVERING;
            case "DELIVERING" -> next == TaskWorkflowStatus.DELIVERED || next == TaskWorkflowStatus.PENDING_CONFIRMATION;
            case "DELIVERED" -> next == TaskWorkflowStatus.PENDING_CONFIRMATION;
            default -> false;
        };
        if (!legal) throw new BusinessException("请按履约顺序更新任务，终态或处理中任务不可推进");
    }

    private void validateCompletionCode(RewardTask task, String code) {
        if (task.getCompletionCodeHash() == null || task.getCompletionCodeIssuedAt() == null) {
            throw new CompletionCodeRejectedException("完成码未生成，请让发布者重新生成");
        }
        if (!task.getCompletionCodeIssuedAt().plusMinutes(CODE_TTL_MINUTES).isAfter(LocalDateTime.now())) {
            task.clearCompletionCode();
            throw new CompletionCodeRejectedException("完成码已过期，请让发布者重新生成");
        }
        if (task.getCompletionCodeAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new CompletionCodeRejectedException("错误次数已达上限，请让发布者重新生成");
        }
        if (code == null || !code.matches("[0-9]{6}")
                || !passwordEncoder.matches(code, task.getCompletionCodeHash())) {
            int attempts = task.registerCompletionCodeAttempt();
            throw new CompletionCodeRejectedException(attempts >= MAX_CODE_ATTEMPTS
                    ? "错误次数已达上限，请让发布者重新生成" : "完成码错误，请重试");
        }
    }

    private RewardTask findTaskForUpdate(Long taskId) {
        return rewardTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private void ensureAssignedRunner(RewardTask task, Long userId) {
        ensureRunner(userId);
        if (task.getAcceptedApplication() == null || !task.getAcceptedApplication().getApplicant().getId().equals(userId)) {
            throw new BusinessException("只有本单接单者可以更新履约进度");
        }
        governanceService.ensureCanProvideService(userId);
    }

    private void ensureRunner(Long userId) {
        if (!userRoleService.hasRole(userId, "ROLE_RUNNER")) {
            throw new BusinessException("请先阅读并同意顺路跑腿服务协议");
        }
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
        if (task.getDeadline() == null || !task.getDeadline().isAfter(LocalDateTime.now())) {
            throw new BusinessException("任务已过截止时间");
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
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
