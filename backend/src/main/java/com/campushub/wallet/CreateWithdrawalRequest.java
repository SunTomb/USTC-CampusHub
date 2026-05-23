package com.campushub.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateWithdrawalRequest(
        @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String channel,
        String accountSnapshot) {
}
