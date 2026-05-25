package com.campushub.payment;

import com.campushub.auth.UserRoleService;
import com.campushub.identity.ApplyRoleRequest;
import com.campushub.identity.IdentityService;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.wallet.WalletAccountRepository;
import com.campushub.wallet.WalletFlowRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "campushub.payment.provider=mock")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoleDepositWalletPaymentIntegrationTest {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoleApplicationRepository roleApplicationRepository;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private WalletFlowRepository walletFlowRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Test
    void runnerDepositWalletPaymentDebitsBalanceAndAssignsRunnerRole() {
        var application = identityService.apply(1L, new ApplyRoleRequest("RUNNER", "想接校园跑腿任务"));
        var beforeWallet = walletAccountRepository.findByUserId(1L).orElseThrow();

        PaymentCreation payment = paymentService.payRoleDepositWithWallet(application.id(), 1L);

        assertThat(payment.provider()).isEqualTo("WALLET");
        assertThat(payment.orderNo()).isEqualTo("WALLET-RD-" + application.id());
        assertThat(payment.providerOrderNo()).isNull();
        assertThat(payment.payUrl()).isEqualTo("wallet://role-deposit/" + application.id());
        assertThat(payment.status()).isEqualTo("PAID");
        assertThat(payment.expiresAt()).isNull();
        assertThat(payment.message()).isEqualTo("余额支付成功");

        var paidApplication = roleApplicationRepository.findById(application.id()).orElseThrow();
        assertThat(paidApplication.getDepositStatus()).isEqualTo("PAID");
        assertThat(paidApplication.getReviewStatus()).isEqualTo("APPROVED");

        var afterWallet = walletAccountRepository.findByUserId(1L).orElseThrow();
        assertThat(afterWallet.getBalance()).isEqualByComparingTo(beforeWallet.getBalance().subtract(new BigDecimal("5.00")));
        assertThat(walletFlowRepository.findByBusinessTypeAndBusinessIdOrderByCreatedAtDesc("ROLE_DEPOSIT", application.id()))
                .anySatisfy(flow -> {
                    assertThat(flow.getFlowType()).isEqualTo("ROLE_DEPOSIT");
                    assertThat(flow.getIdempotencyKey()).isEqualTo("role-deposit-wallet:" + application.id());
                    assertThat(flow.getAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
                    assertThat(flow.getRemark()).isEqualTo("身份保证金余额支付");
                });
        assertThat(userRoleService.findRoleCodes(1L)).contains("ROLE_RUNNER");
    }
}
