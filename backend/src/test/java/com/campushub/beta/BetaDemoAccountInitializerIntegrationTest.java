package com.campushub.beta;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.auth.UserRoleLookup;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "campushub.mail.enabled=false",
        "campushub.beta.demo-reset-enabled=true",
        "campushub.beta.student.email=beta.student@example.edu.cn",
        "campushub.beta.student.username=beta-student",
        "campushub.beta.student.password=BetaStudentPass123!",
        "campushub.beta.student.student-no=BETA-STUDENT-001",
        "campushub.beta.student.real-name=Beta Student",
        "campushub.beta.student.nickname=Beta Student",
        "campushub.beta.student.phone=13900000001",
        "campushub.beta.student.wechat-contact=beta-student-wechat",
        "campushub.beta.admin.email=beta.admin@example.edu.cn",
        "campushub.beta.admin.username=beta-admin",
        "campushub.beta.admin.password=BetaAdminPass123!",
        "campushub.beta.admin.student-no=BETA-ADMIN-001",
        "campushub.beta.admin.real-name=Beta Admin",
        "campushub.beta.admin.nickname=Beta Admin",
        "campushub.beta.admin.phone=13900000002",
        "campushub.beta.admin.wechat-contact=beta-admin-wechat"
})
@ActiveProfiles("test")
class BetaDemoAccountInitializerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleLookup userRoleLookup;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDemoStudentAndAdminWithRolesAndWallets() {
        User student = userRepository.findByEmail("beta.student@example.edu.cn").orElseThrow();
        User admin = userRepository.findByEmail("beta.admin@example.edu.cn").orElseThrow();

        assertThat(student.getUsername()).isEqualTo("beta-student");
        assertThat(student.getWechatContact()).isEqualTo("beta-student-wechat");
        assertThat(passwordEncoder.matches("BetaStudentPass123!", student.getPasswordHash())).isTrue();
        assertThat(userRoleLookup.findRoleCodes(student.getId())).contains("ROLE_STUDENT");
        assertThat(walletAccountRepository.findByUserId(student.getId())).isPresent();

        assertThat(admin.getUsername()).isEqualTo("beta-admin");
        assertThat(admin.getWechatContact()).isEqualTo("beta-admin-wechat");
        assertThat(passwordEncoder.matches("BetaAdminPass123!", admin.getPasswordHash())).isTrue();
        assertThat(userRoleLookup.findRoleCodes(admin.getId())).contains("ROLE_STUDENT", "ROLE_ADMIN");
        assertThat(walletAccountRepository.findByUserId(admin.getId())).isPresent();
    }
}
