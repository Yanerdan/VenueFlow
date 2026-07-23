## Why

C10 can durably create and cancel reservations, but those state changes have no reliable event
record. Publishing directly after commit would lose events on process failure, while publishing
inside the transaction cannot make MySQL and RabbitMQ atomic. The next increment must establish
a recoverable local Outbox boundary before adding consumers, timeout workflows, or automated
reconciliation.

## What Changes

- Add an immutable Booking V002 migration for bounded Outbox event facts, publish attempts,
  claim leases, terminal publication state, and uniqueness by event ID.
- Write reservation-confirmed and reservation-cancelled Outbox events in the same local
  transaction as their Booking state change.
- Add an explicit messaging profile with RabbitMQ Publisher Confirm, Publisher Return,
  mandatory routing, bounded timeouts, and no automatic AMQP recovery that can hide outcomes.
- Add a multi-instance-safe Outbox scanner that leases eligible rows, publishes a stable event
  envelope, marks only positively confirmed events as published, and schedules bounded retry
  for negative, returned, timed-out, or interrupted outcomes.
- Add safe operational inspection and manual replay commands without allowing payload mutation
  or duplicate business events.
- Preserve Docker-free default startup and verification; add opt-in isolated MySQL and RabbitMQ
  evidence for commit-before-publish, crash recovery, duplicate scan, confirm, return, and retry
  behavior.
- Keep consumers, `ConsumedEvent`, dead-letter consumption, notification, timeout cancellation,
  Resource-owned Outbox, and cross-service reconciliation outside this Change.

## Capabilities

### New Capabilities

- `reliable-booking-event-publication`: Booking transactional Outbox persistence, event
  envelopes, leasing, RabbitMQ confirmed publication, retry, inspection, and manual replay.

### Modified Capabilities

- `booking-reservation-management`: Reservation confirmation and cancellation transactions also
  record one immutable Outbox event without changing the existing synchronous capacity
  coordination contract.
- `booking-service-skeleton`: Permit explicit Spring AMQP/Testcontainers support and messaging
  configuration while preserving the secret-free, connection-free default skeleton.

## Impact

- `venueflow-booking-service`: V002 migration, transactional finalization/cancellation changes,
  Outbox domain/persistence/application components, AMQP adapter, configuration, tests, and
  runbook.
- RabbitMQ: one durable topic exchange and bounded publisher routing keys; no consumer queue is
  introduced yet.
- MySQL: additive Booking-owned Outbox table only; C10 V001 and all other service migrations
  remain immutable.
- Dependencies: Spring AMQP and RabbitMQ Testcontainers are allowed only for this bounded
  capability; no Nacos, Feign, Redis, Gateway, tracing stack, or consumer framework is added.
