package com.campushub.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import org.junit.jupiter.api.Test;

class RoleApplicationTest {

    @Test
    void failedDepositReturnsApplicationToPendingPaymentForRetry() {
        RoleApplication application = new RoleApplication(testUser(), PlatformRoleType.RUNNER, "first try");
        application.attachDepositPaymentOrder("CHP-RD-1");
        application.markDepositPaid();

        application.markDepositFailed("payment center failed");

        assertThat(application.getDepositStatus()).isEqualTo("FAILED");
        assertThat(application.getReviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(application.getDepositPaymentOrderNo()).isEqualTo("CHP-RD-1");
        assertThat(application.isRecoverableUnpaid()).isTrue();
    }

    @Test
    void expiredDepositReturnsApplicationToPendingPaymentForRetry() {
        RoleApplication application = new RoleApplication(testUser(), PlatformRoleType.GOODS_PUBLISHER, "first try");
        application.attachDepositPaymentOrder("CHP-RD-2");
        application.markDepositPaid();

        application.markDepositExpired();

        assertThat(application.getDepositStatus()).isEqualTo("EXPIRED");
        assertThat(application.getReviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(application.getDepositPaymentOrderNo()).isEqualTo("CHP-RD-2");
        assertThat(application.isRecoverableUnpaid()).isTrue();
    }

    @Test
    void resetForPaymentClearsFailedDepositOrderForNewRetry() {
        RoleApplication application = new RoleApplication(testUser(), PlatformRoleType.SHOP_MERCHANT, "first try");
        application.attachDepositPaymentOrder("CHP-RD-3");
        application.markDepositFailed("payment center failed");

        application.resetForPayment("retry payment");

        assertThat(application.getDepositStatus()).isEqualTo("PENDING");
        assertThat(application.getReviewStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(application.getDepositPaymentOrderNo()).isNull();
        assertThat(application.getApplyNote()).isEqualTo("retry payment");
    }

    private User testUser() {
        return new User(
                "PB23999999",
                "role_test_user",
                "hash",
                "测试用户",
                "测试用户",
                "13999999999",
                "role-test@mail.ustc.edu.cn",
                "ACTIVE");
    }
}
