package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletWithdrawalSummary(
        Long id,
        String withdrawalNo,
        Long userId,
        String userNickname,
        BigDecimal amount,
        String channel,
        String accountSnapshot,
        String status,
        String reviewNote,
        LocalDateTime reviewedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt) {

    public static WalletWithdrawalSummary from(WalletWithdrawalRequest request) {
        return new WalletWithdrawalSummary(
                request.getId(),
                request.getWithdrawalNo(),
                request.getUser().getId(),
                request.getUser().getNickname(),
                request.getAmount(),
                request.getChannel(),
                request.getAccountSnapshot(),
                request.getStatus(),
                request.getReviewNote(),
                request.getReviewedAt(),
                request.getCompletedAt(),
                request.getCreatedAt());
    }
}
