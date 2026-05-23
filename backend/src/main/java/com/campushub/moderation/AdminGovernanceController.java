package com.campushub.moderation;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/governance")
public class AdminGovernanceController {

    private final GovernanceService governanceService;
    private final CurrentUserService currentUserService;

    public AdminGovernanceController(GovernanceService governanceService, CurrentUserService currentUserService) {
        this.governanceService = governanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<GovernanceDashboardSummary> dashboard() {
        return ApiResponse.ok(governanceService.dashboard());
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportRecordSummary>> reports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType) {
        return ApiResponse.ok(governanceService.listReports(status, targetType));
    }

    @PostMapping("/reports/{reportId}/start-review")
    public ApiResponse<ReportRecordSummary> startReview(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long adminId,
            @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.startReview(reportId, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/reports/{reportId}/reject")
    public ApiResponse<ReportRecordSummary> reject(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long adminId,
            @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.reject(reportId, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ApiResponse<ReportRecordSummary> resolve(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long adminId,
            @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.resolve(reportId, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/reports/{reportId}/escalate")
    public ApiResponse<ReportRecordSummary> escalate(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long adminId,
            @RequestBody GovernanceActionRequest request) {
        return ApiResponse.ok(governanceService.escalate(reportId, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/violations")
    public ApiResponse<ViolationRecordSummary> createViolation(
            @RequestParam(required = false) Long adminId,
            @RequestBody CreateViolationRequest request) {
        return ApiResponse.ok(governanceService.createViolation(currentUserService.requireAdminId(), request));
    }

    @PostMapping("/users/{userId}/credit-adjustments")
    public ApiResponse<CreditAdjustmentSummary> adjustCredit(
            @PathVariable Long userId,
            @RequestParam(required = false) Long adminId,
            @RequestBody CreditAdjustmentRequest request) {
        return ApiResponse.ok(governanceService.adjustCredit(userId, currentUserService.requireAdminId(), request));
    }

    @PostMapping("/users/{userId}/restrictions")
    public ApiResponse<UserRestrictionSummary> restrictUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long adminId,
            @RequestBody UserRestrictionRequest request) {
        return ApiResponse.ok(governanceService.restrictUser(userId, currentUserService.requireAdminId(), request));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AdminActionLogSummary>> auditLogs() {
        return ApiResponse.ok(governanceService.auditLogs());
    }
}
