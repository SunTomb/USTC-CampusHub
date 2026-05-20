package com.campushub.goods;

import com.campushub.file.FileBindingSummary;
import com.campushub.interaction.CommentSummary;
import com.campushub.review.ReviewSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsDetailSummary(
        Long id,
        Long sellerId,
        String sellerNickname,
        Integer sellerCreditScore,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        String conditionLevel,
        String tradeLocation,
        String campusZone,
        String deliveryMethod,
        String contactVisibility,
        String status,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        boolean contactVisible,
        String contactSnapshot,
        List<FileBindingSummary> images,
        List<CommentSummary> comments,
        List<ReviewSummary> sellerReviews,
        long favoriteCount,
        boolean favoritedByViewer) {

    public static GoodsDetailSummary from(
            Goods goods,
            boolean contactVisible,
            String contactSnapshot,
            List<FileBindingSummary> images,
            List<CommentSummary> comments,
            List<ReviewSummary> sellerReviews,
            long favoriteCount,
            boolean favoritedByViewer) {
        return new GoodsDetailSummary(
                goods.getId(),
                goods.getSeller().getId(),
                goods.getSeller().getNickname(),
                goods.getSeller().getCreditScore(),
                goods.getTitle(),
                goods.getDescription(),
                goods.getPrice(),
                goods.getOriginalPrice(),
                goods.getConditionLevel(),
                goods.getTradeLocation(),
                goods.getCampusZone(),
                goods.getDeliveryMethod(),
                goods.getContactVisibility(),
                goods.getStatus(),
                goods.getViewCount(),
                goods.getCreatedAt(),
                goods.getPublishedAt(),
                goods.getUpdatedAt(),
                contactVisible,
                contactSnapshot,
                images,
                comments,
                sellerReviews,
                favoriteCount,
                favoritedByViewer);
    }
}
