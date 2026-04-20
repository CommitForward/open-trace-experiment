package com.example.telemetry;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSpanExporter implements SpanExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSpanExporter.class);

    private final SpanExporter delegate;
    private final String serviceName;
    private final String collectorEndpoint;

    public LoggingSpanExporter(SpanExporter delegate, String serviceName, String collectorEndpoint) {
        this.delegate = delegate;
        this.serviceName = serviceName;
        this.collectorEndpoint = collectorEndpoint;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        String spanSummary = spans.stream()
            .map(span -> span.getName() + "[traceId=" + span.getTraceId() + ", spanId=" + span.getSpanId() + "]")
            .collect(Collectors.joining(", "));

        LOGGER.info(
            "Emitting {} span(s) for service '{}' to collector {}: {}",
            spans.size(),
            serviceName,
            collectorEndpoint,
            spanSummary);

        CompletableResultCode result = delegate.export(spans);
        result.whenComplete(() -> {
            if (result.isSuccess()) {
                LOGGER.info(
                    "Successfully exported {} span(s) for service '{}' to collector {}",
                    spans.size(),
                    serviceName,
                    collectorEndpoint);
            } else {
                LOGGER.error(
                    "Failed to export {} span(s) for service '{}' to collector {}",
                    spans.size(),
                    serviceName,
                    collectorEndpoint);
            }
        });
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        LOGGER.info("Flushing spans for service '{}'", serviceName);
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        LOGGER.info("Shutting down span exporter for service '{}'", serviceName);
        return delegate.shutdown();
    }
}
