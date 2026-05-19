package com.campushub.payment;

public record MockPayResponse(String provider, String tradeNo, String status, String message) {
}
