package com.campushub.wallet;

import com.campushub.common.BusinessException;
import com.campushub.payment.PaymentCenterProperties;
import com.campushub.payment.PaymentCreation;
import com.campushub.payment.PaymentOrder;
import com.campushub.payment.PaymentOrderRepository;
import com.campushub.payment.PaymentProvider;
import com.campushub.payment.PaymentRequest;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOperationService {

    private final WalletRechargeOrderRepository rechargeOrderRepository;
    private final WalletWithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final FeePolicyService feePolicyService;
    private final PaymentProvider paymentProvider;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentCenterProperties paymentCenterProperties;
    private final WalletService walletService;

    public WalletOperationService(
            WalletRechargeOrderRepository rechargeOrderRepository,
            WalletWithdrawalRequestRepository withdrawalRequestRepository,
            UserRepository userRepository,
            FeePolicyService feePolicyService,
            PaymentProvider paymentProvider,
            PaymentOrderRepository paymentOrderRepository,
            PaymentCenterProperties paymentCenterProperties,
            WalletService walletService) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.userRepository = userRepository;
        this.feePolicyService = feePolicyService;
        this.paymentProvider = paymentProvider;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentCenterProperties = paymentCenterProperties;
        this.walletService = walletService;
    }

    @Transactional
    public WalletRechargeSummary createRecharge(Long userId, WalletRechargeRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if ("ALIPAY".equals(request.channel())) {
            return createAlipayRecharge(user, request.amount());
        }
        if ("WECHAT".equals(request.channel())) {
            WalletRechargeOrder order = rechargeOrderRepository.save(new WalletRechargeOrder(nextNo("WR"), user, "WECHAT", request.amount(), BigDecimal.ZERO, request.amount(), "PENDING_REVIEW"));
            return WalletRechargeSummary.from(order);
        }
        throw new BusinessException("不支持的充值渠道");
    }

    @Transactional
    public void markRechargePaidByPaymentOrder(String paymentOrderNo) {
        WalletRechargeOrder order = rechargeOrderRepository.findByPaymentOrderNo(paymentOrderNo)
                .orElseThrow(() -> new BusinessException("充值订单不存在"));
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        order.markPaid();
        walletService.credit(order.getUser().getId(), order.getAmount(), "WALLET_RECHARGE", order.getId(), "recharge:" + order.getId(), "PAYMENT_CALLBACK", null, "钱包充值到账");
    }

    @Transactional
    public WalletRechargeSummary approveWechatRecharge(Long rechargeId, Long adminId, String note) {
        WalletRechargeOrder order = rechargeOrderRepository.findById(rechargeId).orElseThrow(() -> new BusinessException("充值订单不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            throw new BusinessException("充值订单状态不可审核通过");
        }
        order.approve(admin, note);
        walletService.credit(order.getUser().getId(), order.getAmount(), "WALLET_RECHARGE", order.getId(), "wechat-recharge:" + order.getId(), "ADMIN", adminId, "微信人工充值到账");
        return WalletRechargeSummary.from(order);
    }

    @Transactional
    public WalletRechargeSummary rejectWechatRecharge(Long rechargeId, Long adminId, String note) {
        WalletRechargeOrder order = rechargeOrderRepository.findById(rechargeId).orElseThrow(() -> new BusinessException("充值订单不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"PENDING_REVIEW".equals(order.getStatus())) {
            throw new BusinessException("充值订单状态不可拒绝");
        }
        order.reject(admin, note);
        return WalletRechargeSummary.from(order);
    }

    @Transactional(readOnly = true)
    public List<WalletRechargeSummary> listUserRecharges(Long userId) {
        return rechargeOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletRechargeSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WalletRechargeSummary> listAdminRecharges(String status) {
        List<WalletRechargeOrder> orders = status == null || status.isBlank()
                ? rechargeOrderRepository.findTop200ByOrderByCreatedAtDesc()
                : rechargeOrderRepository.findTop200ByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(WalletRechargeSummary::from).toList();
    }

    @Transactional
    public WalletWithdrawalSummary createWithdrawal(Long userId, CreateWithdrawalRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.save(new WalletWithdrawalRequest(nextNo("WW"), user, request.amount(), request.channel(), request.accountSnapshot()));
        walletService.freeze(userId, request.amount(), "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-freeze:" + withdrawal.getId(), "USER", userId, "提现申请冻结余额");
        return WalletWithdrawalSummary.from(withdrawal);
    }

    @Transactional
    public WalletWithdrawalSummary approveWithdrawal(Long withdrawalId, Long adminId, String note) {
        WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"PENDING_REVIEW".equals(withdrawal.getStatus())) {
            throw new BusinessException("提现申请状态不可审核通过");
        }
        withdrawal.approve(admin, note);
        return WalletWithdrawalSummary.from(withdrawal);
    }

    @Transactional
    public WalletWithdrawalSummary rejectWithdrawal(Long withdrawalId, Long adminId, String note) {
        WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"PENDING_REVIEW".equals(withdrawal.getStatus())) {
            throw new BusinessException("提现申请状态不可拒绝");
        }
        withdrawal.reject(admin, note);
        walletService.unfreeze(withdrawal.getUser().getId(), withdrawal.getAmount(), "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-reject:" + withdrawal.getId(), "ADMIN", adminId, "提现拒绝解冻余额");
        return WalletWithdrawalSummary.from(withdrawal);
    }

    @Transactional
    public WalletWithdrawalSummary completeWithdrawal(Long withdrawalId, Long adminId, String note) {
        WalletWithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId).orElseThrow(() -> new BusinessException("提现申请不存在"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!"APPROVED".equals(withdrawal.getStatus())) {
            throw new BusinessException("提现申请状态不可完成");
        }
        walletService.debit(withdrawal.getUser().getId(), withdrawal.getAmount(), "WITHDRAW", "WALLET_WITHDRAWAL", withdrawal.getId(), "withdraw-complete:" + withdrawal.getId(), "ADMIN", adminId, "提现人工打款完成");
        withdrawal.complete(admin, note);
        return WalletWithdrawalSummary.from(withdrawal);
    }

    @Transactional(readOnly = true)
    public List<WalletWithdrawalSummary> listUserWithdrawals(Long userId) {
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletWithdrawalSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WalletWithdrawalSummary> listAdminWithdrawals(String status) {
        List<WalletWithdrawalRequest> requests = status == null || status.isBlank()
                ? withdrawalRequestRepository.findTop200ByOrderByCreatedAtDesc()
                : withdrawalRequestRepository.findTop200ByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(WalletWithdrawalSummary::from).toList();
    }

    private WalletRechargeSummary createAlipayRecharge(User user, BigDecimal amount) {
        BigDecimal fee = feePolicyService.calculateAlipayRechargeFee(amount);
        BigDecimal payAmount = amount.add(fee);
        WalletRechargeOrder recharge = rechargeOrderRepository.save(new WalletRechargeOrder(nextNo("WR"), user, "ALIPAY", amount, fee, payAmount, "PENDING_PAYMENT"));
        PaymentOrder order = paymentOrderRepository.save(new PaymentOrder(nextNo("CHP-WR"), "WALLET_RECHARGE", recharge.getId(), user, payAmount, paymentProvider.providerName(), LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes())));
        PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(order.getOrderNo(), "WALLET_RECHARGE", recharge.getId(), user.getId(), recharge.getRechargeNo(), payAmount, "CampusHub 钱包充值 " + recharge.getRechargeNo(), paymentCenterProperties.getCallbackUrl(), paymentCenterProperties.getExpireMinutes()));
        order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
        recharge.attachPaymentOrder(order.getOrderNo());
        return WalletRechargeSummary.from(recharge);
    }

    private String nextNo(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }
}
