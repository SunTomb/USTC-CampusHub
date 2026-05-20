package com.campushub.goods;

import java.time.LocalDateTime;

public record GoodsIntentSummary(
        Long id,
        Long goodsId,
        Long buyerId,
        String buyerNickname,
        Long sellerId,
        String sellerNickname,
        String message,
        String contactSnapshot,
        String status,
        Long serviceFeeId,
        LocalDateTime createdAt) {

    public static GoodsIntentSummary from(GoodsIntent intent) {
        return new GoodsIntentSummary(
                intent.getId(),
                intent.getGoods().getId(),
                intent.getBuyer().getId(),
                intent.getBuyer().getNickname(),
                intent.getSeller().getId(),
                intent.getSeller().getNickname(),
                intent.getMessage(),
                intent.getContactSnapshot(),
                intent.getStatus(),
                intent.getServiceFee() == null ? null : intent.getServiceFee().getId(),
                intent.getCreatedAt());
    }
}
