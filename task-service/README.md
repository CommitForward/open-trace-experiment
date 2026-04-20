# Task Service Sample

This is a sample Dropwizard 3 service with:

- `POST /tasks` to create a task
- `GET /tasks/{id}` to fetch a task from in-memory storage
- an outbound call to the notification service when a task is created
- a Jersey request event listener that starts and ends an OpenTelemetry span per incoming request
- OTLP HTTP/protobuf export to an OpenTelemetry Collector
- W3C trace context propagation to the notification service

## Prerequisites

- Java 17+
- Maven 3.6+
- an OpenTelemetry Collector listening on `http://localhost:4318/v1/traces` or a custom endpoint configured in `config.yml`
- the notification service running on `http://localhost:8082/notifications/send` or a custom endpoint configured in `config.yml`

## Run

```bash
mvn package
java -jar target/task-service-1.0.0-SNAPSHOT.jar server config.yml
```

## Example Requests

Create a task:

```bash
curl -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -H 'traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01' \
  -d '{"title":"buy milk","description":"before 6 PM"}'
```

Get a task:

```bash
curl http://localhost:8080/tasks/<task-id>
```
