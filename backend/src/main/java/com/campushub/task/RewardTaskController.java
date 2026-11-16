package com.campushub.task;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class RewardTaskController {

    private final RewardTaskRepository rewardTaskRepository;
    private final RunnerTaskService runnerTaskService;
    private final CurrentUserService currentUserService;

    public RewardTaskController(
            RewardTaskRepository rewardTaskRepository,
            RunnerTaskService runnerTaskService,
            CurrentUserService currentUserService) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.runnerTaskService = runnerTaskService;
        this.currentUserService = currentUserService;
    }

    private Long requireActor(Long requestedId) {
        Long currentId = currentUserService.requireUserId();
        if (requestedId != null && !currentId.equals(requestedId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "只能由本人执行任务操作");
        }
        return currentId;
    }

    @GetMapping
    public ApiResponse<List<RewardTaskSummary>> listTasks() {
        List<RewardTaskSummary> tasks = rewardTaskRepository.findByStatusOrderByDeadlineAsc("PUBLISHED").stream()
                .map(RewardTaskSummary::from)
                .toList();
        return ApiResponse.ok(tasks);
    }

    @GetMapping("/mine")
    public ApiResponse<List<RewardTaskSummary>> myTasks() {
        return ApiResponse.ok(runnerTaskService.listByPublisher(currentUserService.requireUserId()));
    }

    @GetMapping("/{taskId}/workspace")
    public ApiResponse<TaskWorkspaceSummary> workspace(@PathVariable Long taskId) {
        return ApiResponse.ok(runnerTaskService.workspace(taskId, currentUserService.requireUserId()));
    }

    @PostMapping("/{taskId}/completion-code")
    public ApiResponse<CompletionCodeResponse> issueCompletionCode(@PathVariable Long taskId) {
        return ApiResponse.ok(runnerTaskService.issueCompletionCode(taskId, currentUserService.requireUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<RewardTaskSummary> getTask(@PathVariable Long id) {
        RewardTask task = rewardTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("task not found"));
        return ApiResponse.ok(RewardTaskSummary.from(task));
    }

    @PostMapping
    public ApiResponse<RewardTaskSummary> publish(
            @RequestParam(required = false) Long publisherId,
            @Valid @RequestBody CreateRunnerTaskRequest request) {
        Long effectivePublisherId = requireActor(publisherId);
        return ApiResponse.ok(runnerTaskService.publish(effectivePublisherId, request));
    }

    @PostMapping("/{taskId}/grab")
    public ApiResponse<RewardTaskSummary> grab(@PathVariable Long taskId, @RequestParam(required = false) Long runnerId) {
        Long effectiveRunnerId = requireActor(runnerId);
        return ApiResponse.ok(runnerTaskService.grab(taskId, effectiveRunnerId));
    }

    @PostMapping("/{taskId}/applications")
    public ApiResponse<TaskApplicationSummary> apply(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long applicantId,
            @RequestBody ApplyTaskRequest request) {
        Long effectiveApplicantId = requireActor(applicantId);
        return ApiResponse.ok(runnerTaskService.apply(taskId, effectiveApplicantId, request));
    }

    @PostMapping("/{taskId}/applications/{applicationId}/accept")
    public ApiResponse<TaskApplicationSummary> acceptApplication(
            @PathVariable Long taskId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long publisherId) {
        Long effectivePublisherId = requireActor(publisherId);
        return ApiResponse.ok(runnerTaskService.acceptApplication(taskId, applicationId, effectivePublisherId));
    }

    @PostMapping("/{taskId}/workflow/{nextStatus}")
    public ApiResponse<RewardTaskSummary> advance(
            @PathVariable Long taskId,
            @PathVariable String nextStatus,
            @RequestParam(required = false) Long actorId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectiveActorId = requireActor(actorId);
        return ApiResponse.ok(runnerTaskService.advance(taskId, effectiveActorId, nextStatus, request));
    }

    @PostMapping("/{taskId}/complete-code")
    public ApiResponse<RewardTaskSummary> completeWithCode(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long runnerId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectiveRunnerId = requireActor(runnerId);
        return ApiResponse.ok(runnerTaskService.completeWithCode(taskId, effectiveRunnerId, request));
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResponse<RewardTaskSummary> confirmCompletion(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long publisherId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectivePublisherId = requireActor(publisherId);
        return ApiResponse.ok(runnerTaskService.confirmCompletion(taskId, effectivePublisherId, request));
    }

    @PostMapping("/{taskId}/issues")
    public ApiResponse<RewardTaskSummary> reportIssue(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long reporterId,
            @Valid @RequestBody ReportTaskIssueRequest request) {
        Long effectiveReporterId = requireActor(reporterId);
        return ApiResponse.ok(runnerTaskService.reportIssue(taskId, effectiveReporterId, request));
    }
}
