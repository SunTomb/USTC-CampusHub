package com.campushub.payment;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payment")
public class AdminPaymentController {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentCallbackEventRepository callbackEventRepository;

    public AdminPaymentController(PaymentOrderRepository paymentOrderRepository, PaymentCallbackEventRepository callbackEventRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.callbackEventRepository = callbackEventRepository;
    }

    @GetMapping("/orders")
    public ApiResponse<List<PaymentOrderSummary>> listOrders(@RequestParam(required = false) String status) {
        List<PaymentOrder> orders = status == null || status.isBlank()
                ? paymentOrderRepository.findTop200ByOrderByCreatedAtDesc()
                : paymentOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        return ApiResponse.ok(orders.stream().map(PaymentOrderSummary::from).toList());
    }

    @GetMapping("/callback-events")
    public ApiResponse<List<PaymentCallbackEventSummary>> listCallbackEvents() {
        return ApiResponse.ok(callbackEventRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .map(PaymentCallbackEventSummary::from)
                .toList());
    }
}
