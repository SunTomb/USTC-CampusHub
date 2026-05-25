package com.campushub.payment;

import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.identity.RoleApplicationSummary;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletFlowRepository;
import java.util.UUID;
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

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RoleApplicationRepository roleApplicationRepository;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    void roleDepositFailedCallbackKeepsApplicationRecoverableForRetry() {
        Long userId = createPaymentRetryUser("failed");
        RoleApplicationSummary application = identityService.apply(userId, new ApplyRoleRequest("RUNNER", "申请跑腿保证金失败重试"));
        PaymentCreation firstPayment = paymentService.createRoleDepositPayment(application.id());

        PaymentStatus failed = paymentService.handlePaymentCenterCallback(
                new PaymentCenterCallbackRequest(
                        "evt-role-failed-1",
                        firstPayment.orderNo(),
                        firstPayment.providerOrderNo(),
                        "ROLE_DEPOSIT",
                        application.id(),
                        application.depositAmount(),
                        "FAILED",
                        null,
                        "payment center failed"),
                new PaymentCallbackHeaders("test-token", null, null, "{}"),
                "test-token",
                "");

        assertThat(failed.status()).isEqualTo("FAILED");
        var failedApplication = roleApplicationRepository.findById(application.id()).orElseThrow();
        assertThat(failedApplication.getDepositStatus()).isEqualTo("FAILED");
        assertThat(failedApplication.getReviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(failedApplication.isRecoverableUnpaid()).isTrue();

        RoleApplicationSummary retryApplication = identityService.apply(3L, new ApplyRoleRequest("RUNNER", "重新支付保证金"));
        PaymentCreation retryPayment = paymentService.createRoleDepositPayment(retryApplication.id());

        assertThat(retryApplication.id()).isEqualTo(application.id());
        assertThat(retryPayment.orderNo()).isNotEqualTo(firstPayment.orderNo());
        assertThat(roleApplicationRepository.findById(application.id()).orElseThrow().getDepositPaymentOrderNo())
                .isEqualTo(retryPayment.orderNo());
    }

    @Test
    void staleRoleDepositCallbackDoesNotMutateNewerRetryApplication() {
        Long userId = createPaymentRetryUser("stale");
        RoleApplicationSummary application = identityService.apply(userId, new ApplyRoleRequest("GOODS_PUBLISHER", "申请二手发布保证金重试"));
        PaymentCreation firstPayment = paymentService.createRoleDepositPayment(application.id());
        var roleApplication = roleApplicationRepository.findById(application.id()).orElseThrow();
        roleApplication.attachDepositPaymentOrder("CHP-RD-newer-retry");
        roleApplicationRepository.saveAndFlush(roleApplication);

        paymentService.handlePaymentCenterCallback(
                new PaymentCenterCallbackRequest(
                        "evt-role-stale-failed-1",
                        firstPayment.orderNo(),
                        firstPayment.providerOrderNo(),
                        "ROLE_DEPOSIT",
                        application.id(),
                        application.depositAmount(),
                        "FAILED",
                        null,
                        "stale failure"),
                new PaymentCallbackHeaders("test-token", null, null, "{}"),
                "test-token",
                "");

        var unchangedApplication = roleApplicationRepository.findById(application.id()).orElseThrow();
        assertThat(unchangedApplication.getDepositStatus()).isEqualTo("PENDING");
        assertThat(unchangedApplication.getReviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(unchangedApplication.getDepositPaymentOrderNo()).isEqualTo("CHP-RD-newer-retry");
    }

    private Long createPaymentRetryUser(String suffix) {
        String unique = suffix + "-" + UUID.randomUUID();
        String digits = String.format("%08d", Math.floorMod(unique.hashCode(), 100_000_000));
        User user = new User(
                "PB-RD-" + unique,
                "rd_" + unique.replace('-', '_'),
                "hash",
                "保证金测试用户",
                "保证金测试用户",
                "139" + digits,
                "rd-" + unique + "@mail.ustc.edu.cn",
                "ACTIVE");
        return userRepository.saveAndFlush(user).getId();
    }
}
