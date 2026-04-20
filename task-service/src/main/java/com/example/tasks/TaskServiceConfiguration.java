package com.example.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import javax.validation.constraints.NotBlank;

public class TaskServiceConfiguration extends Configuration {

    @NotBlank
    private String serviceName = "task-service";

    @NotBlank
    private String otelCollectorEndpoint = "http://localhost:4318/v1/traces";

    @NotBlank
    private String notificationServiceEndpoint = "http://localhost:8082/notifications/send";

    @NotBlank
    private String notificationRecipient = "tasks@example.com";

    @NotBlank
    private String notificationChannel = "EMAIL";

    @NotBlank
    private String traceLogPath = "./data/task-traces.log";

    @NotBlank
    private String otlpBatchLogPath = "./data/task-otlp-batches.log";

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
    public String getNotificationServiceEndpoint() {
        return notificationServiceEndpoint;
    }

    @JsonProperty
    public void setNotificationServiceEndpoint(String notificationServiceEndpoint) {
        this.notificationServiceEndpoint = notificationServiceEndpoint;
    }

    @JsonProperty
    public String getNotificationRecipient() {
        return notificationRecipient;
    }

    @JsonProperty
    public void setNotificationRecipient(String notificationRecipient) {
        this.notificationRecipient = notificationRecipient;
    }

    @JsonProperty
    public String getNotificationChannel() {
        return notificationChannel;
    }

    @JsonProperty
    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
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
