package com.campushub.identity;

import java.math.BigDecimal;

public enum PlatformRoleType {
    RUNNER(new BigDecimal("5.00"), false),
    GOODS_PUBLISHER(new BigDecimal("10.00"), false),
    SHOP_MERCHANT(new BigDecimal("20.00"), true);

    private final BigDecimal depositAmount;
    private final boolean manualReviewRequired;

    PlatformRoleType(BigDecimal depositAmount, boolean manualReviewRequired) {
        this.depositAmount = depositAmount;
        this.manualReviewRequired = manualReviewRequired;
    }

    public BigDecimal depositAmount() {
        return depositAmount;
    }

    public boolean manualReviewRequired() {
        return manualReviewRequired;
    }
}
