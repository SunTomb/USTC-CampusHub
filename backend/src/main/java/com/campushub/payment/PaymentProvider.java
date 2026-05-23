package com.campushub.payment;

public interface PaymentProvider {

    String providerName();

    PaymentCreation createWebPayment(PaymentRequest request);

    PaymentStatus queryPaymentStatus(String orderNo);
}
