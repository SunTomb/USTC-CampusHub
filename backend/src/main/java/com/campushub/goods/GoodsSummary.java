package com.campushub.goods;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsSummary(
        Long id,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Long sellerId,
        String sellerNickname,
        Integer sellerCreditScore,
        String tradeLocation,
        String campusZone,
        String conditionLevel,
        String status,
        Integer viewCount,
        LocalDateTime createdAt,
        String coverUrl) {

    public static GoodsSummary from(Goods goods) {
        return from(goods, null);
    }

    public static GoodsSummary from(Goods goods, String coverUrl) {
        return new GoodsSummary(
                goods.getId(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getPrice(),
                goods.getOriginalPrice(),
                goods.getSeller().getId(),
                goods.getSeller().getNickname(),
                goods.getSeller().getCreditScore(),
                goods.getTradeLocation(),
                goods.getCampusZone(),
                goods.getConditionLevel(),
                goods.getStatus(),
                goods.getViewCount(),
                goods.getCreatedAt(),
                coverUrl);
    }
}
