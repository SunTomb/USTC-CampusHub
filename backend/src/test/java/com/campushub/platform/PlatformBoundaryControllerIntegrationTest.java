package com.campushub.platform;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlatformBoundaryControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @Autowired UserRoleService roles;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publicConfigAndProtocolsNeverExposeSensitiveConfiguration() throws Exception {
        mvc.perform(get("/api/platform/features"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.financialEnabled").value(false))
                .andExpect(content().string(not(containsString("secret"))));
        mvc.perform(get("/api/platform/protocols/RUNNER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value("v1.0"))
                .andExpect(jsonPath("$.data.acceptedAt").isEmpty());
    }

    @Test
    void anonymousViewerIdCannotRevealContacts() throws Exception {
        jdbc.update("UPDATE users SET wechat_contact = ? WHERE id IN (1,3)", "private-contact-proof");
        mvc.perform(get("/api/goods/1?viewerId=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contactVisible").value(false))
                .andExpect(content().string(not(containsString("private-contact-proof"))));
        mvc.perform(get("/api/shops/1?viewerId=3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contactVisible").value(false));
        jdbc.update("UPDATE project_ads SET contact_visibility = 'HIDDEN', status = 'APPROVED', expires_at = NULL WHERE id = 1");
        mvc.perform(get("/api/project-ads/1?viewerId=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contactVisible").value(false));
    }

    @Test
    void privateCollectionsRequireTheCorrectAdministrator() throws Exception {
        String student = "Bearer " + tokens.issueToken(1L, "alice");
        for (String path : new String[]{"/api/users", "/api/users/2", "/api/wallet/accounts", "/api/task-applications", "/api/service-orders", "/api/moderation/reports", "/api/moderation/safety-logs"}) {
            mvc.perform(get(path).header("Authorization", student)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/task-applications")).andExpect(status().isUnauthorized());
        roles.assignRole(1L, "ROLE_TRADE_ADMIN");
        mvc.perform(get("/api/admin/wallet/recharges").header("Authorization", student)).andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAcceptAnAgreementOnBehalfOfAnotherUser() throws Exception {
        mvc.perform(post("/api/identity/users/1/roles")
                .header("Authorization", "Bearer " + tokens.issueToken(4L, "admin"))
                .contentType("application/json")
                .content("{\"roleType\":\"RUNNER\",\"agreementVersion\":\"v1.0\",\"agreementAccepted\":true}"))
                .andExpect(status().isForbidden());
    }
}
