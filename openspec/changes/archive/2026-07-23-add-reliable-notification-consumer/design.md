## Context

C11 atomically records and publishes Booking confirmation/cancellation events with stable event
IDs and at-least-once semantics. C12 provides an independent Notification Service but deliberately
owns no database, queue, or listener. Until a consumer-owned binding exists, mandatory C11
publication is returned as unroutable; after routing exists, duplicates and poison messages must
be handled without blocking Booking.

C13 introduces the first durable consumer. It must keep default startup connection-free, use
only Notification-owned state, manually acknowledge RabbitMQ deliveries, and make every
side-effect idempotent across process stops.

## Goals / Non-Goals

**Goals:**

- Persist consumer identity and one safe in-app notification per effective Booking event.
- Own durable work/retry/dead-letter topology for the two C11 routing keys.
- ACK only after a committed local transaction or a confirmed retry/dead-letter transfer.
- Bound retries, poison handling, listener resources, payloads, logs, and operator replay.
- Prove duplicate, crash-window, retry, DLQ, and replay behavior against real MySQL/RabbitMQ.
- Preserve the C12 secret-free, connection-free default skeleton.

**Non-Goals:**

- Email/SMS delivery, templates, public notification read APIs, WebSocket push, or user preferences.
- Booking timeout/expiration, delayed cancellation, Resource/Search consumers, or reconciliation.
- Modifying Booking events, exchanges, Outbox state, or any existing migration.
- Gateway, authentication, Nacos/Feign, Redis, tracing exporters, or application Compose services.
- Exactly-once delivery across RabbitMQ and MySQL.

## Decisions

### 1. Add one Notification-owned V001 with three bounded tables

V001 creates:

- `notification_consumed_event`: consumer name, event ID/type/version, canonical SHA-256 payload
  hash, result, and consumed time; unique `(consumer_name,event_id)`.
- `notification_record`: event identity, user ID, booking number, notification type, bounded
  generated title/body, and creation time; unique `(consumer_name,event_id)`.
- `notification_consume_failure`: nullable safe event identity/fingerprint, routing key, attempt,
  stable error code, terminal/replay audit facts, and timestamps.

No raw envelope, exception, credentials, or cross-service foreign key is stored. The consumed
identity and notification are committed in the same short local transaction.

Alternative: use only RabbitMQ message IDs for deduplication. Rejected because broker delivery
state cannot atomically protect the MySQL side effect across commit-before-ACK failure.

### 2. Consume only the exact C11 envelope and routes

The consumer accepts only `booking.reservation.confirmed.v1` and
`booking.reservation.cancelled.v1` from `venueflow.events.v1`. It verifies content type, byte
limit, JSON structure, event ID, producer, type/version/routing agreement, UTC time, aggregate,
typed payload, and matching status before entering business logic.

A deterministic canonical representation is hashed with SHA-256. A duplicate is benign only
when consumer, event ID, type, version, and hash all match. Reuse of an event ID with different
content is terminal.

Alternative: deserialize to an arbitrary map and trust the routing key. Rejected because it
weakens compatibility, size, collision, and data-leak boundaries.

### 3. Layer explicit `persistence,messaging` profiles over the skeleton

`persistence` enables Notification's datasource, Flyway, Spring JDBC, and V001. `messaging`
requires persistence and enables RabbitMQ topology/listeners; startup fails fast when required
database/RabbitMQ variables are absent or invalid. The default `skeleton` keeps datasource,
Flyway, RabbitMQ auto-configuration, and listeners disabled.

Alternative: enable messaging whenever the JAR starts. Rejected because RabbitMQ or MySQL failure
would destroy C12's standalone health and build boundary.

### 4. Let Notification own durable queue topology

C13 declares:

```text
source exchange: venueflow.events.v1
work queue:      venueflow.notification.booking.v1
retry exchange: venueflow.notification.retry.v1
retry queue:     venueflow.notification.booking.retry.v1
dead exchange:  venueflow.dead.v1
dead queue:      venueflow.notification.booking.dlq.v1
bindings:        booking.reservation.confirmed.v1
                 booking.reservation.cancelled.v1
```

The retry queue uses a bounded fixed TTL and dead-letters back to the source exchange while
preserving the original routing key. Exact names may be environment-overridden with validated
safe values. Work/retry/dead queues and exchanges are durable; messages are persistent.

