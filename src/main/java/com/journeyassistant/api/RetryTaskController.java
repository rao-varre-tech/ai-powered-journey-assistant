package com.journeyassistant.api;

import com.journeyassistant.notification.IvrCallService;
import com.journeyassistant.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/internal/tasks/journeys")
public class RetryTaskController {
    private final IvrCallService calls;
    private final NotificationService notifications;

    public RetryTaskController(IvrCallService calls, NotificationService notifications) {
        this.calls = calls;
        this.notifications = notifications;
    }

    @PostMapping("/{journeyId}/retry")
    public ResponseEntity<Void> runRetry(@PathVariable UUID journeyId) {
        calls.runScheduledRetry(journeyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{journeyId}/message-fallback")
    public ResponseEntity<Void> runMessageFallback(@PathVariable UUID journeyId) {
        notifications.runMessageFallback(journeyId);
        return ResponseEntity.noContent().build();
    }
}
