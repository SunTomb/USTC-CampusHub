package com.campushub.task;

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

    public RewardTaskController(RewardTaskRepository rewardTaskRepository, RunnerTaskService runnerTaskService) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.runnerTaskService = runnerTaskService;
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
            @RequestParam Long publisherId,
            @Valid @RequestBody CreateRunnerTaskRequest request) {
        return ApiResponse.ok(runnerTaskService.publish(publisherId, request));
    }

    @PostMapping("/{taskId}/grab")
    public ApiResponse<RewardTaskSummary> grab(@PathVariable Long taskId, @RequestParam Long runnerId) {
        return ApiResponse.ok(runnerTaskService.grab(taskId, runnerId));
    }

    @PostMapping("/{taskId}/applications")
    public ApiResponse<TaskApplicationSummary> apply(
            @PathVariable Long taskId,
            @RequestParam Long applicantId,
            @RequestBody ApplyTaskRequest request) {
        return ApiResponse.ok(runnerTaskService.apply(taskId, applicantId, request));
    }

    @PostMapping("/{taskId}/applications/{applicationId}/accept")
    public ApiResponse<TaskApplicationSummary> acceptApplication(
            @PathVariable Long taskId,
            @PathVariable Long applicationId,
            @RequestParam Long publisherId) {
        return ApiResponse.ok(runnerTaskService.acceptApplication(taskId, applicationId, publisherId));
    }

    @PostMapping("/{taskId}/workflow/{nextStatus}")
    public ApiResponse<RewardTaskSummary> advance(
            @PathVariable Long taskId,
            @PathVariable String nextStatus,
            @RequestParam Long actorId,
            @RequestBody(required = false) TaskActionRequest request) {
        return ApiResponse.ok(runnerTaskService.advance(taskId, actorId, nextStatus, request));
    }

    @PostMapping("/{taskId}/complete-code")
    public ApiResponse<RewardTaskSummary> completeWithCode(
            @PathVariable Long taskId,
            @RequestParam Long runnerId,
            @RequestBody(required = false) TaskActionRequest request) {
        return ApiResponse.ok(runnerTaskService.completeWithCode(taskId, runnerId, request));
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResponse<RewardTaskSummary> confirmCompletion(
            @PathVariable Long taskId,
            @RequestParam Long publisherId,
            @RequestBody(required = false) TaskActionRequest request) {
        return ApiResponse.ok(runnerTaskService.confirmCompletion(taskId, publisherId, request));
    }

    @PostMapping("/{taskId}/issues")
    public ApiResponse<RewardTaskSummary> reportIssue(
            @PathVariable Long taskId,
            @RequestParam Long reporterId,
            @Valid @RequestBody ReportTaskIssueRequest request) {
        return ApiResponse.ok(runnerTaskService.reportIssue(taskId, reporterId, request));
    }
}
