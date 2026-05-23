package com.campushub.moderation;

import com.campushub.auth.CurrentUserService;
import com.campushub.audit.SafetyLogRepository;
import com.campushub.audit.SafetyLogSummary;
import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReportRecordRepository reportRecordRepository;
    private final ViolationRecordRepository violationRecordRepository;
    private final SafetyLogRepository safetyLogRepository;
    private final ModerationService moderationService;
    private final CurrentUserService currentUserService;

    public ModerationController(
            ReviewRecordRepository reviewRecordRepository,
            ReportRecordRepository reportRecordRepository,
            ViolationRecordRepository violationRecordRepository,
            SafetyLogRepository safetyLogRepository,
            ModerationService moderationService,
            CurrentUserService currentUserService) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.violationRecordRepository = violationRecordRepository;
        this.safetyLogRepository = safetyLogRepository;
        this.moderationService = moderationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ReviewRecordSummary>> listReviews() {
        List<ReviewRecordSummary> reviews = reviewRecordRepository.findAll().stream()
                .map(ReviewRecordSummary::from)
                .toList();
        return ApiResponse.ok(reviews);
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportRecordSummary>> listReports() {
        List<ReportRecordSummary> reports = reportRecordRepository.findAll().stream()
                .map(ReportRecordSummary::from)
                .toList();
        return ApiResponse.ok(reports);
    }

    @PostMapping("/reports")
    public ApiResponse<ReportRecordSummary> report(@RequestParam(required = false) Long reporterId, @Valid @RequestBody ReportRequest request) {
        Long effectiveReporterId = reporterId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(reporterId);
        return ApiResponse.ok(moderationService.report(effectiveReporterId, request));
    }

    @GetMapping("/violations")
    public ApiResponse<List<ViolationRecordSummary>> listViolations() {
        List<ViolationRecordSummary> violations = violationRecordRepository.findAll().stream()
                .map(ViolationRecordSummary::from)
                .toList();
        return ApiResponse.ok(violations);
    }

    @GetMapping("/safety-logs")
    public ApiResponse<List<SafetyLogSummary>> listSafetyLogs() {
        List<SafetyLogSummary> logs = safetyLogRepository.findAll().stream()
                .map(SafetyLogSummary::from)
                .toList();
        return ApiResponse.ok(logs);
    }
}
