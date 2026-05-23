package com.campushub.payment;

import java.math.BigDecimal;

public record PaymentRequest(
        String orderNo,
        String businessType,
        Long businessId,
        Long payerId,
        String businessNo,
        BigDecimal amount,
        String subject,
        String callbackUrl,
        int expireMinutes) {
}
