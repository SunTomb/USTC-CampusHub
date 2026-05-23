package com.campushub.wallet;

import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FeePolicyService {

    private static final BigDecimal ALIPAY_RECHARGE_RATE = new BigDecimal("0.006");
    private static final BigDecimal TRADE_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal OFFLINE_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal OFFLINE_CAP = new BigDecimal("2.00");
    private static final BigDecimal ONLINE_CAP = new BigDecimal("3.00");

    public BigDecimal calculateAlipayRechargeFee(BigDecimal amount) {
        ensurePositive(amount);
        return amount.multiply(ALIPAY_RECHARGE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOfflineTradeFee(BigDecimal amount) {
        ensurePositive(amount);
        if (amount.compareTo(OFFLINE_THRESHOLD) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(TRADE_FEE_RATE).min(OFFLINE_CAP).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOnlineEscrowFee(BigDecimal amount) {
        ensurePositive(amount);
        return amount.multiply(TRADE_FEE_RATE).min(ONLINE_CAP).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于 0");
        }
    }
}
