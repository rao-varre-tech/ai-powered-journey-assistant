package com.journeyassistant.notification;

import java.time.Duration;
import java.util.UUID;

public interface RetryScheduler {
    void scheduleRetry(UUID journeyId, int retryNumber, Duration delay);

    void scheduleMessageFallback(UUID journeyId, Duration delay);
}
