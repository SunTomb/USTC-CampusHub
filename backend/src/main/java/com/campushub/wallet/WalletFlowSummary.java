package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletFlowSummary(
        Long id,
        String flowNo,
        String direction,
        String flowType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        BigDecimal availableBalanceAfter,
        BigDecimal frozenBalanceAfter,
        String businessType,
        Long businessId,
        String idempotencyKey,
        Long counterpartyUserId,
        String counterpartyNickname,
        String createdBy,
        Long operatorId,
        String operatorNickname,
        String remark,
        LocalDateTime createdAt) {

    public static WalletFlowSummary from(WalletFlow flow) {
        return new WalletFlowSummary(
                flow.getId(),
                flow.getFlowNo(),
                flow.getDirection(),
                flow.getFlowType(),
                flow.getAmount(),
                flow.getBalanceAfter(),
                flow.getAvailableBalanceAfter(),
                flow.getFrozenBalanceAfter(),
                flow.getBusinessType(),
                flow.getBusinessId(),
                flow.getIdempotencyKey(),
                flow.getCounterpartyUser() == null ? null : flow.getCounterpartyUser().getId(),
                flow.getCounterpartyUser() == null ? null : flow.getCounterpartyUser().getNickname(),
                flow.getCreatedBy(),
                flow.getOperator() == null ? null : flow.getOperator().getId(),
                flow.getOperator() == null ? null : flow.getOperator().getNickname(),
                flow.getRemark(),
                flow.getCreatedAt());
    }
}
