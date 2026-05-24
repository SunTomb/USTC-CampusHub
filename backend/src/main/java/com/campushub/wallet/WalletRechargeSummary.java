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
        LocalDateTime createdAt,
        String paymentProvider,
        String paymentPayUrl,
        String wechatQrUrl,
        String wechatNote) {

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
                order.getCreatedAt(),
                null,
                null,
                null,
                null);
    }

    public WalletRechargeSummary withPaymentInfo(String paymentProvider, String paymentPayUrl) {
        return new WalletRechargeSummary(
                id,
                rechargeNo,
                userId,
                userNickname,
                channel,
                amount,
                channelFee,
                payAmount,
                status,
                paymentOrderNo,
                reviewNote,
                reviewedAt,
                createdAt,
                paymentProvider,
                paymentPayUrl,
                wechatQrUrl,
                wechatNote);
    }

    public WalletRechargeSummary withWechatManualInfo(String wechatQrUrl, String wechatNote) {
        return new WalletRechargeSummary(
                id,
                rechargeNo,
                userId,
                userNickname,
                channel,
                amount,
                channelFee,
                payAmount,
                status,
                paymentOrderNo,
                reviewNote,
                reviewedAt,
                createdAt,
                paymentProvider,
                paymentPayUrl,
                wechatQrUrl,
                wechatNote);
    }
}
