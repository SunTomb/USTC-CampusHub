package com.campushub.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        @NotBlank String verificationMode,
        @NotNull @Positive BigDecimal weight,
        String publicNotes,
        String pickupCode,
        String privatePickupNotes) {

    public CreateRunnerTaskRequest(
            String title,
            String description,
            BigDecimal rewardAmount,
            BigDecimal depositAmount,
            String acceptanceMode,
            String originZone,
            String destinationZone,
            String originDetail,
            String destinationDetail,
            LocalDateTime deadline,
            String verificationMode) {
        this(title, description, rewardAmount, depositAmount, acceptanceMode, originZone, destinationZone,
                originDetail, destinationDetail, deadline, verificationMode, BigDecimal.ONE, null, null, null);
    }
}
