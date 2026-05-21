package com.campushub.shop;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShopDetailSummary(
        Long id,
        String name,
        String description,
        Long ownerId,
        String ownerNickname,
        Integer ownerCreditScore,
        String serviceArea,
        String campusZone,
        String contactVisibility,
        String openingHours,
        Long coverFileId,
        String status,
        BigDecimal rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean contactVisible,
        String contactSnapshot,
        List<ServiceItemSummary> serviceItems) {

    public static ShopDetailSummary from(Shop shop, boolean contactVisible, String contactSnapshot, List<ServiceItemSummary> serviceItems) {
        return new ShopDetailSummary(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getOwner().getId(),
                shop.getOwner().getNickname(),
                shop.getOwner().getCreditScore(),
                shop.getServiceArea(),
                shop.getCampusZone(),
                shop.getContactVisibility(),
                shop.getOpeningHours(),
                shop.getCoverFileId(),
                shop.getStatus(),
                shop.getRating(),
                shop.getCreatedAt(),
                shop.getUpdatedAt(),
                contactVisible,
                contactSnapshot,
                serviceItems);
    }
}
