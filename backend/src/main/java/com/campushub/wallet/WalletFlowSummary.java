package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletFlowSummary(
        Long id,
        String flowNo,
        String direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String businessType,
        Long businessId,
        String remark,
        LocalDateTime createdAt) {

    public static WalletFlowSummary from(WalletFlow flow) {
        return new WalletFlowSummary(
                flow.getId(),
                flow.getFlowNo(),
                flow.getDirection(),
                flow.getAmount(),
                flow.getBalanceAfter(),
                flow.getBusinessType(),
                flow.getBusinessId(),
                flow.getRemark(),
                flow.getCreatedAt());
    }
}
