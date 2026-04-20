package com.example.tasks.notifications;

import com.example.tasks.api.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class NotificationServiceClient {

    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> URL_FULL = AttributeKey.stringKey("url.full");
    private static final AttributeKey<Long> HTTP_STATUS_CODE = AttributeKey.longKey("http.response.status_code");

    private final URI notificationEndpoint;
    private final String notificationRecipient;
    private final String notificationChannel;
    private final ObjectMapper objectMapper;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private final HttpClient httpClient;

    public NotificationServiceClient(
        String notificationServiceEndpoint,
        String notificationRecipient,
        String notificationChannel,
        ObjectMapper objectMapper,
        OpenTelemetry openTelemetry
    ) {
        this(notificationServiceEndpoint, notificationRecipient, notificationChannel, objectMapper, openTelemetry, HttpClient.newHttpClient());
    }

    NotificationServiceClient(
        String notificationServiceEndpoint,
        String notificationRecipient,
        String notificationChannel,
        ObjectMapper objectMapper,
        OpenTelemetry openTelemetry,
        HttpClient httpClient
    ) {
        this.notificationEndpoint = URI.create(notificationServiceEndpoint);
        this.notificationRecipient = notificationRecipient;
        this.notificationChannel = notificationChannel;
        this.objectMapper = objectMapper;
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("task-service-notification-client");
        this.httpClient = httpClient;
    }

    public void sendTaskCreatedNotification(Task task) {
        Span span = tracer.spanBuilder("POST " + notificationEndpoint.getPath())
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute(HTTP_METHOD, "POST");
            span.setAttribute(URL_FULL, notificationEndpoint.toString());
            span.addEvent("notification.request.started");

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json");
            openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), headers, MapHeaderSetter.INSTANCE);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(notificationEndpoint)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(task)));

            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            span.setAttribute(HTTP_STATUS_CODE, (long) response.statusCode());
            span.addEvent("notification.response.received");

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                span.setStatus(StatusCode.ERROR);
                throw new WebApplicationException(
                    "Notification service responded with status " + response.statusCode(),
                    Response.Status.BAD_GATEWAY);
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            throw new WebApplicationException("Notification service call failed", exception, Response.Status.BAD_GATEWAY);
        } catch (RuntimeException exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR);
            throw exception;
        } finally {
            span.end();
        }
    }

    private String buildPayload(Task task) throws IOException {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("recipient", notificationRecipient);
        payload.put("channel", notificationChannel);
        payload.put("message", "Task created: " + task.getTitle() + " (" + task.getId() + ")");
        return objectMapper.writeValueAsString(payload);
    }

    private enum MapHeaderSetter implements TextMapSetter<Map<String, String>> {
        INSTANCE;

        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }
    }
}
