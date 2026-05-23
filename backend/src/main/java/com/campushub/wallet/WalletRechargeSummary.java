package com.campushub.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletRechargeSummary(
        Long id,
        String rechargeNo,
        Long userId,
        String userNickname,
        String channel,
        BigDecimal amount,
        BigDecimal channelFee,
        BigDecimal payAmount,
        String status,
        String paymentOrderNo,
        String reviewNote,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt) {

    public static WalletRechargeSummary from(WalletRechargeOrder order) {
        return new WalletRechargeSummary(
                order.getId(),
                order.getRechargeNo(),
                order.getUser().getId(),
                order.getUser().getNickname(),
                order.getChannel(),
                order.getAmount(),
                order.getChannelFee(),
                order.getPayAmount(),
                order.getStatus(),
                order.getPaymentOrderNo(),
                order.getReviewNote(),
                order.getReviewedAt(),
                order.getCreatedAt());
    }
}
