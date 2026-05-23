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

    @GetMapping
    public ApiResponse<List<RewardTaskSummary>> listTasks() {
        List<RewardTaskSummary> tasks = rewardTaskRepository.findByStatusOrderByDeadlineAsc("PUBLISHED").stream()
                .map(RewardTaskSummary::from)
                .toList();
        return ApiResponse.ok(tasks);
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
        Long effectivePublisherId = publisherId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(publisherId);
        return ApiResponse.ok(runnerTaskService.publish(effectivePublisherId, request));
    }

    @PostMapping("/{taskId}/grab")
    public ApiResponse<RewardTaskSummary> grab(@PathVariable Long taskId, @RequestParam(required = false) Long runnerId) {
        Long effectiveRunnerId = runnerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(runnerId);
        return ApiResponse.ok(runnerTaskService.grab(taskId, effectiveRunnerId));
    }

    @PostMapping("/{taskId}/applications")
    public ApiResponse<TaskApplicationSummary> apply(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long applicantId,
            @RequestBody ApplyTaskRequest request) {
        Long effectiveApplicantId = applicantId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(applicantId);
        return ApiResponse.ok(runnerTaskService.apply(taskId, effectiveApplicantId, request));
    }

    @PostMapping("/{taskId}/applications/{applicationId}/accept")
    public ApiResponse<TaskApplicationSummary> acceptApplication(
            @PathVariable Long taskId,
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long publisherId) {
        Long effectivePublisherId = publisherId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(publisherId);
        return ApiResponse.ok(runnerTaskService.acceptApplication(taskId, applicationId, effectivePublisherId));
    }

    @PostMapping("/{taskId}/workflow/{nextStatus}")
    public ApiResponse<RewardTaskSummary> advance(
            @PathVariable Long taskId,
            @PathVariable String nextStatus,
            @RequestParam(required = false) Long actorId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectiveActorId = actorId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(actorId);
        return ApiResponse.ok(runnerTaskService.advance(taskId, effectiveActorId, nextStatus, request));
    }

    @PostMapping("/{taskId}/complete-code")
    public ApiResponse<RewardTaskSummary> completeWithCode(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long runnerId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectiveRunnerId = runnerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(runnerId);
        return ApiResponse.ok(runnerTaskService.completeWithCode(taskId, effectiveRunnerId, request));
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResponse<RewardTaskSummary> confirmCompletion(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long publisherId,
            @RequestBody(required = false) TaskActionRequest request) {
        Long effectivePublisherId = publisherId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(publisherId);
        return ApiResponse.ok(runnerTaskService.confirmCompletion(taskId, effectivePublisherId, request));
    }

    @PostMapping("/{taskId}/issues")
    public ApiResponse<RewardTaskSummary> reportIssue(
            @PathVariable Long taskId,
            @RequestParam(required = false) Long reporterId,
            @Valid @RequestBody ReportTaskIssueRequest request) {
        Long effectiveReporterId = reporterId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(reporterId);
        return ApiResponse.ok(runnerTaskService.reportIssue(taskId, effectiveReporterId, request));
    }
}
