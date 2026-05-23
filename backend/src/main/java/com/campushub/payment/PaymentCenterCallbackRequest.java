package com.campushub.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCenterCallbackRequest(
        String eventId,
        String orderNo,
        String paymentCenterOrderNo,
        String businessType,
        Long businessId,
        BigDecimal amount,
        String status,
        LocalDateTime paidAt,
        String failureReason) {
}
