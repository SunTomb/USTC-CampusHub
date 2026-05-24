package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.CampusHubApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

class BetaDemoAccountInitializerValidationTest {

    @Test
    void rejectsPlaceholderPasswordsWhenResetIsEnabled() {
        SpringApplication application = new SpringApplication(CampusHubApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.ofEntries(
                Map.entry("spring.profiles.active", "test"),
                Map.entry("campushub.mail.enabled", "false"),
                Map.entry("campushub.beta.demo-reset-enabled", "true"),
                Map.entry("campushub.beta.student.email", "beta.student@example.edu.cn"),
                Map.entry("campushub.beta.student.username", "beta-student"),
                Map.entry("campushub.beta.student.password", "change-me"),
                Map.entry("campushub.beta.student.student-no", "BETA-STUDENT-001"),
                Map.entry("campushub.beta.student.phone", "13900000001"),
                Map.entry("campushub.beta.admin.email", "beta.admin@example.edu.cn"),
                Map.entry("campushub.beta.admin.username", "beta-admin"),
                Map.entry("campushub.beta.admin.password", "BetaAdminPass123!"),
                Map.entry("campushub.beta.admin.student-no", "BETA-ADMIN-001"),
                Map.entry("campushub.beta.admin.phone", "13900000002")));

        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = application.run()) {
                throw new AssertionError("context should not start");
            }
        }).satisfies(error -> assertThat(stackMessages(error))
                .contains("Beta demo student password must be non-placeholder and at least 12 characters"));
    }

    private String stackMessages(Throwable error) {
        StringBuilder messages = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            messages.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return messages.toString();
    }
}
