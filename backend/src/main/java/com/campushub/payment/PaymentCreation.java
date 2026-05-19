package com.campushub.payment;

public record PaymentCreation(
        String provider,
        String tradeNo,
        String paymentUrl,
        String status,
        String message) {
}
