package com.campushub.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import com.campushub.platform.PlatformProtocolConsentRepository;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityServiceIntegrationTest {
    @Autowired IdentityService identities;
    @Autowired UserRoleService roles;
    @Autowired UserRepository users;
    @Autowired RoleApplicationRepository applications;
    @Autowired PlatformProtocolConsentRepository consents;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void staleApplicationCannotOverwriteAConcurrentProtocolConversion() {
        Long user = newUser();
        var legacy = applications.saveAndFlush(new RoleApplication(users.findById(user).orElseThrow(), PlatformRoleType.RUNNER, "旧申请"));
        jdbc.update("UPDATE role_applications SET row_version = row_version + 1, agreement_status = 'CONSENTED' WHERE id = ?", legacy.getId());
        legacy.markDepositFailed("late failure");
        assertThatThrownBy(() -> applications.saveAndFlush(legacy))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void runnerAndPublisherAcceptCurrentAgreementWithoutMovingMoney() {
        for (String role : new String[]{"RUNNER", "GOODS_PUBLISHER"}) {
            Long user = newUser();
            var result = identities.apply(user, new ApplyRoleRequest(role, "", "v1.0", true));
            assertThat(result.depositAmount()).isZero();
            assertThat(result.depositStatus()).isEqualTo("NOT_REQUIRED");
            assertThat(result.reviewStatus()).isEqualTo("APPROVED");
            assertThat(result.agreementStatus()).isEqualTo("CONSENTED");
            assertThat(result.agreementConsentedAt()).isNotNull();
            assertThat(roles.findRoleCodes(user)).contains("ROLE_" + role);
        }
    }

    @Test
    void falseOrStaleConsentDoesNotPersistAnApplication() {
        Long user = newUser();
        long before = consents.count();
        assertThatThrownBy(() -> identities.apply(user, new ApplyRoleRequest("RUNNER", "")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("同意");
        assertThatThrownBy(() -> identities.apply(user, new ApplyRoleRequest("RUNNER", "", "old", true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("版本");
        assertThat(consents.count()).isEqualTo(before);
        assertThat(applications.findByUserIdAndRoleType(user, "RUNNER")).isEmpty();
    }

    @Test
    void repeatedApplicationDoesNotFabricateAnotherAcceptance() {
        Long user = newUser();
        var first = identities.apply(user, new ApplyRoleRequest("RUNNER", "", "v1.0", true));
        long count = consents.count();
        var repeated = identities.apply(user, new ApplyRoleRequest("RUNNER", "retry"));
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(repeated.agreementConsentedAt()).isEqualTo(first.agreementConsentedAt());
        assertThat(consents.count()).isEqualTo(count);
    }

    @Test
    void grandfatheredRoleWithoutApplicationDoesNotRequireAnAcceptance() {
        Long user = newUser();
        roles.assignRole(user, "ROLE_RUNNER");
        long count = consents.count();
        assertThatThrownBy(() -> identities.apply(user, new ApplyRoleRequest("RUNNER", "")))
                .hasMessageContaining("无需重新同意");
        assertThat(roles.findRoleCodes(user)).contains("ROLE_RUNNER");
        assertThat(consents.count()).isEqualTo(count);
    }

    @Test
    void unpaidLegacyApplicationKeepsFinancialHistoryWhenConverted() {
        Long user = newUser();
        var legacy = new RoleApplication(users.findById(user).orElseThrow(), PlatformRoleType.GOODS_PUBLISHER, "旧申请");
        legacy.attachDepositPaymentOrder("historical-order");
        legacy = applications.saveAndFlush(legacy);
        var converted = identities.apply(user, new ApplyRoleRequest("GOODS_PUBLISHER", "新申请", "v1.0", true));
        assertThat(converted.id()).isEqualTo(legacy.getId());
        assertThat(converted.depositAmount()).isEqualByComparingTo("10.00");
        assertThat(converted.depositPaymentOrderNo()).isEqualTo("historical-order");
        assertThat(converted.depositPolicy()).isEqualTo("NO_DEPOSIT");
        assertThat(converted.reviewStatus()).isEqualTo("APPROVED");
    }

    @Test
    void merchantWaitsForShowcaseReviewAndCannotApproveAdministratorApplications() {
        Long user = newUser();
        Long reviewer = newUser();
        roles.assignRole(reviewer, "ROLE_SHOWCASE_ADMIN");
        var merchant = identities.apply(user, new ApplyRoleRequest("SHOP_MERCHANT", "", "v1.0", true));
        assertThat(merchant.reviewStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(roles.findRoleCodes(user)).doesNotContain("ROLE_SHOP_MERCHANT");
        identities.approve(merchant.id(), reviewer);
        assertThat(roles.findRoleCodes(user)).contains("ROLE_SHOP_MERCHANT");
        var admin = identities.apply(user, new ApplyRoleRequest("TRADE_ADMIN", ""));
        assertThatThrownBy(() -> identities.approve(admin.id(), reviewer)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> identities.reject(admin.id(), reviewer)).isInstanceOf(ResponseStatusException.class);
    }

    private Long newUser() {
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return users.saveAndFlush(new User(key, "u_" + key, "hash", "未认证", "测试用户", key,
                key + "@example.edu.cn", "ACTIVE")).getId();
    }
}
