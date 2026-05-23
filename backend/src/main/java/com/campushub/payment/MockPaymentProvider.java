package com.campushub.payment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    private final Map<String, String> mockStatuses = new ConcurrentHashMap<>();

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        String providerOrderNo = "MOCK-" + request.orderNo();
        mockStatuses.putIfAbsent(request.orderNo(), "PENDING");
        return new PaymentCreation(
                providerName(),
                request.orderNo(),
                providerOrderNo,
                "mock://pay/" + request.orderNo(),
                "PENDING",
                LocalDateTime.now().plusMinutes(request.expireMinutes()),
                "本地模拟支付单已创建");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String orderNo) {
        String status = mockStatuses.getOrDefault(orderNo, "PENDING");
        return new PaymentStatus(
                providerName(),
                orderNo,
                "MOCK-" + orderNo,
                status,
                "SUCCESS".equals(status) ? LocalDateTime.now() : null,
                null,
                "本地模拟支付状态");
    }

    public void markSuccess(String orderNo) {
        mockStatuses.put(orderNo, "SUCCESS");
    }
}
