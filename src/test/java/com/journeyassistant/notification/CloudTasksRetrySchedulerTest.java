package com.journeyassistant.notification;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.CreateTaskRequest;
import com.google.cloud.tasks.v2.Task;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudTasksRetrySchedulerTest {
    @Test void createsDelayedOidcHttpTaskWithoutNetworkCall() {
        CloudTasksClient client = mock(CloudTasksClient.class);
        when(client.createTask(any(CreateTaskRequest.class))).thenReturn(Task.newBuilder().build());
        UUID id = UUID.randomUUID();
        CloudTasksRetryScheduler scheduler = new CloudTasksRetryScheduler(client, "project", "region", "queue",
                "retry-invoker@example.iam.gserviceaccount.com", "https://journeys.example");

        scheduler.scheduleRetry(id, 2, Duration.ofMinutes(5));

        var request = org.mockito.ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(client).createTask(request.capture());
        var task = request.getValue().getTask();
        assertTrue(task.getName().endsWith("journey-" + id + "-retry-2"));
        assertTrue(task.getHttpRequest().getUrl().equals("https://journeys.example/internal/tasks/journeys/" + id + "/retry"));
        assertTrue(task.getHttpRequest().getOidcToken().getServiceAccountEmail().equals("retry-invoker@example.iam.gserviceaccount.com"));
        assertTrue(task.getScheduleTime().getSeconds() > 0);
    }
}
