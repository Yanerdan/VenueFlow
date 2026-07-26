# VenueFlow Notification Service

`venueflow-notification-service` consumes Booking confirmation, cancellation, expiration, and
completion events and creates deterministic in-app notification records.

## Default startup

The default `skeleton` profile listens on port `8085` and needs no Docker, MySQL, or
RabbitMQ:

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean package
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar
```

Only liveness and readiness are exposed:

```text
GET http://127.0.0.1:8085/actuator/health/liveness
GET http://127.0.0.1:8085/actuator/health/readiness
```

## Reliable consumer

Enable both `persistence,messaging` profiles and provide Notification-owned MySQL
credentials plus RabbitMQ credentials:

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence,messaging"
$env:VENUEFLOW_NOTIFICATION_DB_URL = "jdbc:mysql://127.0.0.1:3306/venueflow_notification"
$env:VENUEFLOW_NOTIFICATION_DB_USERNAME = "venueflow_notification"
$env:VENUEFLOW_NOTIFICATION_DB_PASSWORD = "<local-only-password>"
$env:VENUEFLOW_RABBITMQ_HOST = "127.0.0.1"
$env:VENUEFLOW_RABBITMQ_USERNAME = "<consumer-user>"
$env:VENUEFLOW_RABBITMQ_PASSWORD = "<local-only-password>"
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar
```

Flyway V001 creates the consumed-event inbox, in-app notification record, and bounded
failure-audit tables; V002/V003 add expiration/completion notification types. The listener
accepts only the exact versioned Booking lifecycle envelopes, commits inbox and notification
facts in one transaction, and then manually ACKs. Exact duplicate delivery is harmless.

The consumer declares a fixed-delay retry queue and a terminal DLQ. Transfers use
persistent messages, mandatory routing, and Publisher Confirm/Return; an uncertain
transfer is NACKed and requeued instead of being acknowledged.

## Verification

Default verification is Docker-free:

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean verify
```

Real MySQL 8.4.10 and RabbitMQ 4.1.8 evidence is opt-in:

```powershell
.\mvnw.cmd -pl venueflow-notification-service verify -Pconsumer-it
```

Operational details, topology, replay guardrails, shutdown, and rollback are in the
[Notification consumer runbook](../docs/runbook/notification-consumer.md).
