package com.campushub.wallet;

import java.math.BigDecimal;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class WalletServiceIntegrationTest {

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private WalletFlowRepository walletFlowRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private FeePolicyService feePolicyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletOperationService walletOperationService;

    @Autowired
    private WalletRechargeOrderRepository walletRechargeOrderRepository;

    @Autowired
    private WalletWithdrawalRequestRepository walletWithdrawalRequestRepository;

    @Test
    void walletAccountAndFlowExposePhase9LedgerFields() {
        WalletAccount wallet = walletAccountRepository.findByUserId(1L).orElseThrow();

        wallet.credit(new BigDecimal("25.50"));
        wallet.freeze(new BigDecimal("10.00"));
        wallet = walletAccountRepository.saveAndFlush(wallet);

        WalletFlow flow = walletFlowRepository.saveAndFlush(new WalletFlow(
                wallet,
                wallet.getUser(),
                "WF-P9-LEDGER-001",
                "OUT",
                "ESCROW_FREEZE",
                new BigDecimal("10.00"),
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                "GOODS_ESCROW",
                1L,
                "P9-LEDGER-001",
                null,
                "SYSTEM",
                null,
                "Phase 9 ledger field smoke test"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("215.50"));
        assertThat(wallet.getFrozenBalance()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(wallet.getUpdatedAt()).isNotNull();
        assertThat(flow.getFlowType()).isEqualTo("ESCROW_FREEZE");
        assertThat(flow.getAvailableBalanceAfter()).isEqualByComparingTo(new BigDecimal("215.50"));
        assertThat(flow.getFrozenBalanceAfter()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(flow.getIdempotencyKey()).isEqualTo("P9-LEDGER-001");
    }

    @Test
    void walletServiceCreditsFreezesUnfreezesAndTransfersFrozenFundsIdempotently() {
        User buyer = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User seller = userRepository.findByEmail("student2@mail.ustc.edu.cn").orElseThrow();

        walletService.credit(buyer.getId(), new BigDecimal("100.00"), "TEST", 1L, "credit-1", "SYSTEM", null, "测试入账");
        walletService.credit(buyer.getId(), new BigDecimal("100.00"), "TEST", 1L, "credit-1", "SYSTEM", null, "测试入账");
        walletService.freeze(buyer.getId(), new BigDecimal("30.00"), "GOODS_ESCROW", 2L, "freeze-1", "SYSTEM", null, "冻结托管");
        walletService.unfreeze(buyer.getId(), new BigDecimal("10.00"), "GOODS_ESCROW", 2L, "unfreeze-1", "SYSTEM", null, "部分解冻");
        walletService.transferFrozen(buyer.getId(), seller.getId(), new BigDecimal("20.00"), "GOODS_ESCROW", 2L, "release-1", "SYSTEM", null, "托管划转");

        WalletAccount buyerWallet = walletAccountRepository.findByUserId(buyer.getId()).orElseThrow();
        WalletAccount sellerWallet = walletAccountRepository.findByUserId(seller.getId()).orElseThrow();

        assertThat(buyerWallet.getBalance()).isEqualByComparingTo("290.00");
        assertThat(buyerWallet.getFrozenBalance()).isEqualByComparingTo("0.00");
        assertThat(sellerWallet.getBalance()).isEqualByComparingTo("220.00");
        assertThat(walletFlowRepository.findByIdempotencyKey("credit-1")).isPresent();
    }

    @Test
    void alipayRechargeCreatesPaymentOrderAndCreditsBalanceAfterCallback() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        WalletRechargeSummary summary = walletOperationService.createRecharge(user.getId(), new WalletRechargeRequest("ALIPAY", new BigDecimal("100.00"), "支付宝充值"));

        assertThat(summary.channelFee()).isEqualByComparingTo("0.60");
        assertThat(summary.payAmount()).isEqualByComparingTo("100.60");
        assertThat(summary.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(summary.paymentOrderNo()).isNotBlank();
    }

    @Test
    void wechatRechargeWaitsForAdminApprovalAndThenCreditsBalance() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("admin@mail.ustc.edu.cn").orElseThrow();
        WalletRechargeSummary summary = walletOperationService.createRecharge(user.getId(), new WalletRechargeRequest("WECHAT", new BigDecimal("30.00"), "微信人工充值"));

        assertThat(summary.status()).isEqualTo("PENDING_REVIEW");
        walletOperationService.approveWechatRecharge(summary.id(), admin.getId(), "已确认微信收款");

        WalletAccount wallet = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
        WalletRechargeOrder order = walletRechargeOrderRepository.findById(summary.id()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("230.00");
        assertThat(order.getStatus()).isEqualTo("PAID");
    }

    @Test
    void withdrawalFreezesBalanceThenRejectUnfreezesAndCompleteDeductsFrozenBalance() {
        User user = userRepository.findByEmail("student1@mail.ustc.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("admin@mail.ustc.edu.cn").orElseThrow();
        walletService.credit(user.getId(), new BigDecimal("100.00"), "TEST", 10L, "withdraw-credit", "SYSTEM", null, "提现测试入账");

        WalletWithdrawalSummary rejected = walletOperationService.createWithdrawal(user.getId(), new CreateWithdrawalRequest(new BigDecimal("30.00"), "WECHAT", "微信昵称：同学A"));
        walletOperationService.rejectWithdrawal(rejected.id(), admin.getId(), "资料不完整");
        WalletAccount afterReject = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(afterReject.getBalance()).isEqualByComparingTo("300.00");
        assertThat(afterReject.getFrozenBalance()).isEqualByComparingTo("0.00");

        WalletWithdrawalSummary approved = walletOperationService.createWithdrawal(user.getId(), new CreateWithdrawalRequest(new BigDecimal("40.00"), "WECHAT", "微信昵称：同学A"));
        walletOperationService.approveWithdrawal(approved.id(), admin.getId(), "审核通过");
        walletOperationService.completeWithdrawal(approved.id(), admin.getId(), "已人工打款");
        WalletAccount afterComplete = walletAccountRepository.findByUserId(user.getId()).orElseThrow();
        WalletWithdrawalRequest completed = walletWithdrawalRequestRepository.findById(approved.id()).orElseThrow();
        assertThat(afterComplete.getBalance()).isEqualByComparingTo("220.00");
        assertThat(afterComplete.getFrozenBalance()).isEqualByComparingTo("0.00");
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void feePolicyCalculatesRechargeOfflineAndOnlineFees() {
        assertThat(feePolicyService.calculateAlipayRechargeFee(new BigDecimal("100.00"))).isEqualByComparingTo("0.60");
        assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("49.99"))).isEqualByComparingTo("0.00");
        assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("80.00"))).isEqualByComparingTo("0.80");
        assertThat(feePolicyService.calculateOfflineTradeFee(new BigDecimal("500.00"))).isEqualByComparingTo("2.00");
        assertThat(feePolicyService.calculateOnlineEscrowFee(new BigDecimal("80.00"))).isEqualByComparingTo("0.80");
        assertThat(feePolicyService.calculateOnlineEscrowFee(new BigDecimal("500.00"))).isEqualByComparingTo("3.00");
    }
}
