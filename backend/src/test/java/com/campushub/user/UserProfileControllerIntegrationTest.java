package com.campushub.user;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleLookup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "campushub.mail.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class UserProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRoleLookup userRoleLookup;

    @Test
    void currentUserCanUpdateUsernameAndNickname() throws Exception {
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice_profile\",\"nickname\":\"Alice Profile\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice_profile"))
                .andExpect(jsonPath("$.data.nickname").value("Alice Profile"));

        User user = userRepository.findById(1L).orElseThrow();
        assertThat(user.getUsername()).isEqualTo("alice_profile");
        assertThat(user.getNickname()).isEqualTo("Alice Profile");
    }

    @Test
    void currentUserCanUpdateOwnContactsAndPreserveIdentityAndRoles() throws Exception {
        User before = userRepository.findById(1L).orElseThrow();
        User otherBefore = userRepository.findById(2L).orElseThrow();
        String passwordHash = before.getPasswordHash();
        List<String> rolesBefore = userRoleLookup.findRoleCodes(before.getId());
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(put("/api/users/me/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wechatContact\":\"  alice-native  \",\"qqContact\":\" 12345678 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(before.getUsername()))
                .andExpect(jsonPath("$.data.email").value(before.getEmail()))
                .andExpect(jsonPath("$.data.wechatContact").value("alice-native"))
                .andExpect(jsonPath("$.data.qqContact").value("12345678"));

        User after = userRepository.findById(1L).orElseThrow();
        User otherAfter = userRepository.findById(2L).orElseThrow();
        assertThat(after.getWechatContact()).isEqualTo("alice-native");
        assertThat(after.getQqContact()).isEqualTo("12345678");
        assertThat(after.getUsername()).isEqualTo(before.getUsername());
        assertThat(after.getEmail()).isEqualTo(before.getEmail());
        assertThat(after.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(after.getRealName()).isEqualTo(before.getRealName());
        assertThat(after.getNickname()).isEqualTo(before.getNickname());
        assertThat(after.getPhone()).isEqualTo(before.getPhone());
        assertThat(after.getStatus()).isEqualTo(before.getStatus());
        assertThat(after.getCreditScore()).isEqualTo(before.getCreditScore());
        assertThat(userRoleLookup.findRoleCodes(after.getId())).containsExactlyInAnyOrderElementsOf(rolesBefore);
        assertThat(otherAfter.getWechatContact()).isEqualTo(otherBefore.getWechatContact());
        assertThat(otherAfter.getQqContact()).isEqualTo(otherBefore.getQqContact());
    }

    @Test
    void invalidContactUpdateDoesNotMutateExistingContacts() throws Exception {
        User before = userRepository.findById(1L).orElseThrow();
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(put("/api/users/me/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wechatContact\":\"   \",\"qqContact\":\"  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        User after = userRepository.findById(1L).orElseThrow();
        assertThat(after.getWechatContact()).isEqualTo(before.getWechatContact());
        assertThat(after.getQqContact()).isEqualTo(before.getQqContact());
    }

    @Test
    void contactUpdateRejectsValuesBeyondDatabaseLengthsWithoutMutation() throws Exception {
        User before = userRepository.findById(1L).orElseThrow();
        String token = jwtTokenService.issueToken(1L, "alice");
        String tooLongWechat = "w".repeat(121);

        mockMvc.perform(put("/api/users/me/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wechatContact\":\"" + tooLongWechat + "\",\"qqContact\":\"valid-qq\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        User after = userRepository.findById(1L).orElseThrow();
        assertThat(after.getWechatContact()).isEqualTo(before.getWechatContact());
        assertThat(after.getQqContact()).isEqualTo(before.getQqContact());
    }

    @Test
    void currentUserCanChangePasswordWithCurrentPassword() throws Exception {
        User fixture = userRepository.findById(1L).orElseThrow();
        fixture.resetPasswordHash(passwordEncoder.encode("password123"));
        userRepository.saveAndFlush(fixture);
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123\",\"newPassword\":\"NewPassw0rd!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findById(1L).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassw0rd!2026", user.getPasswordHash())).isTrue();
    }

    @Test
    void currentUserPasswordChangeRejectsWrongCurrentPassword() throws Exception {
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"NewPassw0rd!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
