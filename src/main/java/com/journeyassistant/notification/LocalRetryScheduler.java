package com.journeyassistant.notification;

import org.springframework.scheduling.TaskScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class LocalRetryScheduler implements RetryScheduler {
    private final TaskScheduler scheduler;
    private final IvrCallService calls;

    public LocalRetryScheduler(TaskScheduler scheduler, IvrCallService calls) {
        this.scheduler = scheduler;
        this.calls = calls;
    }

    @Override
    public void scheduleRetry(UUID journeyId, int retryNumber, Duration delay) {
        scheduler.schedule(() -> calls.runScheduledRetry(journeyId), Instant.now().plus(delay));
    }
}
