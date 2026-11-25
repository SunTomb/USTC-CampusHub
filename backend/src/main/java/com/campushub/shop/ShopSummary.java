package com.campushub.shop;

import java.math.BigDecimal;

public record ShopSummary(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerNickname,
        String serviceArea,
        String campusZone,
        String contactVisibility,
        String openingHours,
        Long coverFileId,
        String status,
        BigDecimal rating) {

    public static ShopSummary from(Shop shop) {
        return from(shop,null);
    }
    public static ShopSummary from(Shop shop, BigDecimal score) {
        return new ShopSummary(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getOwner().getId(),
                shop.getOwner().getNickname(),
                shop.getServiceArea(),
                shop.getCampusZone(),
                shop.getContactVisibility(),
                shop.getOpeningHours(),
                shop.getCoverFileId(),
                shop.getStatus(),
                score);
    }
}
