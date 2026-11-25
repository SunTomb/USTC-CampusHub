package com.campushub.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campushub.auth.JwtTokenService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StationNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void recipientCanMarkReadAndRepeatedMarkKeepsOriginalTimestamp() throws Exception {
        Long notificationId = createNotificationFor(1L);
        String token = jwtTokenService.issueToken(1L, "alice");

        mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readAt").exists());

        LocalDateTime firstReadAt = notificationRepository.findById(notificationId).orElseThrow().getReadAt();
        assertThat(firstReadAt).isNotNull();

        mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").value(org.hamcrest.Matchers.startsWith(firstReadAt.toString().substring(0, 26))));

        assertThat(notificationRepository.findById(notificationId).orElseThrow().getReadAt())
                .isEqualTo(firstReadAt);
    }

    @Test
    void nonRecipientCannotMarkNotificationRead() throws Exception {
        Long notificationId = createNotificationFor(1L);
        String token = jwtTokenService.issueToken(2L, "bob");

        mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(notificationRepository.findById(notificationId).orElseThrow().getReadAt()).isNull();
    }

    @Test
    void administratorCannotMarkAnotherUsersNotificationRead() throws Exception {
        Long notificationId = createNotificationFor(1L);
        String token = jwtTokenService.issueToken(4L, "admin");

        mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(notificationRepository.findById(notificationId).orElseThrow().getReadAt()).isNull();
    }

    private Long createNotificationFor(Long recipientId) {
        User recipient = userRepository.findById(recipientId).orElseThrow();
        return notificationService.notify(recipient, "测试通知", "测试内容", "TASK", 1L).id();
    }
}
