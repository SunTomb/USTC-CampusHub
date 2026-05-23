package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentOrderSummary(
        Long id,
        String orderNo,
        String businessType,
        Long businessId,
        Long payerId,
        String payerNickname,
        BigDecimal amount,
        String provider,
        String providerOrderNo,
        String payUrl,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime failedAt,
        String failureReason,
        LocalDateTime createdAt) {

    public static PaymentOrderSummary from(PaymentOrder order) {
        return new PaymentOrderSummary(
                order.getId(),
                order.getOrderNo(),
                order.getBusinessType(),
                order.getBusinessId(),
                order.getPayer().getId(),
                order.getPayer().getNickname(),
                order.getAmount(),
                order.getProvider(),
                order.getProviderOrderNo(),
                order.getPayUrl(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getFailedAt(),
                order.getFailureReason(),
                order.getCreatedAt());
    }

    public static PaymentCreation toCreation(PaymentOrder order) {
        return new PaymentCreation(
                order.getProvider(),
                order.getOrderNo(),
                order.getProviderOrderNo(),
                order.getPayUrl(),
                order.getStatus(),
                order.getExpiresAt(),
                "复用待支付订单");
    }
}
