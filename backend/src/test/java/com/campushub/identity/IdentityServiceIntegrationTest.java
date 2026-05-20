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
        assertThat(summary.depositStatus()).isEqualTo("PAID");
        assertThat(summary.reviewStatus()).isEqualTo("APPROVED");
    }

    @Test
    void shopMerchantApplicationRequiresManualReviewWithTwentyYuanDeposit() {
        RoleApplicationSummary summary = identityService.apply(2L, new ApplyRoleRequest("SHOP_MERCHANT", "申请开通学生技能店铺"));

        assertThat(summary.roleType()).isEqualTo("SHOP_MERCHANT");
        assertThat(summary.depositAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(summary.depositStatus()).isEqualTo("PAID");
        assertThat(summary.reviewStatus()).isEqualTo("PENDING_REVIEW");
    }
}
