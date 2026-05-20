package com.campushub.identity;

import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/users/{userId}/roles")
    public ApiResponse<RoleApplicationSummary> applyRole(
            @PathVariable Long userId,
            @Valid @RequestBody ApplyRoleRequest request) {
        return ApiResponse.ok(identityService.apply(userId, request));
    }
}
