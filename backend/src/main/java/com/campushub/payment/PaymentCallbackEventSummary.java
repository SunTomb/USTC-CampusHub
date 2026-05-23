package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCallbackEventSummary(
        Long id,
        String eventId,
        String orderNo,
        String providerOrderNo,
        String status,
        BigDecimal amount,
        boolean verified,
        boolean handled,
        String failureReason,
        LocalDateTime createdAt) {

    public static PaymentCallbackEventSummary from(PaymentCallbackEvent event) {
        return new PaymentCallbackEventSummary(
                event.getId(),
                event.getEventId(),
                event.getOrderNo(),
                event.getProviderOrderNo(),
                event.getStatus(),
                event.getAmount(),
                event.isVerified(),
                event.isHandled(),
                event.getFailureReason(),
                event.getCreatedAt());
    }
}
