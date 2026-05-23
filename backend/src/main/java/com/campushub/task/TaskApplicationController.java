package com.campushub.task;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task-applications")
public class TaskApplicationController {

    private final TaskApplicationRepository taskApplicationRepository;
    private final CurrentUserService currentUserService;

    public TaskApplicationController(TaskApplicationRepository taskApplicationRepository, CurrentUserService currentUserService) {
        this.taskApplicationRepository = taskApplicationRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<TaskApplicationSummary>> listApplications() {
        List<TaskApplicationSummary> applications = taskApplicationRepository.findAll().stream()
                .map(TaskApplicationSummary::from)
                .toList();
        return ApiResponse.ok(applications);
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskApplicationSummary> getApplication(@PathVariable Long id) {
        TaskApplication application = taskApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("task application not found"));
        return ApiResponse.ok(TaskApplicationSummary.from(application));
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<TaskApplicationSummary>> listTaskApplications(@PathVariable Long taskId) {
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
