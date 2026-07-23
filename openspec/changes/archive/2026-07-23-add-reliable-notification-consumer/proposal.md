## Why

C11 publishes Booking events at least once, but without a consumer-owned binding they remain
unroutable and no notification side effect is durable. C13 completes the consumer half with
transactional deduplication, bounded failure handling, and evidence for duplicate and poison
messages while preserving C12's connection-free default.

## What Changes

- Add Notification-owned V001 persistence for consumed-event identity, in-app notification
  records, and bounded consumption-failure facts.
- Add explicit `persistence,messaging` runtime behavior for a durable consumer-owned Booking
  queue/binding, manual acknowledgement, strict envelope validation, and transactional
  `(consumer_name,event_id)` deduplication.
- Record one safe in-app notification for each effective Booking confirmation or cancellation;
  duplicate deliveries create no duplicate record.
- Add confirmed republish to a bounded retry queue with backoff, terminal dead-letter routing,
  stable error codes, metrics, and a preview/confirmation operator command for DLQ replay.
- Add Docker-free unit/architecture verification and an explicit `consumer-it` profile using
  isolated MySQL 8.4 and RabbitMQ 4.1.8.
- Keep email delivery, public notification APIs, timeout cancellation, Booking changes,
  cross-service calls, and production Compose application containers outside C13.

## Capabilities

### New Capabilities

- `reliable-notification-consumption`: Notification-owned persistence, consumer topology, manual
  ACK, transactional inbox deduplication, notification records, retry/DLQ, replay controls, and
  verification.

### Modified Capabilities

- `notification-service-skeleton`: Permit the explicitly profiled persistence and AMQP
  dependencies introduced by C13 while retaining the secret-free, connection-free default
  skeleton and restricted management surface.

## Impact

- `venueflow-notification-service`: dependencies, profile configuration, V001 migration,
  consumer/domain/persistence/messaging code, tests, and runbook documentation.
- MySQL: one Notification-owned schema with additive V001 tables only.
- RabbitMQ: consumer-owned durable queue, retry queue, dead-letter exchange/queue, and bindings
  to the existing `venueflow.events.v1` topic exchange.
- Runtime configuration: environment-only Notification database and RabbitMQ credentials plus
  bounded non-secret queue, timeout, prefetch, retry, and replay settings.
- C11 Booking producer and its migrations/event envelope remain unchanged.
