package com.example.telemetry;

import io.dropwizard.core.setup.Environment;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public final class OpenTelemetryInstaller {

    private OpenTelemetryInstaller() {
    }

    public static OpenTelemetrySdk install(
        Environment environment,
        String serviceName,
        String collectorEndpoint,
        String instrumentationName,
        String traceLogPath,
        String otlpBatchLogPath
    ) {
        OpenTelemetrySdk openTelemetry = OpenTelemetryFactory.create(serviceName, collectorEndpoint);
        environment.lifecycle().manage(new OpenTelemetryManaged(openTelemetry));
        environment.jersey().register(new TelemetryRequestListener(openTelemetry, instrumentationName));
        environment.jersey().register(new FileTraceRequestListener(serviceName, traceLogPath));
        environment.jersey().register(new OtlpBatchFileRequestListener(serviceName, otlpBatchLogPath));
        return openTelemetry;
    }
}
