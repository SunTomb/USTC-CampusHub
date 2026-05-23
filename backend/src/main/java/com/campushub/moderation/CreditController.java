package com.campushub.moderation;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit")
public class CreditController {

    private final GovernanceService governanceService;
    private final CurrentUserService currentUserService;

    public CreditController(GovernanceService governanceService, CurrentUserService currentUserService) {
        this.governanceService = governanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<CreditCenterSummary> creditCenter(@PathVariable Long userId) {
        return ApiResponse.ok(governanceService.creditCenter(currentUserService.requireSameUser(userId)));
    }

    @GetMapping("/users/{userId}/reports")
    public ApiResponse<List<ReportRecordSummary>> myReports(@PathVariable Long userId) {
        return ApiResponse.ok(governanceService.reportsByReporter(currentUserService.requireSameUser(userId)));
    }
}
