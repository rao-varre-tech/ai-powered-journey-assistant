package com.journeyassistant.notification;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.beans.factory.ObjectProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class LocalRetryScheduler implements RetryScheduler {
    private final TaskScheduler scheduler;
    private final IvrCallService calls;
    private final ObjectProvider<NotificationService> notifications;

    public LocalRetryScheduler(TaskScheduler scheduler, IvrCallService calls, ObjectProvider<NotificationService> notifications) {
        this.scheduler = scheduler;
        this.calls = calls;
        this.notifications = notifications;
    }

    @Override
    public void scheduleRetry(UUID journeyId, int retryNumber, Duration delay) {
        scheduler.schedule(() -> calls.runScheduledRetry(journeyId), Instant.now().plus(delay));
    }

    @Override
    public void scheduleMessageFallback(UUID journeyId, Duration delay) {
        scheduler.schedule(() -> notifications.getObject().runMessageFallback(journeyId), Instant.now().plus(delay));
    }
}
