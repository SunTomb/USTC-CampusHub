package com.campushub.ops;

import com.campushub.common.ApiResponse;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.identity.RoleApplicationSummary;
import com.campushub.projectad.ProjectAdReviewRequest;
import com.campushub.projectad.ProjectAdService;
import com.campushub.projectad.ProjectAdSummary;
import com.campushub.shop.ServiceOrderRepository;
import com.campushub.shop.ServiceOrderSummary;
import com.campushub.task.RewardTask;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.RewardTaskSummary;
import com.campushub.task.TaskIssueRepository;
import com.campushub.task.TaskIssueSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ops")
public class OperationsController {

    private final RewardTaskRepository rewardTaskRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ProjectAdService projectAdService;

    public OperationsController(
            RewardTaskRepository rewardTaskRepository,
            TaskIssueRepository taskIssueRepository,
            RoleApplicationRepository roleApplicationRepository,
            ServiceOrderRepository serviceOrderRepository,
            ProjectAdService projectAdService) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.projectAdService = projectAdService;
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

    @GetMapping("/shop-orders")
    public ApiResponse<List<ServiceOrderSummary>> shopOrders() {
        return ApiResponse.ok(serviceOrderRepository.findAll().stream()
                .map(ServiceOrderSummary::from)
                .toList());
    }

    @GetMapping("/project-ads")
    public ApiResponse<List<ProjectAdSummary>> projectAds(@RequestParam(required = false) String status) {
        return ApiResponse.ok(projectAdService.listForAdmin(status));
    }

    @PostMapping("/project-ads/{id}/approve")
    public ApiResponse<ProjectAdSummary> approveProjectAd(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.approve(id, adminId, request));
    }

    @PostMapping("/project-ads/{id}/reject")
    public ApiResponse<ProjectAdSummary> rejectProjectAd(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.reject(id, adminId, request));
    }

    @PostMapping("/project-ads/{id}/feature")
    public ApiResponse<ProjectAdSummary> featureProjectAd(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.feature(id, adminId, request));
    }

    @PostMapping("/project-ads/{id}/unfeature")
    public ApiResponse<ProjectAdSummary> unfeatureProjectAd(@PathVariable Long id, @RequestParam Long adminId) {
        return ApiResponse.ok(projectAdService.unfeature(id, adminId));
    }

    @PostMapping("/project-ads/{id}/block")
    public ApiResponse<ProjectAdSummary> blockProjectAd(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.block(id, adminId, request));
    }
}
