package com.campushub.notification;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final StationNotificationRepository stationNotificationRepository;

    public NotificationService(StationNotificationRepository stationNotificationRepository) {
        this.stationNotificationRepository = stationNotificationRepository;
    }

    @Transactional
    public StationNotificationSummary notify(User recipient, String title, String content, String targetType, Long targetId) {
        StationNotification notification = new StationNotification(recipient, title, content, targetType, targetId);
        return StationNotificationSummary.from(stationNotificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<StationNotificationSummary> listForUser(Long userId) {
        return stationNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(StationNotificationSummary::from)
                .toList();
    }

    @Transactional
    public StationNotificationSummary markRead(Long notificationId) {
        StationNotification notification = stationNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));
        notification.markRead();
        return StationNotificationSummary.from(notification);
    }
}
