package com.campushub.payment;

import com.campushub.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "direct-alipay-disabled")
public class AlipayPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "DIRECT_ALIPAY_DISABLED";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        throw new BusinessException("CampusHub 不直接接入支付宝支付，请使用 API-Transfer-Station 支付中心。 ");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String orderNo) {
        throw new BusinessException("CampusHub 不直接查询支付宝支付状态，请使用 API-Transfer-Station 支付中心。 ");
    }
}
