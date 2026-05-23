package com.campushub.payment;

import java.time.LocalDateTime;

public record PaymentCreation(
        String provider,
        String orderNo,
        String providerOrderNo,
        String payUrl,
        String status,
        LocalDateTime expiresAt,
        String message) {
}
