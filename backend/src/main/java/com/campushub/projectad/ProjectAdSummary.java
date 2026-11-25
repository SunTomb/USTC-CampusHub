package com.campushub.projectad;

import java.time.LocalDateTime;

public record ProjectAdSummary(
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
        String contactInfo,
        String contactVisibility,
        String status,
        Integer viewCount,
        Boolean featured,
        Integer featuredPriority,
        LocalDateTime expiresAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt) {

    public static ProjectAdSummary from(ProjectAd projectAd) {
        return from(projectAd,true);
    }
    public static ProjectAdSummary publicCard(ProjectAd projectAd) {return from(projectAd,"PUBLIC".equals(projectAd.getContactVisibility()));}
    private static ProjectAdSummary from(ProjectAd projectAd,boolean reveal) {
        return new ProjectAdSummary(
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
                reveal ? projectAd.getContactInfo() : null,
                projectAd.getContactVisibility(),
                projectAd.getStatus(),
                projectAd.getViewCount(),
                projectAd.getFeatured(),
                projectAd.getFeaturedPriority(),
                projectAd.getExpiresAt(),
                projectAd.getPublishedAt(),
                projectAd.getCreatedAt());
    }
}
