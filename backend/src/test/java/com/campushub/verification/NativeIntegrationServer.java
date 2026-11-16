package com.campushub.verification;

import com.campushub.CampusHubApplication;
import com.campushub.auth.UserRoleService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Test-classpath-only entry point; never included in the production jar. */
public final class NativeIntegrationServer {
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:native-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("campushub.jwt.secret", UUID.randomUUID().toString() + UUID.randomUUID());
        System.setProperty("campushub.jwt.issuer", "native-isolated-integration");
        System.setProperty("campushub.mail.enabled", "false");
        System.setProperty("campushub.financial.enabled", "false");
        System.setProperty("campushub.beta.demo-reset-enabled", "false");
        System.setProperty("logging.level.org.springframework.boot.autoconfigure.security", "ERROR");
        var context = SpringApplication.run(CampusHubApplication.class, args);
        var users = context.getBean(UserRepository.class);
        var encoder = context.getBean(PasswordEncoder.class);
        var roles = context.getBean(UserRoleService.class);
        for (String name : new String[]{"publisher", "runner", "stranger"}) {
            User user = new User("native-" + name, "native_" + name, encoder.encode("Native-Only-2026!"),
                    "合成测试用户", "联调" + name, "native-" + name, name + "@native.example.edu.cn", "ACTIVE");
            user.updateContact("native_" + name, null);
            users.save(user);
            roles.assignRole(user.getId(), "ROLE_STUDENT");
        }
        System.out.println("NATIVE_ISOLATED_SERVER_READY: synthetic accounts; mail/finance/reset disabled");
    }
}
