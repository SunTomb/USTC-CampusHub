package com.campushub.projectad;

import com.campushub.file.FileBindingSummary;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectAdDetailSummary(
        Long id,
        String title,
        String adType,
        String summary,
        String description,
        String tags,
        String campusZone,
        Long coverFileId,
        Long publisherId,
        String publisherNickname,
        String linkUrl,
        String status,
        Integer viewCount,
        Boolean featured,
        Integer featuredPriority,
        LocalDateTime expiresAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        String contactVisibility,
        Boolean contactVisible,
        String contactInfo,
        String reviewNote,
        Long favoriteCount,
        Long commentCount,
        Boolean favorited,
        List<FileBindingSummary> attachments) {

    public static ProjectAdDetailSummary from(
            ProjectAd projectAd,
            boolean contactVisible,
            long favoriteCount,
            long commentCount,
            boolean favorited,
            List<FileBindingSummary> attachments) {
        return new ProjectAdDetailSummary(
                projectAd.getId(),
                projectAd.getTitle(),
                projectAd.getAdType(),
                projectAd.getSummary(),
                projectAd.getDescription(),
                projectAd.getTags(),
                projectAd.getCampusZone(),
                projectAd.getCoverFileId(),
                projectAd.getPublisher().getId(),
                projectAd.getPublisher().getNickname(),
                projectAd.getLinkUrl(),
                projectAd.getStatus(),
                projectAd.getViewCount(),
                projectAd.getFeatured(),
                projectAd.getFeaturedPriority(),
                projectAd.getExpiresAt(),
                projectAd.getPublishedAt(),
                projectAd.getCreatedAt(),
                projectAd.getContactVisibility(),
                contactVisible,
                contactVisible ? projectAd.getContactInfo() : null,
                projectAd.getReviewNote(),
                favoriteCount,
                commentCount,
                favorited,
                attachments);
    }
}
