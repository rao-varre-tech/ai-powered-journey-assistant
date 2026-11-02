package com.journeyassistant.notification;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalRetrySchedulerTest {
    @Test void schedulesCallbackAtRequestedDelay() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        IvrCallService calls = mock(IvrCallService.class);
        UUID id = UUID.randomUUID();
        LocalRetryScheduler retries = new LocalRetryScheduler(scheduler, calls);

        retries.scheduleRetry(id, 1, Duration.ofMinutes(5));

        var callback = forClass(Runnable.class);
        var scheduledAt = forClass(Instant.class);
        verify(scheduler).schedule(callback.capture(), scheduledAt.capture());
        org.junit.jupiter.api.Assertions.assertTrue(scheduledAt.getValue().isAfter(Instant.now().plus(Duration.ofMinutes(4))));
        callback.getValue().run();
        verify(calls).runScheduledRetry(id);
    }
}
