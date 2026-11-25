package com.campushub.platform;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/protocols")
public class PlatformProtocolController {

    private final PlatformProtocolService protocolService;
    private final CurrentUserService currentUserService;

    public PlatformProtocolController(PlatformProtocolService protocolService, CurrentUserService currentUserService) {
        this.protocolService = protocolService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{roleType}")
    public ApiResponse<PlatformProtocolSummary> current(@PathVariable String roleType) {
        return ApiResponse.ok(protocolService.current(roleType, currentUserService.optionalUserId().orElse(null)));
    }
}
