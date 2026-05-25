package com.campushub.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IdentityServiceIntegrationTest {

    @Autowired
    private IdentityService identityService;

    @Test
    void runnerApplicationAutoApprovesWithFiveYuanDeposit() {
        RoleApplicationSummary summary = identityService.apply(1L, new ApplyRoleRequest("RUNNER", "想接校园跑腿任务"));

        assertThat(summary.roleType()).isEqualTo("RUNNER");
        assertThat(summary.depositAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(summary.depositStatus()).isEqualTo("PENDING");
        assertThat(summary.reviewStatus()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void applyingAgainForPendingPaymentRoleReusesApplicationAndListsIt() {
        RoleApplicationSummary first = identityService.apply(3L, new ApplyRoleRequest("RUNNER", "第一次申请跑腿身份"));
        RoleApplicationSummary second = identityService.apply(3L, new ApplyRoleRequest("RUNNER", "继续支付保证金"));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.applyNote()).isEqualTo("继续支付保证金");
        assertThat(second.depositStatus()).isEqualTo("PENDING");
        assertThat(second.reviewStatus()).isEqualTo("PENDING_PAYMENT");

        assertThat(identityService.listUserApplications(3L))
                .filteredOn(application -> application.id().equals(first.id()))
                .singleElement()
                .extracting(RoleApplicationSummary::roleType)
                .isEqualTo("RUNNER");
    }

    @Test
    void shopMerchantApplicationRequiresManualReviewWithTwentyYuanDeposit() {
        RoleApplicationSummary summary = identityService.apply(2L, new ApplyRoleRequest("SHOP_MERCHANT", "申请开通学生技能店铺"));

        assertThat(summary.roleType()).isEqualTo("SHOP_MERCHANT");
        assertThat(summary.depositAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(summary.depositStatus()).isEqualTo("PENDING");
        assertThat(summary.reviewStatus()).isEqualTo("PENDING_PAYMENT");
    }
}
