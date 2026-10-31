package com.campushub.identity;

import java.math.BigDecimal;

public enum PlatformRoleType {
    RUNNER(new BigDecimal("5.00"), false, "ROLE_RUNNER"),
    GOODS_PUBLISHER(new BigDecimal("10.00"), false, "ROLE_GOODS_PUBLISHER"),
    SHOP_MERCHANT(new BigDecimal("20.00"), true, "ROLE_SHOP_MERCHANT"),
    TRADE_ADMIN(BigDecimal.ZERO, true, "ROLE_TRADE_ADMIN"),
    SHOWCASE_ADMIN(BigDecimal.ZERO, true, "ROLE_SHOWCASE_ADMIN");

    private final BigDecimal depositAmount;
    private final boolean manualReviewRequired;
    private final String grantedRoleCode;

    PlatformRoleType(BigDecimal depositAmount, boolean manualReviewRequired, String grantedRoleCode) {
        this.depositAmount = depositAmount;
        this.manualReviewRequired = manualReviewRequired;
        this.grantedRoleCode = grantedRoleCode;
    }

    public BigDecimal depositAmount() {
        return depositAmount;
    }

    public boolean manualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean requiresAgreement() {
        return this == RUNNER || this == GOODS_PUBLISHER || this == SHOP_MERCHANT;
    }

    public boolean requiresDeposit() {
        return depositAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String grantedRoleCode() {
        return grantedRoleCode;
    }
}
