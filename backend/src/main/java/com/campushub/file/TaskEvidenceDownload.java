package com.campushub.file;

public record TaskEvidenceDownload(byte[] bytes, String contentType, long sizeBytes) {
}
