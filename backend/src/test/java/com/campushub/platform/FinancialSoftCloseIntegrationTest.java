package com.campushub.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import com.campushub.goods.GoodsService;
import com.campushub.payment.PaymentService;
import com.campushub.payment.PaymentOrderRepository;
import com.campushub.wallet.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "campushub.financial.enabled=false")
@ActiveProfiles("test")
@Transactional
class FinancialSoftCloseIntegrationTest {
    @Autowired FinancialWriteGuard guard;
    @Autowired PaymentService payments;
    @Autowired WalletOperationService wallets;
    @Autowired GoodsService goods;
    @Autowired PaymentOrderRepository paymentOrders;
    @Autowired WalletRechargeOrderRepository recharges;
    @Autowired WalletWithdrawalRequestRepository withdrawals;

    @Test
    void closedMoneyPathsRejectBeforeCreatingRecords() {
        assertThat(guard.isEnabled()).isFalse();
        long orderCount = paymentOrders.count();
        long rechargeCount = recharges.count();
        long withdrawalCount = withdrawals.count();
        assertClosed(() -> payments.createServiceFeePayment(3L));
        assertClosed(() -> payments.createRoleDepositPayment(1L));
        assertClosed(() -> payments.payRoleDepositWithWallet(1L, 1L));
        assertClosed(() -> payments.createMockServiceFeePayment(3L));
        assertClosed(() -> payments.markMockServiceFeeSuccess(3L));
        assertClosed(() -> wallets.createRecharge(1L, new WalletRechargeRequest("ALIPAY", BigDecimal.TEN, "")));
        assertClosed(() -> wallets.createRecharge(1L, new WalletRechargeRequest("WECHAT", BigDecimal.TEN, "")));
        assertClosed(() -> wallets.createWithdrawal(1L, new CreateWithdrawalRequest(BigDecimal.TEN, "WECHAT", "")));
        assertClosed(() -> goods.createOnlineEscrowOrder(1L, 2L));
        assertClosed(() -> goods.freezeGoodsEscrow(1L, 2L));
        assertClosed(() -> goods.confirmGoodsEscrow(1L, 2L));
        assertClosed(() -> goods.cancelGoodsEscrow(1L, 2L, ""));
        assertClosed(() -> goods.disputeGoodsEscrow(1L, 2L, ""));
        assertThat(paymentOrders.count()).isEqualTo(orderCount);
        assertThat(recharges.count()).isEqualTo(rechargeCount);
        assertThat(withdrawals.count()).isEqualTo(withdrawalCount);
    }

    @Test
    void administrativeFinancialActionsAreAlsoReadOnly() {
        assertClosed(() -> wallets.approveWechatRecharge(1L, 4L, ""));
        assertClosed(() -> wallets.rejectWechatRecharge(1L, 4L, ""));
        assertClosed(() -> wallets.approveWithdrawal(1L, 4L, ""));
        assertClosed(() -> wallets.rejectWithdrawal(1L, 4L, ""));
        assertClosed(() -> wallets.completeWithdrawal(1L, 4L, ""));
        assertThat(wallets.listAdminRecharges(null)).isNotNull();
        assertThat(wallets.listAdminWithdrawals(null)).isNotNull();
    }

    private void assertClosed(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(BusinessException.class).hasMessageContaining("暂未开放");
    }
}
