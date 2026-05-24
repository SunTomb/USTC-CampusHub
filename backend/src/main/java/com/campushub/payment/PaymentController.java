package com.campushub.payment;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final PaymentService paymentService;
    private final PaymentCenterProperties paymentCenterProperties;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public PaymentController(
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            PaymentService paymentService,
            PaymentCenterProperties paymentCenterProperties,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper) {
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.paymentService = paymentService;
        this.paymentCenterProperties = paymentCenterProperties;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/service-fees")
    public ApiResponse<List<ServiceFeeSummary>> listServiceFees() {
        currentUserService.requireAdminId();
        List<ServiceFeeSummary> fees = serviceFeeRecordRepository.findAll().stream()
                .map(ServiceFeeSummary::from)
                .toList();
        return ApiResponse.ok(fees);
    }

    @GetMapping("/users/{userId}/service-fees")
    public ApiResponse<List<ServiceFeeSummary>> listUserServiceFees(@PathVariable Long userId) {
        Long effectiveUserId = currentUserService.requireSameUser(userId);
        List<ServiceFeeSummary> fees = serviceFeeRecordRepository.findByPayerIdOrderByCreatedAtDesc(effectiveUserId).stream()
                .map(ServiceFeeSummary::from)
                .toList();
        return ApiResponse.ok(fees);
    }

    @PostMapping("/service-fees/{feeId}/pay")
    public ApiResponse<PaymentCreation> createServiceFeePayment(@PathVariable Long feeId) {
        ServiceFeeRecord fee = serviceFeeRecordRepository.findById(feeId)
                .orElseThrow(() -> new BusinessException("服务费记录不存在"));
        currentUserService.requireSameUser(fee.getPayer().getId());
        return ApiResponse.ok(paymentService.createServiceFeePayment(feeId));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrderSummary> getOrder(@PathVariable String orderNo) {
        PaymentOrderSummary order = paymentService.getOrder(orderNo);
        currentUserService.requireSameUser(order.payerId());
        return ApiResponse.ok(order);
    }

    @PostMapping("/callbacks/payment-center")
    public ApiResponse<PaymentStatus> handlePaymentCenterCallback(
            @RequestHeader(name = "X-CampusHub-Payment-Token", required = false) String token,
            @RequestHeader(name = "X-CampusHub-Payment-Signature", required = false) String signature,
            @RequestHeader(name = "X-CampusHub-Payment-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {
        PaymentCenterCallbackRequest request;
        try {
            request = objectMapper.readValue(rawBody, PaymentCenterCallbackRequest.class);
        } catch (Exception e) {
            throw new BusinessException("支付回调内容无效");
        }
        return ApiResponse.ok(paymentService.handlePaymentCenterCallback(
                request,
                new PaymentCallbackHeaders(token, signature, timestamp, rawBody),
                paymentCenterProperties.getCallbackToken(),
                paymentCenterProperties.getSigningSecret()));
    }

    @PostMapping("/service-fees/{feeId}/mock-pay")
    public ApiResponse<PaymentCreation> createMockServiceFeePayment(@PathVariable Long feeId) {
        return ApiResponse.ok(paymentService.createServiceFeePayment(feeId));
    }

    @PostMapping("/service-fees/{feeId}/mock-success")
    public ApiResponse<PaymentStatus> markMockServiceFeeSuccess(@PathVariable Long feeId) {
        return ApiResponse.ok(paymentService.markMockServiceFeeSuccess(feeId));
    }
}
