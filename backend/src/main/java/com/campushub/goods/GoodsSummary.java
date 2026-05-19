package com.campushub.goods;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoodsSummary(
        Long id,
        String title,
        String description,
        BigDecimal price,
        String sellerNickname,
        String tradeLocation,
        String conditionLevel,
        Integer viewCount,
        LocalDateTime createdAt) {

    public static GoodsSummary from(Goods goods) {
        return new GoodsSummary(
                goods.getId(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getPrice(),
                goods.getSeller().getNickname(),
                goods.getTradeLocation(),
                goods.getConditionLevel(),
                goods.getViewCount(),
                goods.getCreatedAt());
    }
}
