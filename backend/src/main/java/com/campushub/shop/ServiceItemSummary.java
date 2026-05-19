package com.campushub.shop;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceItemSummary(
        Long id,
        Long shopId,
        String shopName,
        String title,
        String description,
        BigDecimal price,
        Integer durationMinutes,
        String status,
        LocalDateTime createdAt) {

    public static ServiceItemSummary from(ServiceItem item) {
        return new ServiceItemSummary(
                item.getId(),
                item.getShop().getId(),
                item.getShop().getName(),
                item.getTitle(),
                item.getDescription(),
                item.getPrice(),
                item.getDurationMinutes(),
                item.getStatus(),
                item.getCreatedAt());
    }
}
