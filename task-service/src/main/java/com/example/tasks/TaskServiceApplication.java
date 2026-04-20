package com.example.tasks;

import com.example.tasks.api.TaskResource;
import com.example.tasks.notifications.NotificationServiceClient;
import com.example.tasks.store.InMemoryTaskStore;
import com.example.telemetry.OpenTelemetryInstaller;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TaskServiceApplication extends Application<TaskServiceConfiguration> {

    public static void main(String[] args) throws Exception {
        new TaskServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "task-service";
    }

    @Override
    public void initialize(Bootstrap<TaskServiceConfiguration> bootstrap) {
        // No bootstrap customizations required for this sample.
    }

    @Override
    public void run(TaskServiceConfiguration configuration, Environment environment) {
        OpenTelemetrySdk openTelemetry = OpenTelemetryInstaller.install(
            environment,
            configuration.getServiceName(),
            configuration.getOtelCollectorEndpoint(),
            "task-service-http",
            configuration.getTraceLogPath(),
            configuration.getOtlpBatchLogPath());

        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        NotificationServiceClient notificationServiceClient = new NotificationServiceClient(
            configuration.getNotificationServiceEndpoint(),
            configuration.getNotificationRecipient(),
            configuration.getNotificationChannel(),
            environment.getObjectMapper(),
            openTelemetry);
        environment.jersey().register(new TaskResource(taskStore, notificationServiceClient));
    }
}
