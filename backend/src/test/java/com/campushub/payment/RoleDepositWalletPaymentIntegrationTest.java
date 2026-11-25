package com.campushub.payment;

import com.campushub.auth.UserRoleService;
import com.campushub.identity.PlatformRoleType;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccountRepository;
import com.campushub.wallet.WalletFlowRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"campushub.payment.provider=mock", "campushub.financial.enabled=true"})
@org.springframework.transaction.annotation.Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoleDepositWalletPaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoleApplicationRepository roleApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private WalletFlowRepository walletFlowRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Test
    void runnerDepositWalletPaymentDebitsBalanceAndAssignsRunnerRole() {
        var application = roleApplicationRepository.saveAndFlush(new RoleApplication(
                userRepository.findById(1L).orElseThrow(), PlatformRoleType.RUNNER, "历史保证金申请"));
        var beforeBalance = walletAccountRepository.findByUserId(1L).orElseThrow().getBalance();

        PaymentCreation payment = paymentService.payRoleDepositWithWallet(application.getId(), 1L);

        assertThat(payment.provider()).isEqualTo("WALLET");
        assertThat(payment.orderNo()).isEqualTo("WALLET-RD-" + application.getId());
        assertThat(payment.providerOrderNo()).isNull();
        assertThat(payment.payUrl()).isEqualTo("wallet://role-deposit/" + application.getId());
        assertThat(payment.status()).isEqualTo("PAID");
        assertThat(payment.expiresAt()).isNull();
        assertThat(payment.message()).isEqualTo("余额支付成功");

        var paidApplication = roleApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(paidApplication.getDepositStatus()).isEqualTo("PAID");
        assertThat(paidApplication.getReviewStatus()).isEqualTo("APPROVED");

        var afterWallet = walletAccountRepository.findByUserId(1L).orElseThrow();
        assertThat(afterWallet.getBalance()).isEqualByComparingTo(beforeBalance.subtract(new BigDecimal("5.00")));
        assertThat(walletFlowRepository.findByBusinessTypeAndBusinessIdOrderByCreatedAtDesc("ROLE_DEPOSIT", application.getId()))
                .anySatisfy(flow -> {
                    assertThat(flow.getFlowType()).isEqualTo("ROLE_DEPOSIT");
                    assertThat(flow.getIdempotencyKey()).isEqualTo("role-deposit-wallet:" + application.getId());
                    assertThat(flow.getAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
                    assertThat(flow.getRemark()).isEqualTo("身份保证金余额支付");
                });
        assertThat(userRoleService.findRoleCodes(1L)).contains("ROLE_RUNNER");
    }
}
