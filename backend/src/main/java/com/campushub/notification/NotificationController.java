package com.campushub.notification;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users/{userId}/notifications")
    public ApiResponse<List<StationNotificationSummary>> listNotifications(@PathVariable Long userId) {
        return ApiResponse.ok(notificationService.listForUser(currentUserService.requireSameUser(userId)));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<StationNotificationSummary> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }
}
