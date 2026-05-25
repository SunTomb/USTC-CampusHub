package com.campushub.identity;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.payment.PaymentCreation;
import com.campushub.payment.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final CurrentUserService currentUserService;
    private final RoleApplicationRepository roleApplicationRepository;

    public IdentityController(
            IdentityService identityService,
            PaymentService paymentService,
            CurrentUserService currentUserService,
            RoleApplicationRepository roleApplicationRepository) {
        this.identityService = identityService;
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
        this.roleApplicationRepository = roleApplicationRepository;
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<RoleApplicationSummary>> listUserApplications(@PathVariable Long userId) {
        return ApiResponse.ok(identityService.listUserApplications(currentUserService.requireSameUser(userId)));
    }

    @PostMapping("/users/{userId}/roles")
    public ApiResponse<RoleApplicationSummary> applyRole(
            @PathVariable Long userId,
            @Valid @RequestBody ApplyRoleRequest request) {
        return ApiResponse.ok(identityService.apply(currentUserService.requireSameUser(userId), request));
    }

    @PostMapping("/roles/{applicationId}/deposit-pay")
    public ApiResponse<PaymentCreation> createDepositPayment(@PathVariable Long applicationId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        currentUserService.requireSameUser(application.getUser().getId());
        return ApiResponse.ok(paymentService.createRoleDepositPayment(applicationId));
    }
}
