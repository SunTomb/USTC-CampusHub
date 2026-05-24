package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BetaDemoAccountInitializerValidationTest {

    @Test
    void rejectsPlaceholderPasswordsWhenResetIsEnabled() {
        BetaDemoProperties properties = new BetaDemoProperties();
        properties.setDemoResetEnabled(true);
        properties.setStudent(account("change-me"));
        properties.setAdmin(account("BetaAdminPass123!"));
        BetaDemoAccountInitializer initializer = new BetaDemoAccountInitializer(
                properties,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Beta demo student password must be non-placeholder and at least 12 characters");
    }

    private BetaDemoProperties.DemoAccount account(String password) {
        BetaDemoProperties.DemoAccount account = new BetaDemoProperties.DemoAccount();
        account.setEmail("beta@example.edu.cn");
        account.setUsername("beta-user");
        account.setPassword(password);
        account.setStudentNo("BETA-001");
        account.setPhone("13900000001");
        account.setWechatContact("beta-wechat");
        return account;
    }
}
