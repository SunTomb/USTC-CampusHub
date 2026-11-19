package com.campushub.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateRunnerTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull BigDecimal rewardAmount,
        BigDecimal depositAmount,
        @NotBlank String acceptanceMode,
        @NotBlank String originZone,
        @NotBlank String destinationZone,
        String originDetail,
        String destinationDetail,
        @NotNull LocalDateTime deadline,
        @NotBlank String verificationMode,
        @NotNull @Min(1) @Max(100000) @JsonDeserialize(using = WeightGramsDeserializer.class) Integer weightGrams,
        @Size(max = 1000) String notes,
        @Size(max = 120) String pickupCode) {
}
