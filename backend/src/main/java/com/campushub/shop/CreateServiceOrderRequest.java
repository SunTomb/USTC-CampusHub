package com.campushub.shop;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateServiceOrderRequest(
        @NotNull @Future LocalDateTime appointmentTime,
        BigDecimal amount,
        @Size(max = 255) String note) {
}
