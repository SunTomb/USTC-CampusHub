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
@Transactional
class PaymentOrderServiceIntegrationTest {

    @Autowired ServiceFeeRecordRepository serviceFeeRecordRepository;
    @Autowired UserRepository userRepository;
    @Autowired PaymentOrderRepository paymentOrderRepository;
    @Autowired PaymentCallbackEventRepository paymentCallbackEventRepository;

    @Test
    void serviceFeeRecordStoresPaymentCenterMappingFields() {
        User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
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
        User payer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
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
