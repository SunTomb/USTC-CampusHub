package com.campushub.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateRunnerTaskRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull BigDecimal rewardAmount,
        BigDecimal depositAmount,
        @NotBlank String acceptanceMode,
        @NotBlank String originZone,
        @NotBlank String destinationZone,
        String originDetail,
        String destinationDetail,
        @NotNull LocalDateTime deadline,
        @NotBlank String verificationMode) {
}
