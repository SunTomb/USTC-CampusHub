package com.campushub.identity;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/master")
public class MasterAdminController {

    private final IdentityService identityService;
    private final CurrentUserService currentUserService;

    public MasterAdminController(IdentityService identityService, CurrentUserService currentUserService) {
        this.identityService = identityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin-applications")
    public ApiResponse<List<RoleApplicationSummary>> listPendingAdminApplications() {
        currentUserService.requireMasterAdminId();
        return ApiResponse.ok(identityService.listPendingAdminApplications());
    }

    @PostMapping("/admin-applications/{applicationId}/approve")
    public ApiResponse<RoleApplicationSummary> approveAdminApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.approve(applicationId, currentUserService.requireMasterAdminId()));
    }

    @PostMapping("/admin-applications/{applicationId}/reject")
    public ApiResponse<RoleApplicationSummary> rejectAdminApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.reject(applicationId, currentUserService.requireMasterAdminId()));
    }
}
