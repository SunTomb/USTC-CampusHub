package com.campushub.platform;

import com.campushub.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformFeatureController {

    private final FinancialWriteGuard financialWriteGuard;

    public PlatformFeatureController(FinancialWriteGuard financialWriteGuard) {
        this.financialWriteGuard = financialWriteGuard;
    }

    @GetMapping("/features")
    public ApiResponse<FeatureSummary> features() {
        return ApiResponse.ok(new FeatureSummary(financialWriteGuard.isEnabled()));
    }

    public record FeatureSummary(boolean financialEnabled) {
    }
}
