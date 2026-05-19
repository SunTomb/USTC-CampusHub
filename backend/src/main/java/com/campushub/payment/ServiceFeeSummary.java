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
        LocalDateTime createdAt,
        LocalDateTime paidAt) {

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
                fee.getCreatedAt(),
                fee.getPaidAt());
    }
}
