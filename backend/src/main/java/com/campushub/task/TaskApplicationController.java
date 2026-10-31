package com.campushub.task;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task-applications")
public class TaskApplicationController {

    private final TaskApplicationRepository taskApplicationRepository;
    private final RewardTaskRepository rewardTaskRepository;
    private final CurrentUserService currentUserService;

    public TaskApplicationController(
            TaskApplicationRepository taskApplicationRepository,
            RewardTaskRepository rewardTaskRepository,
            CurrentUserService currentUserService) {
        this.taskApplicationRepository = taskApplicationRepository;
        this.rewardTaskRepository = rewardTaskRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<TaskApplicationSummary>> listApplications() {
        currentUserService.requireUserId();
        if (!currentUserService.isTradeReader()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        List<TaskApplicationSummary> applications = taskApplicationRepository.findAll().stream()
                .map(TaskApplicationSummary::from)
                .toList();
        return ApiResponse.ok(applications);
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskApplicationSummary> getApplication(@PathVariable Long id) {
        TaskApplication application = taskApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("task application not found"));
        requireParticipantOrAdmin(application);
        return ApiResponse.ok(TaskApplicationSummary.from(application));
    }

    private void requireParticipantOrAdmin(TaskApplication application) {
        Long currentUserId = currentUserService.requireUserId();
        if (!currentUserId.equals(application.getApplicant().getId())
                && !currentUserId.equals(application.getTask().getPublisher().getId())
                && !currentUserService.isTradeReader()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<TaskApplicationSummary>> listTaskApplications(@PathVariable Long taskId) {
        RewardTask task = rewardTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("task not found"));
        Long currentUserId = currentUserService.requireUserId();
        if (!currentUserId.equals(task.getPublisher().getId()) && !currentUserService.isTradeReader()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
        List<TaskApplicationSummary> applications = taskApplicationRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(TaskApplicationSummary::from)
                .toList();
        return ApiResponse.ok(applications);
    }

    @GetMapping("/applicant/{applicantId}")
    public ApiResponse<List<TaskApplicationSummary>> listApplicantApplications(@PathVariable Long applicantId) {
        Long effectiveApplicantId = currentUserService.requireSameUser(applicantId);
        List<TaskApplicationSummary> applications = taskApplicationRepository
                .findByApplicantIdOrderByCreatedAtDesc(effectiveApplicantId).stream()
                .map(TaskApplicationSummary::from)
                .toList();
        return ApiResponse.ok(applications);
    }
}
