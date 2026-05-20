package com.campushub.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StationNotificationRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsUnreadNotificationForRecipient() {
        User user = userRepository.findById(1L).orElseThrow();
        notificationService.notify(user, "任务已被接单", "你的任务已有同学接单", "TASK", 1L);

        List<StationNotification> notifications = repository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getReadAt()).isNull();
    }
}
