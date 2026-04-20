package com.example.telemetry;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenTelemetryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryFactory.class);
    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");

    private OpenTelemetryFactory() {
    }

    public static OpenTelemetrySdk create(String serviceName, String collectorEndpoint) {
        LOGGER.info("Initializing OpenTelemetry for service '{}' with OTLP HTTP endpoint {}", serviceName, collectorEndpoint);
        OtlpHttpSpanExporter otlpSpanExporter = OtlpHttpSpanExporter.builder()
            .setEndpoint(collectorEndpoint)
            .build();
        LoggingSpanExporter spanExporter = new LoggingSpanExporter(
            otlpSpanExporter,
            serviceName,
            collectorEndpoint);

        Resource resource = Resource.getDefault()
            .merge(Resource.builder()
                .put(SERVICE_NAME, serviceName)
                .build());

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance())))
            .build();
    }
}
