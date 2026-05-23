package com.campushub.payment;

import com.campushub.wallet.WalletFlowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "campushub.payment.provider=mock")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceFeeRecordRepository serviceFeeRecordRepository;

    @Autowired
    private WalletFlowRepository walletFlowRepository;

    @Test
    void mockPaymentCreatesLocalPaymentAndMarksPendingServiceFeePaid() throws Exception {
        ServiceFeeRecord fee = serviceFeeRecordRepository.findById(3L).orElseThrow();
        assertThat(fee.getStatus()).isEqualTo("PENDING");

        mockMvc.perform(post("/api/payment/service-fees/3/mock-pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.orderNo").exists())
                .andExpect(jsonPath("$.data.payUrl").exists());

        mockMvc.perform(post("/api/payment/service-fees/3/mock-success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        ServiceFeeRecord paidFee = serviceFeeRecordRepository.findById(3L).orElseThrow();
        assertThat(paidFee.getStatus()).isEqualTo("PAID");
        assertThat(paidFee.getPaidAt()).isNotNull();
        assertThat(walletFlowRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .anyMatch(flow -> flow.getBusinessType().equals("SERVICE_FEE") && flow.getBusinessId().equals(3L));
    }
}
