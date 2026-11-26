package com.campushub.notification;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationNotificationRepository extends JpaRepository<StationNotification, Long> {

    @EntityGraph(attributePaths = "recipient")
    List<StationNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StationNotification n set n.readAt = :readAt where n.recipient.id = :recipientId and n.readAt is null")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);
}
