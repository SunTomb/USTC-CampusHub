package com.campushub.projectad;

import java.time.LocalDateTime;

public record ProjectAdSummary(
        Long id,
        String title,
        String description,
        String publisherNickname,
        String linkUrl,
        String contactInfo,
        Integer viewCount,
        LocalDateTime createdAt) {

    public static ProjectAdSummary from(ProjectAd projectAd) {
        return new ProjectAdSummary(
                projectAd.getId(),
                projectAd.getTitle(),
                projectAd.getDescription(),
                projectAd.getPublisher().getNickname(),
                projectAd.getLinkUrl(),
                projectAd.getContactInfo(),
                projectAd.getViewCount(),
                projectAd.getCreatedAt());
    }
}
