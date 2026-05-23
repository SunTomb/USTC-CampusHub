package com.campushub.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PaymentCenterProviderTest {

    @Test
    void paymentCenterProviderFailsClearlyWhenCreateUrlIsMissing() {
        PaymentCenterProperties properties = new PaymentCenterProperties();
        properties.setBaseUrl("https://payment-center.invalid");
        properties.setCallbackUrl("https://campushub.invalid/api/payment/callbacks/payment-center");
        properties.setCallbackToken("internal-token");
        properties.setSigningSecret("internal-secret");
        properties.setExpireMinutes(30);
        PaymentCenterProvider provider = new PaymentCenterProvider(properties, RestClient.builder());

        PaymentRequest request = new PaymentRequest(
                "CHP-PHASE8-003",
                "SERVICE_FEE",
                1L,
                2L,
                "SF-PHASE8-003",
                new BigDecimal("1.00"),
                "CampusHub 服务费 SF-PHASE8-003",
                "https://campushub.invalid/api/payment/callbacks/payment-center",
                30);

        assertThatThrownBy(() -> provider.createWebPayment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付中心创建路径未配置");
    }
}
