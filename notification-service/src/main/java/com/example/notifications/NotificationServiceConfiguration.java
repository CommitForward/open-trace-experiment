package com.example.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import javax.validation.constraints.NotBlank;

public class NotificationServiceConfiguration extends Configuration {

    @NotBlank
    private String serviceName = "notification-service";

    @NotBlank
    private String otelCollectorEndpoint = "http://localhost:4318/v1/traces";

    @NotBlank
    private String notificationLogPath = "./data/notifications.log";

    @NotBlank
    private String traceLogPath = "./data/notification-traces.log";

    @NotBlank
    private String otlpBatchLogPath = "./data/notification-otlp-batches.log";

    @JsonProperty
    public String getServiceName() {
        return serviceName;
    }

    @JsonProperty
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @JsonProperty
    public String getOtelCollectorEndpoint() {
        return otelCollectorEndpoint;
    }

    @JsonProperty
    public void setOtelCollectorEndpoint(String otelCollectorEndpoint) {
        this.otelCollectorEndpoint = otelCollectorEndpoint;
    }

    @JsonProperty
    public String getNotificationLogPath() {
        return notificationLogPath;
    }

    @JsonProperty
    public void setNotificationLogPath(String notificationLogPath) {
        this.notificationLogPath = notificationLogPath;
    }

    @JsonProperty
    public String getTraceLogPath() {
        return traceLogPath;
    }

    @JsonProperty
    public void setTraceLogPath(String traceLogPath) {
        this.traceLogPath = traceLogPath;
    }

    @JsonProperty
    public String getOtlpBatchLogPath() {
        return otlpBatchLogPath;
    }

    @JsonProperty
    public void setOtlpBatchLogPath(String otlpBatchLogPath) {
        this.otlpBatchLogPath = otlpBatchLogPath;
    }
}
