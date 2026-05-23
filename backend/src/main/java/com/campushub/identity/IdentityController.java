package com.campushub.identity;

import com.campushub.common.ApiResponse;
import com.campushub.payment.PaymentCreation;
import com.campushub.payment.PaymentService;
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
    private final PaymentService paymentService;

    public IdentityController(IdentityService identityService, PaymentService paymentService) {
        this.identityService = identityService;
        this.paymentService = paymentService;
    }

    @PostMapping("/users/{userId}/roles")
    public ApiResponse<RoleApplicationSummary> applyRole(
            @PathVariable Long userId,
            @Valid @RequestBody ApplyRoleRequest request) {
        return ApiResponse.ok(identityService.apply(userId, request));
    }

    @PostMapping("/roles/{applicationId}/deposit-pay")
    public ApiResponse<PaymentCreation> createDepositPayment(@PathVariable Long applicationId) {
        return ApiResponse.ok(paymentService.createRoleDepositPayment(applicationId));
    }
}
