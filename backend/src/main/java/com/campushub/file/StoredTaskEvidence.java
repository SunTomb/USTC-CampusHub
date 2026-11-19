package com.campushub.file;

public record StoredTaskEvidence(
        String storageKey,
        String originalName,
        String contentType,
        long sizeBytes,
        String sha256) {
}
