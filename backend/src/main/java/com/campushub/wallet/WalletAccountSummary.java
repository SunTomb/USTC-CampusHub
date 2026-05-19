package com.campushub.wallet;

import java.math.BigDecimal;

public record WalletAccountSummary(
        Long id,
        Long userId,
        String nickname,
        BigDecimal balance,
        BigDecimal frozenBalance,
        String status) {

    public static WalletAccountSummary from(WalletAccount account) {
        return new WalletAccountSummary(
                account.getId(),
                account.getUser().getId(),
                account.getUser().getNickname(),
                account.getBalance(),
                account.getFrozenBalance(),
                account.getStatus());
    }
}
