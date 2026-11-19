package com.campushub.file;

import java.time.LocalDateTime;

public record TaskDeliveryPhotoSummary(
        Long id,
        String originalName,
        String contentType,
        Long sizeBytes,
        String sha256,
        String status,
        String downloadPath,
        String note,
        LocalDateTime createdAt) {

    public static TaskDeliveryPhotoSummary from(TaskDeliveryPhoto photo) {
        return new TaskDeliveryPhotoSummary(
                photo.getId(),
                photo.getOriginalName(),
                photo.getContentType(),
                photo.getSizeBytes(),
                photo.getSha256(),
                photo.getStatus(),
                "/api/tasks/" + photo.getTask().getId() + "/evidence/" + photo.getId(),
                photo.getNote(),
                photo.getCreatedAt());
    }
}