Alternative: let Booking declare consumer queues. Rejected because queue lifecycle, retry, and
dead-letter policy belong to the consumer.

### 5. Commit local facts before manual ACK

The listener uses manual acknowledgement with bounded prefetch/concurrency:

1. Validate and canonicalize the envelope.
2. In one short transaction, insert consumed identity and notification.
3. Commit.
4. ACK.

On duplicate-key conflict it loads the stored consumed identity. An exact match is ACKed without
another notification; a mismatch is terminal. If the process stops after step 3, redelivery
converges through the unique key. No network call occurs inside the database transaction.

Alternative: ACK before commit. Rejected because a process stop could permanently lose the
notification.

### 6. Transfer failures with confirm-before-ACK

Retryable failures below the maximum are republished persistently to the retry exchange with an
incremented bounded `x-venueflow-attempt` header. Invalid, unsupported, collision, or exhausted
messages are republished to the dead exchange with a stable error-code header.

The transfer uses mandatory routing, Publisher Confirm/Return, and bounded wait. The original is
ACKed only after positive confirm and no return; otherwise it is NACKed with requeue. This can
duplicate transfer during a crash window, so the stable event ID remains the correctness
boundary. Fixed retry delay is chosen for C13 simplicity; multiple backoff tiers can be added
later only with evidence.

Alternative: repeatedly NACK/requeue the work message. Rejected because poison messages would
hot-loop and no bounded terminal policy would exist.

### 7. Provide a bounded application-level DLQ replay command

There is no replay HTTP endpoint. A command performs a manual `basic.get` of the next DLQ item,
shows only event ID/fingerprint, routing key, bytes, attempts, and stable error code, then returns
it to the queue during preview. Confirmed replay requires both expected identity and fingerprint,
operator reason; it republishes to the source exchange with attempt reset, waits for confirm and
routing, then ACKs the DLQ source.

A stop after republish and before DLQ ACK can duplicate the event; consumed-event deduplication
keeps the notification side effect safe.

Alternative: bulk automatic DLQ drain. Rejected because it can reactivate an entire poison set
without diagnosis or an audit reason.

### 8. Separate deterministic tests from opt-in infrastructure evidence

Default tests use repository/publisher/ack ports and fake clock to cover validation, canonical
hashes, dedup decisions, notification derivation, retry classification, confirm mapping, replay
guards, configuration, and connection absence.

`consumer-it` uses pinned MySQL 8.4 and RabbitMQ 4.1.8 Testcontainers to prove V001, topology,
manual ACK, exact duplicate, commit-before-ACK redelivery, retry TTL, poison DLQ, replay, and
connection recovery. Default/root `clean verify` never starts Testcontainers.

Alternative: only mock the listener. Rejected because broker redelivery, TTL/DLX, confirm routing,
and transaction/ACK crash windows require real infrastructure evidence.

## Risks / Trade-offs

- [Commit succeeds but ACK fails] → Stable event identity and transactional unique keys make
  redelivery harmless.
- [Retry republish succeeds but original ACK fails] → Duplicate retry copies remain safe through
  the same deduplication boundary.
- [Single retry TTL is coarse] → Keep C13 deterministic and bounded; add tiered delays only when
  operational evidence requires them.
- [DLQ replay repeats an already completed side effect] → Preserve event ID and require preview,
  expected identity, reason, confirmation, and consumed-event verification.
- [Database unavailable prevents failure-row persistence] → Retain the broker message, emit only
  stable metrics/logs, and persist failure facts when storage is available.
- [C11 DEAD events remain unpublished from earlier unroutable attempts] → Operators use the
  existing C11 preview/confirmed requeue after C13 bindings are deployed.

## Migration Plan

1. Apply Notification V001 with `messaging` disabled.
2. Deploy Notification with the default skeleton and verify liveness/readiness.
3. Start `persistence,messaging` to declare consumer topology and begin consumption.
4. Verify queue binding and Notification consumer metrics.
5. Use the existing C11 operator command to requeue any `UNROUTABLE` Booking Outbox events.
6. Observe duplicates, retries, DLQ depth, oldest age, and database health before increasing
   listener concurrency.

Rollback stops the Notification messaging profile first, preserving work/retry/DLQ messages and
V001 facts. Application code may roll back while V001 remains; migrations are never edited or
automatically dropped.

## Open Questions

None for C13. Notification read APIs, templates, delivery channels, retention cleanup, timeout
cancellation, and reconciliation remain separate Changes.
