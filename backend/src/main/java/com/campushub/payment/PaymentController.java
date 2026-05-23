package com.campushub.payment;

import com.campushub.common.ApiResponse;
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

    public PaymentController(ServiceFeeRecordRepository serviceFeeRecordRepository, PaymentService paymentService, PaymentCenterProperties paymentCenterProperties) {
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.paymentService = paymentService;
        this.paymentCenterProperties = paymentCenterProperties;
    }

    @GetMapping("/service-fees")
    public ApiResponse<List<ServiceFeeSummary>> listServiceFees() {
        List<ServiceFeeSummary> fees = serviceFeeRecordRepository.findAll().stream()
                .map(ServiceFeeSummary::from)
                .toList();
        return ApiResponse.ok(fees);
    }

    @GetMapping("/users/{userId}/service-fees")
    public ApiResponse<List<ServiceFeeSummary>> listUserServiceFees(@PathVariable Long userId) {
        List<ServiceFeeSummary> fees = serviceFeeRecordRepository.findByPayerIdOrderByCreatedAtDesc(userId).stream()
                .map(ServiceFeeSummary::from)
                .toList();
        return ApiResponse.ok(fees);
    }

    @PostMapping("/service-fees/{feeId}/pay")
    public ApiResponse<PaymentCreation> createServiceFeePayment(@PathVariable Long feeId) {
        return ApiResponse.ok(paymentService.createServiceFeePayment(feeId));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<PaymentOrderSummary> getOrder(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.getOrder(orderNo));
    }

    @PostMapping("/callbacks/payment-center")
    public ApiResponse<PaymentStatus> handlePaymentCenterCallback(
            @RequestHeader(name = "X-CampusHub-Payment-Token", required = false) String token,
            @RequestHeader(name = "X-CampusHub-Payment-Signature", required = false) String signature,
            @RequestHeader(name = "X-CampusHub-Payment-Timestamp", required = false) String timestamp,
            @RequestBody PaymentCenterCallbackRequest request) {
        return ApiResponse.ok(paymentService.handlePaymentCenterCallback(
                request,
                new PaymentCallbackHeaders(token, signature, timestamp),
                paymentCenterProperties.getCallbackToken()));
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
