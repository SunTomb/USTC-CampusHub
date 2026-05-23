package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceFeeSummary(
        Long id,
        String feeNo,
        Long payerId,
        String payerNickname,
        String targetType,
        Long targetId,
        BigDecimal amount,
        String status,
        String paymentOrderNo,
        String paymentProvider,
        String paymentCenterOrderNo,
        String payUrl,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime failedAt,
        String failureReason) {

    public static ServiceFeeSummary from(ServiceFeeRecord fee) {
        return new ServiceFeeSummary(
                fee.getId(),
                fee.getFeeNo(),
                fee.getPayer().getId(),
                fee.getPayer().getNickname(),
                fee.getTargetType(),
                fee.getTargetId(),
                fee.getAmount(),
                fee.getStatus(),
                fee.getPaymentOrderNo(),
                fee.getPaymentProvider(),
                fee.getPaymentCenterOrderNo(),
                fee.getPayUrl(),
                fee.getCreatedAt(),
                fee.getPaidAt(),
                fee.getExpiresAt(),
                fee.getFailedAt(),
                fee.getFailureReason());
    }
}
