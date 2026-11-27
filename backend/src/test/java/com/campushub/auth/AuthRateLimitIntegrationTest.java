package com.campushub.auth;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "campushub.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "campushub.auth.rate-limit.enabled=true")
@Transactional
class AuthRateLimitIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void sixthSendCodeRequestIsRejected() throws Exception {
        for (int i = 0; i < 5; i++) {
            sendCode("192.0.2.1", "", i).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
        sendCode("192.0.2.1", "", 5).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("验证码请求过于频繁，请稍后再试"));
    }

    @Test
    void publicPeerCannotChangeItsBucketWithForwardedHeaders() throws Exception {
        for (int i = 0; i < 5; i++) {
            sendCode("192.0.2.2", "198.51.100." + i, i).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
        sendCode("192.0.2.2", "198.51.100.5", 5).andExpect(status().isTooManyRequests());
    }

    @Test
    void lockedAccountRejectsCorrectPasswordEvenFromANewPeer() throws Exception {
        createUser("limit_lock");
        for (int i = 0; i < 5; i++) {
            wrongPassword("192.0.2.3", i == 0 ? "  LIMIT_LOCK " : "limit_lock");
        }
        login("192.0.2.4", "limit_lock", "fixture-password").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("登录尝试过于频繁，请 15 分钟后再试"));
    }

    @Test
    void successClearsAccountFailures() throws Exception {
        createUser("limit_clear");
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 4; i++) wrongPassword("192.0.2.5", "limit_clear");
            login("192.0.2.5", "limit_clear", "fixture-password").andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    void successfulLoginDoesNotClearIpFailuresAcrossAccounts() throws Exception {
        createUser("limit_ip");
        for (int i = 0; i < 19; i++) wrongPassword("192.0.2.6", "limit_missing_" + i);
        login("192.0.2.6", "limit_ip", "fixture-password").andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        wrongPassword("192.0.2.6", "limit_missing_last");
        login("192.0.2.6", "limit_ip", "fixture-password").andExpect(status().isTooManyRequests());
    }

    private ResultActions sendCode(String peer, String forwarded, int n) throws Exception {
        return mvc.perform(post("/api/auth/register/send-code").with(r -> { r.setRemoteAddr(peer); return r; })
                .header("X-Forwarded-For", forwarded).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rate" + n + "@example.edu.cn\"}"));
    }

    private void createUser(String name) {
        users.save(new User(name, name, encoder.encode("fixture-password"), name, name, name,
                name + "@example.edu.cn", "ACTIVE"));
    }

    private void wrongPassword(String peer, String name) throws Exception {
        login(peer, name, "wrong").andExpect(status().isOk()).andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    private ResultActions login(String peer, String name, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr(peer); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"password\":\"" + password + "\"}"));
    }
}
