package com.campushub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "campushub.mail.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private com.campushub.user.UserRepository users;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Test
    void allowsAnonymousPublicReadAndRejectsAnonymousWrite() throws Exception {
        mockMvc.perform(get("/api/goods"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/goods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAcceptsEmailAsIdentifier() throws Exception {
        var fixture = users.findById(1L).orElseThrow();
        fixture.resetPasswordHash(encoder.encode("password123"));
        users.saveAndFlush(fixture);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice@campus.example\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    @Test
    void requiresAdminRoleForAdminApis() throws Exception {
        String studentToken = jwtTokenService.issueToken(1L, "alice");
        String adminToken = jwtTokenService.issueToken(4L, "admin");

        mockMvc.perform(get("/api/admin/wallet/recharges")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/wallet/recharges")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
