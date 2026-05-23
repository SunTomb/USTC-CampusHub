package com.campushub.ops;

import com.campushub.auth.CurrentUserService;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final OperationsAnalyticsService operationsAnalyticsService;
    private final AnalyticsDateRangeParser analyticsDateRangeParser;
    private final CurrentUserService currentUserService;

    public OperationsController(
            RewardTaskRepository rewardTaskRepository,
            TaskIssueRepository taskIssueRepository,
            RoleApplicationRepository roleApplicationRepository,
            ServiceOrderRepository serviceOrderRepository,
            ProjectAdService projectAdService,
            OperationsAnalyticsService operationsAnalyticsService,
            AnalyticsDateRangeParser analyticsDateRangeParser,
            CurrentUserService currentUserService) {
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.projectAdService = projectAdService;
        this.operationsAnalyticsService = operationsAnalyticsService;
        this.analyticsDateRangeParser = analyticsDateRangeParser;
        this.currentUserService = currentUserService;
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

    @GetMapping("/analytics/overview")
    public ApiResponse<OperationsAnalyticsOverview> analyticsOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(operationsAnalyticsService.overview(parseRange(startDate, endDate)));
    }

    @GetMapping("/analytics/funnels")
    public ApiResponse<OperationsFunnelSummary> analyticsFunnels(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(operationsAnalyticsService.funnels(parseRange(startDate, endDate)));
    }

    @GetMapping("/analytics/zones")
    public ApiResponse<OperationsZoneSummary> analyticsZones(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(operationsAnalyticsService.zones(parseRange(startDate, endDate)));
    }

    @GetMapping("/analytics/fees")
    public ApiResponse<FeeAnalyticsSummary> analyticsFees(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(operationsAnalyticsService.fees(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/tasks.csv")
    public ResponseEntity<String> exportTasks(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportTasks(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/goods.csv")
    public ResponseEntity<String> exportGoods(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportGoods(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/shop-orders.csv")
    public ResponseEntity<String> exportShopOrders(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportShopOrders(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/project-ads.csv")
    public ResponseEntity<String> exportProjectAds(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportProjectAds(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/governance.csv")
    public ResponseEntity<String> exportGovernance(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportGovernance(parseRange(startDate, endDate)));
    }

    @GetMapping("/exports/fees.csv")
    public ResponseEntity<String> exportFees(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return csv(operationsAnalyticsService.exportFees(parseRange(startDate, endDate)));
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
            @RequestParam(required = false) Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.approve(id, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/project-ads/{id}/reject")
    public ApiResponse<ProjectAdSummary> rejectProjectAd(
            @PathVariable Long id,
            @RequestParam(required = false) Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.reject(id, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/project-ads/{id}/feature")
    public ApiResponse<ProjectAdSummary> featureProjectAd(
            @PathVariable Long id,
            @RequestParam(required = false) Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.feature(id, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/project-ads/{id}/unfeature")
    public ApiResponse<ProjectAdSummary> unfeatureProjectAd(@PathVariable Long id, @RequestParam(required = false) Long adminId) {
        return ApiResponse.ok(projectAdService.unfeature(id, currentUserService.requireAdminId()));
    }

    @PostMapping("/project-ads/{id}/block")
    public ApiResponse<ProjectAdSummary> blockProjectAd(
            @PathVariable Long id,
            @RequestParam(required = false) Long adminId,
            @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.block(id, currentUserService.requireAdminId(), request));
    }

    private AnalyticsDateRange parseRange(String startDate, String endDate) {
        return analyticsDateRangeParser.parse(startDate, endDate);
    }

    private ResponseEntity<String> csv(CsvExport export) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(export.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.body());
    }
}
