# Notification reliable consumer runbook

## Runtime boundary

The default `skeleton` profile remains infrastructure-free. The reliable consumer runs
only with `persistence,messaging`; selecting `messaging` without `persistence` fails
startup. Credentials come only from environment variables:

- `VENUEFLOW_NOTIFICATION_DB_URL`, `VENUEFLOW_NOTIFICATION_DB_USERNAME`,
  `VENUEFLOW_NOTIFICATION_DB_PASSWORD`
- `VENUEFLOW_RABBITMQ_HOST`, `VENUEFLOW_RABBITMQ_PORT`,
  `VENUEFLOW_RABBITMQ_USERNAME`, `VENUEFLOW_RABBITMQ_PASSWORD`,
  `VENUEFLOW_RABBITMQ_VHOST`

The RabbitMQ identity needs configure/consume access to the Notification topology and
publish access to its retry/dead exchanges. Do not commit real credentials.

## Topology and delivery

| Role | Default name |
| --- | --- |
| C11 source topic exchange | `venueflow.events.v1` |
| Work queue | `venueflow.notification.booking.v1` |
| Accepted routes | `booking.reservation.confirmed.v1`, `booking.reservation.cancelled.v1` |
| Retry exchange/queue | `venueflow.notification.retry.v1` / `venueflow.notification.booking.retry.v1` |
| Dead exchange/queue | `venueflow.dead.v1` / `venueflow.notification.booking.dlq.v1` |

The work queue uses manual ACK. Notification commits the consumed identity and one
notification in a single short transaction before ACK. A crash after commit but before
ACK causes redelivery; inbox deduplication makes it harmless. Transient failures move to
the fixed-delay retry queue and return to the source exchange. Terminal or exhausted
messages move to the DLQ. The source message is ACKed only after the transfer is
confirmed; unknown broker outcomes are NACKed with requeue.

Start Booking publication before the Notification consumer when recovering an empty
environment so the source exchange exists first. On shutdown, stop the consumer and
allow the listener container to finish its bounded in-flight work before stopping
RabbitMQ or MySQL.

## Monitoring

Monitor bounded counters/logs for received, consumed, duplicate, retry, dead-letter,
replay, ACK/NACK, and outcome. Also alert on work/retry/DLQ depth and oldest observed
message age. Logs contain event identifiers, routing keys, attempts, and failure codes;
they never contain message bodies, notification text, credentials, or endpoints.

## DLQ preview and replay

The application command defaults to no action. Preview reads only bounded metadata and
returns the message to the DLQ:

```powershell
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence,messaging `
  --venueflow.notification.admin.action=PREVIEW_DLQ
```

Replay requires the previewed event identity and fingerprint, a bounded audit reason,
and explicit confirmation:

```powershell
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence,messaging `
  --venueflow.notification.admin.action=REPLAY_DLQ `
  --venueflow.notification.admin.expected-identity="<event-id>" `
  --venueflow.notification.admin.expected-fingerprint="<sha256>" `
  --venueflow.notification.admin.reason="<operator reason>" `
  --venueflow.notification.admin.confirm=true
```

Replay republishes to the original route, resets the bounded attempt count, waits for
confirmed routing, and only then ACKs the DLQ copy. A replayed duplicate remains
harmless at the inbox boundary.

## Rollback

Stop the Notification consumer first. Roll back the application binary/configuration,
but never edit or delete V001 and never run Flyway clean. Existing work, retry, and DLQ
messages may remain for a later compatible deployment. If rollback removes consumer
compatibility, pause C11 Booking publication; do not purge queues or volumes as part of
normal rollback.
