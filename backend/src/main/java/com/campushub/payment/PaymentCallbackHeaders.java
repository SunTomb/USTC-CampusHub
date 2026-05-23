package com.campushub.payment;

public record PaymentCallbackHeaders(String token, String signature, String timestamp) {
}
