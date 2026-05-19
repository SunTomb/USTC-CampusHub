package com.campushub.payment;

import com.campushub.common.BusinessException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "alipay")
public class AlipayPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "ALIPAY_WEB_PAY";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        throw new BusinessException("生产支付宝网页支付尚未启用：请先配置应用、公私钥、HTTPS 回调与签名验签。 ");
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, String> callbackParams) {
        throw new BusinessException("生产支付宝回调验签尚未启用。 ");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String tradeNo) {
        throw new BusinessException("生产支付宝查询尚未启用。 ");
    }
}
