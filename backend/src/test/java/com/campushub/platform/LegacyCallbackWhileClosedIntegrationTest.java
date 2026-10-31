package com.campushub.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.identity.*;
import com.campushub.payment.*;
import com.campushub.user.UserRepository;
import com.campushub.wallet.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "campushub.financial.enabled=false")
@ActiveProfiles("test")
@Transactional
class LegacyCallbackWhileClosedIntegrationTest {
    @Autowired PaymentService payments;
    @Autowired PaymentOrderRepository orders;
    @Autowired WalletRechargeOrderRepository recharges;
    @Autowired WalletAccountRepository wallets;
    @Autowired UserRepository users;
    @Autowired IdentityService identities;
    @Autowired RoleApplicationRepository applications;

    @Test
    void existingRechargeStillSettlesOnceWhileNewPaymentsAreDisabled() {
        String key = UUID.randomUUID().toString();
        var user = users.findById(1L).orElseThrow();
        BigDecimal before = wallets.findByUserId(1L).orElseThrow().getBalance();
        var recharge = recharges.saveAndFlush(new WalletRechargeOrder("R-" + key, user, "ALIPAY", BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.TEN, "PENDING_PAYMENT"));
        var order = orders.saveAndFlush(new PaymentOrder("P-" + key, "WALLET_RECHARGE", recharge.getId(), user,
                BigDecimal.TEN, "PAYMENT_CENTER", LocalDateTime.now().plusMinutes(30)));
        recharge.attachPaymentOrder(order.getOrderNo());
        var request = new PaymentCenterCallbackRequest("event-" + key, order.getOrderNo(), null,
                "WALLET_RECHARGE", recharge.getId(), BigDecimal.TEN, "PAID", LocalDateTime.now(), null);
        payments.handlePaymentCenterCallback(request, new PaymentCallbackHeaders("test-token", null, null, "{}"), "test-token", "");
        payments.handlePaymentCenterCallback(request, new PaymentCallbackHeaders("test-token", null, null, "{}"), "test-token", "");
        assertThat(wallets.findByUserId(1L).orElseThrow().getBalance()).isEqualByComparingTo(before.add(BigDecimal.TEN));
        assertThat(recharge.getStatus()).isEqualTo("PAID");
    }

    @Test
    void lateDepositFailureCannotRevertAnAgreementActivation() {
        String key = UUID.randomUUID().toString();
        var user = users.findById(2L).orElseThrow();
        var legacy = applications.saveAndFlush(new RoleApplication(user, PlatformRoleType.RUNNER, "历史申请"));
        var order = orders.saveAndFlush(new PaymentOrder("P-" + key, "ROLE_DEPOSIT", legacy.getId(), user,
                new BigDecimal("5.00"), "PAYMENT_CENTER", LocalDateTime.now().plusMinutes(30)));
        legacy.attachDepositPaymentOrder(order.getOrderNo());
        var accepted = identities.apply(2L, new ApplyRoleRequest("RUNNER", "", "v1.0", true));
        payments.handlePaymentCenterCallback(new PaymentCenterCallbackRequest("event-" + key, order.getOrderNo(), null,
                "ROLE_DEPOSIT", legacy.getId(), new BigDecimal("5.00"), "FAILED", null, "历史支付失败"),
                new PaymentCallbackHeaders("test-token", null, null, "{}"), "test-token", "");
        var current = applications.findById(legacy.getId()).orElseThrow();
        assertThat(current.getReviewStatus()).isEqualTo("APPROVED");
        assertThat(current.getAgreementStatus()).isEqualTo("CONSENTED");
        assertThat(current.getAgreementConsentedAt()).isEqualTo(accepted.agreementConsentedAt());
        assertThat(current.getDepositPaymentOrderNo()).isEqualTo(order.getOrderNo());
    }
}
