package com.campushub.goods;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsOrderSummary(
        Long id,
        String orderNo,
        Long goodsId,
        String goodsTitle,
        Long buyerId,
        String buyerNickname,
        Long sellerId,
        String sellerNickname,
        BigDecimal amount,
        BigDecimal serviceFee,
        String status,
        String contactSnapshot,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt) {

    public static GoodsOrderSummary from(GoodsOrder order) {
        return new GoodsOrderSummary(
                order.getId(),
                order.getOrderNo(),
                order.getGoods().getId(),
                order.getGoods().getTitle(),
                order.getBuyer().getId(),
                order.getBuyer().getNickname(),
                order.getSeller().getId(),
                order.getSeller().getNickname(),
                order.getAmount(),
                order.getServiceFee(),
                order.getStatus(),
                order.getContactSnapshot(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCompletedAt(),
                order.getCanceledAt());
    }
}
