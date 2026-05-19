package com.campushub.payment;

public record PaymentStatus(
        String provider,
        String tradeNo,
        String status,
        String message) {
}
