package com.campushub.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateShopRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 128) String serviceArea,
        @NotBlank @Size(max = 40) String campusZone,
        @NotBlank @Size(max = 40) String contactVisibility,
        @Size(max = 255) String openingHours,
        Long coverFileId) {
}
