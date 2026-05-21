package com.campushub.shop;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceItemSummary(
        Long id,
        Long shopId,
        String shopName,
        String category,
        String title,
        String description,
        BigDecimal price,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String priceUnit,
        Long coverFileId,
        Integer durationMinutes,
        String status,
        LocalDateTime createdAt) {

    public static ServiceItemSummary from(ServiceItem item) {
        return new ServiceItemSummary(
                item.getId(),
                item.getShop().getId(),
                item.getShop().getName(),
                item.getCategory(),
                item.getTitle(),
                item.getDescription(),
                item.getPrice(),
                item.getMinPrice(),
                item.getMaxPrice(),
                item.getPriceUnit(),
                item.getCoverFileId(),
                item.getDurationMinutes(),
                item.getStatus(),
                item.getCreatedAt());
    }
}
