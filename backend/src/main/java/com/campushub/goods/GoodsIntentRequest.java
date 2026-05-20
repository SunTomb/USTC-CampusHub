package com.campushub.goods;

import jakarta.validation.constraints.Size;

public record GoodsIntentRequest(@Size(max = 500) String message) {
}
