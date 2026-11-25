package com.campushub.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Transactional
class PaymentOrderServiceIntegrationTest {

    @Autowired ServiceFeeRecordRepository serviceFeeRecordRepository;
    @Autowired UserRepository userRepository;
    @Autowired PaymentOrderRepository paymentOrderRepository;
    @Autowired PaymentCallbackEventRepository paymentCallbackEventRepository;

    @Autowired PaymentService paymentService;

    @Test void independentAlipaySettlesOnlyMatchingOrderOnce() {
        var payer=userRepository.findById(1L).orElseThrow();
        var amount=new BigDecimal("1.23");
        var fee=serviceFeeRecordRepository.saveAndFlush(new ServiceFeeRecord("SF-ALI-TEST",payer,"GOODS_INTENT",1L,amount));
        var order=paymentOrderRepository.saveAndFlush(new PaymentOrder("ALI-TEST-1","SERVICE_FEE",fee.getId(),payer,amount,"ALIPAY_PAGE",java.time.LocalDateTime.now().plusMinutes(15)));
        var fields=new java.util.HashMap<>(java.util.Map.of("trade_no","1231231","out_trade_no",order.getOrderNo(),"trade_status","TRADE_SUCCESS","total_amount","1.23","gmt_payment","2026-11-24 16:00:00"));
        fields.put("total_amount","2.00"); org.assertj.core.api.Assertions.assertThatThrownBy(()->paymentService.settleAlipay(fields)).hasMessageContaining("不匹配");
        fields.put("total_amount","1.23");paymentService.settleAlipay(fields);paymentService.settleAlipay(fields);
        assertThat(order.getStatus()).isEqualTo("PAID");assertThat(fee.getStatus()).isEqualTo("PAID");assertThat(paymentCallbackEventRepository.findByEventId("ali:1231231")).isPresent();
    }

    @Test
    void serviceFeeRecordStoresPaymentCenterMappingFields() {
        User payer = userRepository.findById(1L).orElseThrow();
        ServiceFeeRecord fee = new ServiceFeeRecord("SF-PHASE8-001", payer, "GOODS_INTENT", 1L, new BigDecimal("1.00"));

        fee.attachPaymentOrder("CHP-PHASE8-001", "PAYMENT_CENTER", "ATS-PHASE8-001", "https://pay.example/phase8", java.time.LocalDateTime.now().plusMinutes(30));
        ServiceFeeRecord saved = serviceFeeRecordRepository.saveAndFlush(fee);

        assertThat(saved.getPaymentOrderNo()).isEqualTo("CHP-PHASE8-001");
        assertThat(saved.getPaymentProvider()).isEqualTo("PAYMENT_CENTER");
        assertThat(saved.getPaymentCenterOrderNo()).isEqualTo("ATS-PHASE8-001");
        assertThat(saved.getPayUrl()).isEqualTo("https://pay.example/phase8");
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    void paymentOrderAndCallbackEventPersistForIdempotency() {
        User payer = userRepository.findById(1L).orElseThrow();
        PaymentOrder order = new PaymentOrder(
                "CHP-PHASE8-002",
                "SERVICE_FEE",
                1L,
                payer,
                new BigDecimal("1.00"),
                "MOCK",
                java.time.LocalDateTime.now().plusMinutes(30));
        order.attachProviderOrder("MOCK-PHASE8-002", "mock://pay/CHP-PHASE8-002");
        paymentOrderRepository.saveAndFlush(order);

        PaymentCallbackEvent event = new PaymentCallbackEvent(
                "evt-phase8-002",
                "CHP-PHASE8-002",
                "MOCK-PHASE8-002",
                "PAID",
                new BigDecimal("1.00"),
                true,
                true,
                null);
        paymentCallbackEventRepository.saveAndFlush(event);

        assertThat(paymentOrderRepository.findByOrderNo("CHP-PHASE8-002")).isPresent();
        assertThat(paymentCallbackEventRepository.findByEventId("evt-phase8-002")).isPresent();
    }
}
