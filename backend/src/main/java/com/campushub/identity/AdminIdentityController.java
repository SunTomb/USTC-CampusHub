package com.campushub.identity;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/identity")
public class AdminIdentityController {

    private final IdentityService identityService;
    private final CurrentUserService currentUserService;

    public AdminIdentityController(IdentityService identityService, CurrentUserService currentUserService) {
        this.identityService = identityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/shop-merchant-applications/pending")
    public ApiResponse<List<RoleApplicationSummary>> listPendingShopMerchantApplications() {
        return ApiResponse.ok(identityService.listPendingShopMerchantApplications());
    }

    @PostMapping("/role-applications/{applicationId}/approve")
    public ApiResponse<RoleApplicationSummary> approveRoleApplication(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long reviewerId) {
        return ApiResponse.ok(identityService.approve(applicationId, currentUserService.requireAdminId()));
    }

    @PostMapping("/role-applications/{applicationId}/reject")
    public ApiResponse<RoleApplicationSummary> rejectRoleApplication(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Long reviewerId) {
        return ApiResponse.ok(identityService.reject(applicationId, currentUserService.requireAdminId()));
    }
}
