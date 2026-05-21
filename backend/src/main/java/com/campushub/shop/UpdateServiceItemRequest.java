package com.campushub.shop;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateServiceItemRequest(
        @NotBlank @Size(max = 40) String category,
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        @NotBlank @Size(max = 30) String priceUnit,
        @NotNull @Min(1) Integer durationMinutes,
        Long coverFileId) {
}
