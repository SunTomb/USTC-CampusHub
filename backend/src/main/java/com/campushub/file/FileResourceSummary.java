package com.campushub.file;

import java.time.LocalDateTime;

public record FileResourceSummary(
        Long id,
        Long uploaderId,
        String uploaderNickname,
        String originalName,
        String storagePath,
        String contentType,
        Long sizeBytes,
        String status,
        LocalDateTime createdAt) {

    public static FileResourceSummary from(FileResource file) {
        return new FileResourceSummary(
                file.getId(),
                file.getUploader().getId(),
                file.getUploader().getNickname(),
                file.getOriginalName(),
                file.getStoragePath(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getStatus(),
                file.getCreatedAt());
    }
}
