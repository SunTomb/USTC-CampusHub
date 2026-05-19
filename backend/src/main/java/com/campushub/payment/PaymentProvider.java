package com.campushub.payment;

import java.util.Map;

public interface PaymentProvider {

    String providerName();

    PaymentCreation createWebPayment(PaymentRequest request);

    boolean verifyCallbackSignature(Map<String, String> callbackParams);

    PaymentStatus queryPaymentStatus(String tradeNo);
}
