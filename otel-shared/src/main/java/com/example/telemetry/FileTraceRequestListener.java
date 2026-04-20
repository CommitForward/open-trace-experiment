package com.example.telemetry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
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
public class FileTraceRequestListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTraceRequestListener.class);

    private final String serviceName;
    private final Path traceLogPath;

    public FileTraceRequestListener(String serviceName, String traceLogPath) {
        this.serviceName = serviceName;
        this.traceLogPath = Path.of(traceLogPath);
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        // No application lifecycle handling needed.
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new TraceFileRequestEventListener(serviceName, traceLogPath, requestEvent);
    }

    private static final class TraceFileRequestEventListener implements RequestEventListener {

        private final String serviceName;
        private final Path traceLogPath;
        private final Instant startedAt;
        private final String method;
        private final String path;
        private final String traceId;
        private final String parentSpanId;
        private final String traceFlags;

        private TraceFileRequestEventListener(String serviceName, Path traceLogPath, RequestEvent requestEvent) {
            ContainerRequestContext request = requestEvent.getContainerRequest();
            this.serviceName = serviceName;
            this.traceLogPath = traceLogPath;
            this.startedAt = Instant.now();
            this.method = request.getMethod();
            this.path = "/" + request.getUriInfo().getPath(false);

            String[] traceContext = parseTraceparent(firstHeader(request, "traceparent"));
            this.traceId = traceContext[0];
            this.parentSpanId = traceContext[1];
            this.traceFlags = traceContext[2];
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() != RequestEvent.Type.FINISHED) {
                return;
            }

            int status = event.getContainerResponse() != null ? event.getContainerResponse().getStatus() : 0;
            String exceptionType = event.getException() != null ? event.getException().getClass().getName() : "";
            String logEntry = OtlpTraceLogFormatter.buildExportTraceServiceRequest(
                serviceName,
                "file-trace-listener",
                startedAt,
                method,
                path,
                traceId,
                parentSpanId,
                traceFlags,
                status,
                exceptionType);

            try {
                Path parent = traceLogPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(
                    traceLogPath,
                    logEntry + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
            } catch (IOException exception) {
                LOGGER.error("Failed to append trace log to {}", traceLogPath, exception);
            }
        }

        private static String firstHeader(ContainerRequestContext request, String key) {
            List<String> values = request.getHeaders().get(key);
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(0);
        }

        private static String[] parseTraceparent(String traceparent) {
            if (traceparent == null) {
                return new String[] {"missing", "missing", "00"};
            }

            String[] parts = traceparent.split("-");
            if (parts.length != 4) {
                return new String[] {"invalid", "invalid", "00"};
            }

            return new String[] {parts[1], parts[2], parts[3]};
        }
    }
}
