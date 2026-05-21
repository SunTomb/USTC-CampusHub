package com.campushub.projectad;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ProjectAdRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String adType,
        @Size(max = 500) String summary,
        @NotBlank String description,
        @Size(max = 500) String tags,
        String campusZone,
        Long coverFileId,
        String linkUrl,
        @NotBlank @Size(max = 255) String contactInfo,
        @NotBlank String contactVisibility,
        @Future LocalDateTime expiresAt) {
}
