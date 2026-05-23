package com.campushub.payment;

import com.campushub.common.BusinessException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "campushub.payment.provider", havingValue = "payment-center")
public class PaymentCenterProvider implements PaymentProvider {

    private final PaymentCenterProperties properties;
    private final RestClient restClient;

    public PaymentCenterProvider(PaymentCenterProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public String providerName() {
        return "PAYMENT_CENTER";
    }

    @Override
    public PaymentCreation createWebPayment(PaymentRequest request) {
        validateCreateConfig();
        PaymentCenterCreateResponse response = restClient.post()
                .uri(properties.getBaseUrl() + properties.getCreatePath())
                .header("X-CampusHub-Payment-Token", properties.getCallbackToken())
                .body(Map.of(
                        "app", "campushub",
                        "orderNo", request.orderNo(),
                        "businessType", request.businessType(),
                        "businessId", request.businessId(),
                        "payerId", request.payerId(),
                        "amount", request.amount().toPlainString(),
                        "subject", request.subject(),
                        "callbackUrl", request.callbackUrl(),
                        "expireMinutes", request.expireMinutes()))
                .retrieve()
                .body(PaymentCenterCreateResponse.class);
        if (response == null || !StringUtils.hasText(response.paymentCenterOrderNo())) {
            throw new BusinessException("支付中心未返回有效支付单");
        }
        return new PaymentCreation(providerName(), request.orderNo(), response.paymentCenterOrderNo(), response.payUrl(), response.status(), response.expiresAt(), "支付中心收款单已创建");
    }

    @Override
    public PaymentStatus queryPaymentStatus(String orderNo) {
        throw new BusinessException("支付中心主动查询暂未启用，请以内部回调为准");
    }

    private void validateCreateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException("支付中心地址未配置");
        }
        if (!StringUtils.hasText(properties.getCreatePath())) {
            throw new BusinessException("支付中心创建路径未配置");
        }
        if (!StringUtils.hasText(properties.getCallbackUrl())) {
            throw new BusinessException("支付中心回调地址未配置");
        }
        if (!StringUtils.hasText(properties.getCallbackToken())) {
            throw new BusinessException("支付中心内部 token 未配置");
        }
    }

    private record PaymentCenterCreateResponse(String paymentCenterOrderNo, String payUrl, String status, LocalDateTime expiresAt) {
    }
}
