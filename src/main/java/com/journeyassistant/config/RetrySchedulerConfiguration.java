package com.journeyassistant.config;

import com.journeyassistant.notification.IvrCallService;
import com.journeyassistant.notification.LocalRetryScheduler;
import com.journeyassistant.notification.RetryScheduler;
import com.journeyassistant.notification.CloudTasksRetryScheduler;
import com.google.cloud.tasks.v2.CloudTasksClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class RetrySchedulerConfiguration {
    @Bean
    @Profile("local")
    ThreadPoolTaskScheduler localTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("journey-retry-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }

    @Bean(destroyMethod = "close")
    @Profile("gcp")
    CloudTasksClient cloudTasksClient() throws Exception { return CloudTasksClient.create(); }

    @Bean
    @Profile("local")
    RetryScheduler localRetryScheduler(TaskScheduler taskScheduler, IvrCallService calls) {
        return new LocalRetryScheduler(taskScheduler, calls);
    }

    @Bean
    @Profile("gcp")
    RetryScheduler cloudTasksRetryScheduler(CloudTasksClient client,
            @Value("${gcp.tasks.project-id}") String projectId,
            @Value("${gcp.tasks.location}") String location,
            @Value("${gcp.tasks.queue}") String queue,
            @Value("${gcp.tasks.invoker-service-account}") String invokerServiceAccount,
            @Value("${twilio.public-base-url}") String publicBaseUrl) {
        if (projectId.isBlank() || location.isBlank() || queue.isBlank() || invokerServiceAccount.isBlank() || publicBaseUrl.isBlank())
            throw new IllegalStateException("GCP Cloud Tasks configuration is incomplete");
        return new CloudTasksRetryScheduler(client, projectId, location, queue, invokerServiceAccount, publicBaseUrl);
    }
}
