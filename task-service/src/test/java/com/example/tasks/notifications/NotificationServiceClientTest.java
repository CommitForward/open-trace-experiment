package com.example.tasks.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tasks.api.Task;
import com.example.telemetry.OpenTelemetryFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationServiceClientTest {

    private OpenTelemetrySdk openTelemetry;

    @AfterEach
    void tearDown() {
        if (openTelemetry != null) {
            openTelemetry.close();
        }
    }

    @Test
    void propagatesTraceparentToNotificationService() {
        AtomicReference<HttpRequest> capturedRequest = new AtomicReference<>();
        HttpClient httpClient = new StubHttpClient(capturedRequest);

        openTelemetry = OpenTelemetryFactory.create("task-service", "http://localhost:4318/v1/traces");
        NotificationServiceClient client = new NotificationServiceClient(
            "http://notification-service.local/notifications/send",
            "tasks@example.com",
            "EMAIL",
            new ObjectMapper().findAndRegisterModules(),
            openTelemetry,
            httpClient);

        Tracer tracer = openTelemetry.getTracer("test");
        Span parentSpan = tracer.spanBuilder("incoming-request").startSpan();
        String parentTraceId = parentSpan.getSpanContext().getTraceId();
        try (Scope ignored = parentSpan.makeCurrent()) {
            client.sendTaskCreatedNotification(new Task(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "buy milk",
                "before 6 PM",
                Instant.parse("2026-04-18T11:00:00Z")));
        } finally {
            parentSpan.end();
        }

        HttpRequest request = capturedRequest.get();
        assertNotNull(request);
        String traceparent = request.headers().firstValue("traceparent").orElse(null);
        assertNotNull(traceparent);
        String[] parts = traceparent.split("-");
        assertEquals(4, parts.length);
        assertEquals(parentTraceId, parts[1]);
        assertTrue(request.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    private static final class StubHttpClient extends HttpClient {

        private final AtomicReference<HttpRequest> capturedRequest;

        private StubHttpClient(AtomicReference<HttpRequest> capturedRequest) {
            this.capturedRequest = capturedRequest;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
            capturedRequest.set(request);
            return new StubHttpResponse<>(request, responseBodyHandler);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubHttpResponse<T> implements HttpResponse<T> {

        private final HttpRequest request;
        private final T body;

        private StubHttpResponse(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException {
            this.request = request;
            HttpResponse.ResponseInfo responseInfo = new HttpResponse.ResponseInfo() {
                @Override
                public int statusCode() {
                    return 202;
                }

                @Override
                public HttpHeaders headers() {
                    return HttpHeaders.of(java.util.Map.of("Content-Type", List.of("application/json")), (a, b) -> true);
                }

                @Override
                public HttpClient.Version version() {
                    return HttpClient.Version.HTTP_1_1;
                }
            };

            HttpResponse.BodySubscriber<T> subscriber = responseBodyHandler.apply(responseInfo);
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onNext(List.of(ByteBuffer.wrap("{\"status\":\"RECORDED\"}".getBytes())));
            subscriber.onComplete();
            this.body = subscriber.getBody().toCompletableFuture().join();
        }

        @Override
        public int statusCode() {
            return 202;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of("Content-Type", List.of("application/json")), (a, b) -> true);
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
