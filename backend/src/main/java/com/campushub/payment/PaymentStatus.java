package com.campushub.payment;

import java.time.LocalDateTime;

public record PaymentStatus(
        String provider,
        String orderNo,
        String providerOrderNo,
        String status,
        LocalDateTime paidAt,
        String failureReason,
        String message) {
}
