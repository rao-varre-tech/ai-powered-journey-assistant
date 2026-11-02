package com.journeyassistant.notification;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.CreateTaskRequest;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.OidcToken;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.cloud.tasks.v2.TaskName;
import com.google.protobuf.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class CloudTasksRetryScheduler implements RetryScheduler {
    private final CloudTasksClient client;
    private final String projectId;
    private final String location;
    private final String queue;
    private final String invokerServiceAccount;
    private final String publicBaseUrl;

    public CloudTasksRetryScheduler(CloudTasksClient client, String projectId, String location, String queue,
                                    String invokerServiceAccount, String publicBaseUrl) {
        this.client = client;
        this.projectId = projectId;
        this.location = location;
        this.queue = queue;
        this.invokerServiceAccount = invokerServiceAccount;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @Override
    public void scheduleRetry(UUID journeyId, int retryNumber, Duration delay) {
        Instant scheduledAt = Instant.now().plus(delay);
        String taskName = TaskName.of(projectId, location, queue, "journey-" + journeyId + "-retry-" + retryNumber).toString();
        String audience = publicBaseUrl;
        HttpRequest request = HttpRequest.newBuilder()
                .setHttpMethod(HttpMethod.POST)
                .setUrl(publicBaseUrl + "/internal/tasks/journeys/" + journeyId + "/retry")
                .setOidcToken(OidcToken.newBuilder().setServiceAccountEmail(invokerServiceAccount).setAudience(audience).build())
                .build();
        Task task = Task.newBuilder().setName(taskName).setHttpRequest(request)
                .setScheduleTime(Timestamp.newBuilder().setSeconds(scheduledAt.getEpochSecond()).setNanos(scheduledAt.getNano()))
                .build();
        client.createTask(CreateTaskRequest.newBuilder().setParent(QueueName.of(projectId, location, queue).toString())
                .setTask(task).build());
    }
}
