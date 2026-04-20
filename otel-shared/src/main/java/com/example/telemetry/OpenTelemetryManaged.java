package com.example.telemetry;

import io.dropwizard.lifecycle.Managed;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class OpenTelemetryManaged implements Managed {

    private final OpenTelemetrySdk openTelemetry;

    public OpenTelemetryManaged(OpenTelemetrySdk openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void start() {
        // OpenTelemetry is initialized eagerly.
    }

    @Override
    public void stop() {
        openTelemetry.getSdkTracerProvider().shutdown();
        openTelemetry.close();
    }
}
