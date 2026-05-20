package com.campushub.notification;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationNotificationRepository extends JpaRepository<StationNotification, Long> {

    @EntityGraph(attributePaths = "recipient")
    List<StationNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
}
