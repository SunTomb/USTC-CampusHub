package com.campushub.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record WalletRechargeRequest(
        @NotBlank String channel,
        @DecimalMin("0.01") BigDecimal amount,
        String remark) {
}
