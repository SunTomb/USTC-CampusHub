package com.campushub.shop;

import jakarta.validation.constraints.Size;

public record ServiceOrderActionRequest(Long actorId, @Size(max = 255) String note, @Size(max = 500) String cancelReason) {
}
