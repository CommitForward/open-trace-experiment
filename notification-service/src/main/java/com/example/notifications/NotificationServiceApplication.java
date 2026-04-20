package com.example.notifications;

import com.example.notifications.api.NotificationResource;
import com.example.notifications.store.FileNotificationStore;
import com.example.telemetry.OpenTelemetryInstaller;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class NotificationServiceApplication extends Application<NotificationServiceConfiguration> {

    public static void main(String[] args) throws Exception {
        new NotificationServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "notification-service";
    }

    @Override
    public void initialize(Bootstrap<NotificationServiceConfiguration> bootstrap) {
        // No bootstrap customizations required.
    }

    @Override
    public void run(NotificationServiceConfiguration configuration, Environment environment) {
        OpenTelemetrySdk openTelemetry = OpenTelemetryInstaller.install(
            environment,
            configuration.getServiceName(),
            configuration.getOtelCollectorEndpoint(),
            "notification-service-http",
            configuration.getTraceLogPath(),
            configuration.getOtlpBatchLogPath());

        FileNotificationStore notificationStore = new FileNotificationStore(
            configuration.getNotificationLogPath(),
            environment.getObjectMapper());

        environment.jersey().register(new NotificationResource(notificationStore));
    }
}
