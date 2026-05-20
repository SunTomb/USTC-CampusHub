package com.campushub.goods;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateGoodsRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        BigDecimal originalPrice,
        @NotBlank @Size(max = 40) String conditionLevel,
        @NotBlank @Size(max = 40) String campusZone,
        @NotBlank @Size(max = 120) String tradeLocation,
        @NotBlank @Size(max = 40) String deliveryMethod,
        @NotBlank @Size(max = 40) String contactVisibility) {
}
