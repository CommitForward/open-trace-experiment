package com.example.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(Priorities.USER)
public class TelemetryRequestListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRequestListener.class);
    private static final AttributeKey<Long> HTTP_STATUS_CODE = AttributeKey.longKey("http.response.status_code");
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public TelemetryRequestListener(OpenTelemetry openTelemetry, String instrumentationName) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(instrumentationName);
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        // No application lifecycle handling needed.
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new SpanRequestEventListener(openTelemetry, tracer, requestEvent);
    }

    private static class SpanRequestEventListener implements RequestEventListener {

        private final Span span;
        private final Scope scope;

        private SpanRequestEventListener(OpenTelemetry openTelemetry, Tracer tracer, RequestEvent requestEvent) {
            ContainerRequestContext request = requestEvent.getContainerRequest();
            Context parentContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, RequestHeaderGetter.INSTANCE);

            String method = request.getMethod();
            String path = "/" + request.getUriInfo().getPath(false);
            String spanName = method + " " + path;

            this.span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
            this.span.setAttribute(HTTP_METHOD, method);
            this.span.setAttribute(URL_PATH, path);
            this.span.addEvent("request.received");
            this.scope = this.span.makeCurrent();
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getException() != null) {
                span.recordException(event.getException());
                span.setStatus(StatusCode.ERROR);
            }

            if (event.getType() == RequestEvent.Type.FINISHED) {
                if (event.getContainerResponse() != null) {
                    int status = event.getContainerResponse().getStatus();
                    span.setAttribute(HTTP_STATUS_CODE, (long) status);
                    span.addEvent("response.sent");
                    if (status >= 500) {
                        span.setStatus(StatusCode.ERROR);
                    }
                }
                scope.close();
                span.end();
                LOGGER.info(
                    "Ended server span traceId={} spanId={}",
                    span.getSpanContext().getTraceId(),
                    span.getSpanContext().getSpanId());
            }
        }
    }

    private enum RequestHeaderGetter implements TextMapGetter<ContainerRequestContext> {
        INSTANCE;

        @Override
        public Iterable<String> keys(ContainerRequestContext carrier) {
            return carrier.getHeaders().keySet();
        }

        @Override
        public String get(ContainerRequestContext carrier, String key) {
            List<String> values = carrier.getHeaders().get(key);
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(0);
        }
    }
}
