package com.campushub.ops;

import com.campushub.common.ApiResponse;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.identity.RoleApplicationSummary;
import com.campushub.task.RewardTask;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.RewardTaskSummary;
import com.campushub.task.TaskIssueRepository;
import com.campushub.task.TaskIssueSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ops")
public class OperationsController {

    private final RewardTaskRepository rewardTaskRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final RoleApplicationRepository roleApplicationRepository;

    public OperationsController(
            RewardTaskRepository rewardTaskRepository,
            TaskIssueRepository taskIssueRepository,
            RoleApplicationRepository roleApplicationRepository) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.roleApplicationRepository = roleApplicationRepository;
    }

    @GetMapping("/dashboard")
    public ApiResponse<OperationsDashboardSummary> dashboard() {
        List<RewardTask> tasks = rewardTaskRepository.findAll();
        long publishedTasks = tasks.stream().filter(task -> "PUBLISHED".equals(task.getWorkflowStatus())).count();
        long acceptedTasks = tasks.stream().filter(task -> "ACCEPTED".equals(task.getWorkflowStatus())).count();
        long completedTasks = tasks.stream().filter(task -> "COMPLETED".equals(task.getWorkflowStatus())).count();
        long openIssues = taskIssueRepository.findByStatusOrderByCreatedAtAsc("OPEN").size();
        long pendingRoleApplications = roleApplicationRepository.findByReviewStatusOrderByCreatedAtAsc("PENDING_REVIEW").size();
        return ApiResponse.ok(new OperationsDashboardSummary(publishedTasks, acceptedTasks, completedTasks, openIssues, pendingRoleApplications));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<RewardTaskSummary>> tasks() {
        return ApiResponse.ok(rewardTaskRepository.findAll().stream()
                .map(RewardTaskSummary::from)
                .toList());
    }

    @GetMapping("/task-issues")
    public ApiResponse<List<TaskIssueSummary>> taskIssues() {
        return ApiResponse.ok(taskIssueRepository.findByStatusOrderByCreatedAtAsc("OPEN").stream()
                .map(TaskIssueSummary::from)
                .toList());
    }

    @GetMapping("/role-applications")
    public ApiResponse<List<RoleApplicationSummary>> roleApplications() {
        return ApiResponse.ok(roleApplicationRepository.findByReviewStatusOrderByCreatedAtAsc("PENDING_REVIEW").stream()
                .map(RoleApplicationSummary::from)
                .toList());
    }
}
