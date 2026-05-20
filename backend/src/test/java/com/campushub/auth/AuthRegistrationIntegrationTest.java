package com.campushub.auth;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "campushub.mail.enabled=false",
        "campushub.mail.code.ttl-minutes=10",
        "campushub.mail.code.resend-seconds=60"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void sendCodeAcceptsEduCnEmailAndStoresOnlyHashedCode() throws Exception {
        mockMvc.perform(post("/api/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  New.Student@MAIL.USTC.EDU.CN  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        EmailVerificationCode code = emailVerificationCodeRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc("new.student@mail.ustc.edu.cn", "REGISTER")
                .orElseThrow();

        assertThat(code.getCodeHash()).startsWith("$2");
        assertThat(code.getCodeHash()).doesNotContain("New.Student");
        assertThat(code.getUsedAt()).isNull();
        assertThat(code.getAttemptCount()).isZero();
    }

    @Test
    void sendCodeRejectsNonEduCnEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("edu.cn")));
    }

    @Test
    void registerRequiresAtLeastOneCampusContact() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"contact.required@ustc.edu.cn\",\"password\":\"Passw0rd!2026\",\"emailCode\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请至少填写微信或 QQ 联系方式"));
    }

    @Test
    void registerCreatesActiveStudentUserWithWalletAfterCodeVerified() throws Exception {
        mockMvc.perform(post("/api/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"register.ok@ustc.edu.cn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        EmailVerificationCode code = emailVerificationCodeRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc("register.ok@ustc.edu.cn", "REGISTER")
                .orElseThrow();
        code.setCodeHash("$2a$10$6TwUK6iN3GnXUwc7bpHxNuAsTwPFHd9h1lV9s0GlR1.3o9C706/kK");
        emailVerificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"REGISTER.OK@USTC.EDU.CN\",\"password\":\"Passw0rd!2026\",\"emailCode\":\"123456\",\"wechatContact\":\"  campus-wechat  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("register.ok@ustc.edu.cn"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.creditScore").value(100));

        User user = userRepository.findByEmail("register.ok@ustc.edu.cn").orElseThrow();
        assertThat(user.getWechatContact()).isEqualTo("campus-wechat");
        EmailVerificationCode usedCode = emailVerificationCodeRepository.findById(code.getId()).orElseThrow();
        assertThat(usedCode.getUsedAt()).isNotNull();
    }
}
