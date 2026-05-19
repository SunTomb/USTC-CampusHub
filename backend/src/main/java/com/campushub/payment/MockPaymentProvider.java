package com.campushub.payment;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "LOCAL_MOCK_ALIPAY_READY";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        return new PaymentCreation(
                providerName(),
                request.tradeNo(),
                "/api/payment/service-fees/" + request.serviceFeeId() + "/mock-success",
                "CREATED",
                "本地模拟支付已创建，可调用 mock-success 完成支付状态流转。");
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, String> callbackParams) {
        return true;
    }

    @Override
    public PaymentStatus queryPaymentStatus(String tradeNo) {
        return new PaymentStatus(providerName(), tradeNo, "SUCCESS", "本地模拟支付默认返回成功。 ");
    }
}
