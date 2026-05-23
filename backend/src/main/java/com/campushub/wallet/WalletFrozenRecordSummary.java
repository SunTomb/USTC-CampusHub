package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletFrozenRecordSummary(
        Long id,
        String freezeNo,
        Long userId,
        String userNickname,
        String businessType,
        Long businessId,
        BigDecimal amount,
        String status,
        LocalDateTime frozenAt,
        LocalDateTime releasedAt,
        String remark) {

    public static WalletFrozenRecordSummary from(WalletFrozenRecord record) {
        return new WalletFrozenRecordSummary(
                record.getId(),
                record.getFreezeNo(),
                record.getUser().getId(),
                record.getUser().getNickname(),
                record.getBusinessType(),
                record.getBusinessId(),
                record.getAmount(),
                record.getStatus(),
                record.getFrozenAt(),
                record.getReleasedAt(),
                record.getRemark());
    }
}
