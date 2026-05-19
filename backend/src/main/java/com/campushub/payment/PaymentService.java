package com.campushub.payment;

import com.campushub.common.BusinessException;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import com.campushub.wallet.WalletFlow;
import com.campushub.wallet.WalletFlowRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentProvider paymentProvider;
    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;

    public PaymentService(
            PaymentProvider paymentProvider,
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            WalletAccountRepository walletAccountRepository,
            WalletFlowRepository walletFlowRepository) {
        this.paymentProvider = paymentProvider;
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
    }

    public PaymentCreation createServiceFeePayment(Long serviceFeeId) {
        ServiceFeeRecord fee = findServiceFee(serviceFeeId);
        if ("PAID".equals(fee.getStatus())) {
            throw new BusinessException("服务费已支付");
        }
        return paymentProvider.createWebPayment(new PaymentRequest(
                fee.getId(),
                fee.getFeeNo(),
                fee.getAmount(),
                "CampusHub 服务费 " + fee.getFeeNo()));
    }

    @Transactional
    public PaymentStatus markMockServiceFeeSuccess(Long serviceFeeId) {
        ServiceFeeRecord fee = findServiceFee(serviceFeeId);
        if (!"PAID".equals(fee.getStatus())) {
            PaymentStatus providerStatus = paymentProvider.queryPaymentStatus(fee.getFeeNo());
            if (!"SUCCESS".equals(providerStatus.status())) {
                throw new BusinessException("支付尚未成功");
            }
            fee.markPaid(LocalDateTime.now());
            serviceFeeRecordRepository.save(fee);
            WalletAccount wallet = walletAccountRepository.findByUserId(fee.getPayer().getId())
                    .orElseThrow(() -> new BusinessException("付款用户钱包不存在"));
            walletFlowRepository.save(new WalletFlow(
                    wallet,
                    fee.getPayer(),
                    "WF-FEE-" + fee.getFeeNo(),
                    "OUT",
                    fee.getAmount(),
                    wallet.getBalance(),
                    "SERVICE_FEE",
                    fee.getId(),
                    "本地模拟支付服务费"));
        }
        return new PaymentStatus(paymentProvider.providerName(), fee.getFeeNo(), "SUCCESS", "服务费已标记为本地支付成功。 ");
    }

    private ServiceFeeRecord findServiceFee(Long serviceFeeId) {
        return serviceFeeRecordRepository.findById(serviceFeeId)
                .orElseThrow(() -> new BusinessException("服务费记录不存在"));
    }
}
