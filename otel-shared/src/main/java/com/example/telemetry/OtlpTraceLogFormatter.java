package com.example.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OtlpTraceLogFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtlpTraceLogFormatter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OtlpTraceLogFormatter() {
    }

    static String buildExportTraceServiceRequest(
        String serviceName,
        String instrumentationScopeName,
        Instant startedAt,
        String method,
        String path,
        String traceId,
        String parentSpanId,
        String traceFlags,
        int status,
        String exceptionType
    ) {
        Instant endedAt = Instant.now();
        SpanContext spanContext = Span.current().getSpanContext();

        String effectiveTraceId = spanContext.isValid() ? spanContext.getTraceId() : traceId;
        String effectiveSpanId = spanContext.isValid() ? spanContext.getSpanId() : "missing";
        String effectiveTraceFlags = spanContext.isValid() ? spanContext.getTraceFlags().asHex() : traceFlags;

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("resourceSpans", List.of(resourceSpan(
            serviceName,
            instrumentationScopeName,
            startedAt,
            endedAt,
            method,
            path,
            effectiveTraceId,
            effectiveSpanId,
            parentSpanId,
            effectiveTraceFlags,
            status,
            exceptionType)));

        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to serialize OTLP trace log entry", exception);
            return "{\"serializationError\":\"true\"}";
        }
    }

    private static Map<String, Object> resourceSpan(
        String serviceName,
        String instrumentationScopeName,
        Instant startedAt,
        Instant endedAt,
        String method,
        String path,
        String traceId,
        String spanId,
        String parentSpanId,
        String traceFlags,
        int status,
        String exceptionType
    ) {
        Map<String, Object> resourceSpan = new LinkedHashMap<>();
        resourceSpan.put("resource", Map.of("attributes", List.of(attribute("service.name", serviceName))));
        resourceSpan.put("scopeSpans", List.of(scopeSpan(
            instrumentationScopeName,
            startedAt,
            endedAt,
            method,
            path,
            traceId,
            spanId,
            parentSpanId,
            traceFlags,
            status,
            exceptionType)));
        return resourceSpan;
    }

    private static Map<String, Object> scopeSpan(
        String instrumentationScopeName,
        Instant startedAt,
        Instant endedAt,
        String method,
        String path,
        String traceId,
        String spanId,
        String parentSpanId,
        String traceFlags,
        int status,
        String exceptionType
    ) {
        Map<String, Object> scopeSpan = new LinkedHashMap<>();
        scopeSpan.put("scope", Map.of("name", instrumentationScopeName));
        scopeSpan.put("spans", List.of(span(
            startedAt,
            endedAt,
            method,
            path,
            traceId,
            spanId,
            parentSpanId,
            traceFlags,
            status,
            exceptionType)));
        return scopeSpan;
    }

    private static Map<String, Object> span(
        Instant startedAt,
        Instant endedAt,
        String method,
        String path,
        String traceId,
        String spanId,
        String parentSpanId,
        String traceFlags,
        int status,
        String exceptionType
    ) {
        Map<String, Object> span = new LinkedHashMap<>();
        span.put("traceId", traceId);
        span.put("spanId", spanId);
        span.put("parentSpanId", parentSpanId);
        span.put("name", method + " " + path);
        span.put("kind", "SPAN_KIND_SERVER");
        span.put("startTimeUnixNano", toUnixNanos(startedAt));
        span.put("endTimeUnixNano", toUnixNanos(endedAt));
        span.put("traceState", "");
        span.put("flags", traceFlags);
        span.put("attributes", attributes(method, path, status, exceptionType));
        span.put("events", events(startedAt, endedAt));
        span.put("status", Map.of(
            "code", status >= 500 || !exceptionType.isEmpty() ? "STATUS_CODE_ERROR" : "STATUS_CODE_UNSET",
            "message", exceptionType));
        return span;
    }

    private static List<Map<String, Object>> attributes(String method, String path, int status, String exceptionType) {
        List<Map<String, Object>> attributes = new ArrayList<>();
        attributes.add(attribute("http.request.method", method));
        attributes.add(attribute("url.path", path));
        attributes.add(attribute("http.response.status_code", status));
        if (!exceptionType.isEmpty()) {
            attributes.add(attribute("exception.type", exceptionType));
        }
        return attributes;
    }

    private static List<Map<String, Object>> events(Instant startedAt, Instant endedAt) {
        return List.of(
            event("request.received", startedAt),
            event("response.sent", endedAt));
    }

    private static Map<String, Object> event(String name, Instant at) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("name", name);
        event.put("timeUnixNano", toUnixNanos(at));
        event.put("attributes", List.of());
        return event;
    }

    private static Map<String, Object> attribute(String key, String value) {
        return Map.of(
            "key", key,
            "value", Map.of("stringValue", value));
    }

    private static Map<String, Object> attribute(String key, int value) {
        return Map.of(
            "key", key,
            "value", Map.of("intValue", value));
    }

    private static String toUnixNanos(Instant instant) {
        return Long.toString(instant.getEpochSecond() * 1_000_000_000L + instant.getNano());
    }
}
