# Notification Service Sample

This is a Dropwizard 3 notification service with:

- `POST /notifications/send` to accept a notification request
- file-backed request recording in `notificationLogPath`
- a randomized 10 second delay for about 2 out of every 10 responses
- OpenTelemetry request tracing exported over OTLP HTTP to `http://localhost:4318/v1/traces`
- a shared telemetry library used by both services in this repository

## Prerequisites

- Java 17+
- Maven 3.6+
- an OpenTelemetry Collector listening on `http://localhost:4318/v1/traces`

## Run

```bash
mvn -pl notification-service -am package
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar server notification-service/config.yml
```

## Example Request

```bash
curl -X POST http://localhost:8082/notifications/send \
  -H 'Content-Type: application/json' \
  -d '{"recipient":"alice@example.com","channel":"EMAIL","message":"build finished"}'
```
