package com.campushub.payment;

import java.math.BigDecimal;

public record PaymentRequest(
        Long serviceFeeId,
        String tradeNo,
        BigDecimal amount,
        String subject) {
}
