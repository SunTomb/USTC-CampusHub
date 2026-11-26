package com.campushub.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void markAllReadUpdatesOnlyUnreadRowsForRecipient() {
        User recipient = userRepository.findById(1L).orElseThrow();
        User other = userRepository.findById(2L).orElseThrow();
        Long first = notificationService.notify(recipient, "First", "Body", "TASK", 1L).id();
        Long second = notificationService.notify(recipient, "Second", "Body", "TASK", 2L).id();
        Long alreadyRead = notificationService.notify(recipient, "Older", "Body", "TASK", 3L).id();
        Long untouched = notificationService.notify(other, "Other", "Body", "TASK", 4L).id();
        notificationService.markRead(alreadyRead, recipient.getId());
        entityManager.flush();
        entityManager.clear();
        LocalDateTime original = readAt(alreadyRead);
        assertThat(original).isNotNull();

        assertThat(notificationService.markAllRead(recipient.getId())).isEqualTo(2);
        LocalDateTime bulkReadAt = readAt(first);
        assertThat(bulkReadAt).isNotNull();
        assertThat(readAt(second)).isEqualTo(bulkReadAt);
        assertThat(notificationService.markAllRead(recipient.getId())).isZero();
        assertThat(readAt(first)).isEqualTo(bulkReadAt);
        assertThat(readAt(second)).isEqualTo(bulkReadAt);
        assertThat(readAt(alreadyRead)).isEqualTo(original);
        assertThat(readAt(untouched)).isNull();
    }

    private LocalDateTime readAt(Long id) {
        return repository.findById(id).orElseThrow().getReadAt();
    }

    @Test
    void createsUnreadNotificationForRecipient() {
        User user = userRepository.findById(1L).orElseThrow();
        notificationService.notify(user, "任务已被接单", "你的任务已有同学接单", "TASK", 1L);

        List<StationNotification> notifications = repository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getReadAt()).isNull();
    }
}
