package com.campushub.shop;

import java.math.BigDecimal;

public record ShopSummary(
        Long id,
        String name,
        String description,
        String ownerNickname,
        String serviceArea,
        BigDecimal rating) {

    public static ShopSummary from(Shop shop) {
        return new ShopSummary(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getOwner().getNickname(),
                shop.getServiceArea(),
                shop.getRating());
    }
}
