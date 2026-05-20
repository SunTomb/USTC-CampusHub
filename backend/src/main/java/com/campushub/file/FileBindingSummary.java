package com.campushub.file;

import java.time.LocalDateTime;

public record FileBindingSummary(
        Long id,
        String targetType,
        Long targetId,
        String usageType,
        Integer sortOrder,
        FileResourceSummary file,
        LocalDateTime createdAt) {

    public static FileBindingSummary from(FileBinding binding) {
        return new FileBindingSummary(
                binding.getId(),
                binding.getTargetType(),
                binding.getTargetId(),
                binding.getUsageType(),
                binding.getSortOrder(),
                FileResourceSummary.from(binding.getFile()),
                binding.getCreatedAt());
    }

    public Long fileId() {
        return file.id();
    }

    public String fileName() {
        return file.originalName();
    }

    public String storagePath() {
        return file.storagePath();
    }

    public String contentType() {
        return file.contentType();
    }
}
