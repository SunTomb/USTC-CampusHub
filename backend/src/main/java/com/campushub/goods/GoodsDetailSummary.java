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
        Long categoryId,
        String categoryName,
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
        LocalDateTime soldAt,
        boolean contactVisible,
        String contactSnapshot,
        String myIntentStatus,
        boolean canEdit,
        boolean canDelete,
        boolean canOffShelf,
        boolean canMarkSold,
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
        return from(goods, null, contactVisible, contactSnapshot, null, false, false, false, false,
                images, comments, sellerReviews, favoriteCount, favoritedByViewer);
    }

    public static GoodsDetailSummary from(
            Goods goods,
            String categoryName,
            boolean contactVisible,
            String contactSnapshot,
            String myIntentStatus,
            boolean canEdit,
            boolean canDelete,
            boolean canOffShelf,
            boolean canMarkSold,
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
                goods.getCategoryId(),
                categoryName,
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
                goods.getSoldAt(),
                contactVisible,
                contactSnapshot,
                myIntentStatus,
                canEdit,
                canDelete,
                canOffShelf,
                canMarkSold,
                images,
                comments,
                sellerReviews,
                favoriteCount,
                favoritedByViewer);
    }
}
