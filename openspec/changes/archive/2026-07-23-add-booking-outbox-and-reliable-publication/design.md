## Context

C10 persists a `CONFIRMED` reservation after Resource allocation and changes it to `CANCELLED`
after Resource release. Those local transactions are durable, but no event fact survives a
process stop between database commit and a later RabbitMQ send. The base Compose stack already
contains pinned RabbitMQ 4.1.8, while Booking default startup intentionally remains independent
of all external infrastructure.

The engineering baseline requires local transaction + Outbox, Publisher Confirm, Publisher
Return, mandatory routing, bounded retry, multi-instance-safe scanning, operational visibility,
and evidence for crash and duplicate delivery boundaries. C11 establishes the producer half
only. Consumers must later use `ConsumedEvent` because no producer design can guarantee
exactly-once delivery across MySQL and RabbitMQ.

## Goals / Non-Goals

**Goals:**

- Atomically record one immutable event for each effective reservation confirmation and
  cancellation.
- Publish eligible events at least once through RabbitMQ with positive-confirm evidence.
- Recover leases after publisher termination and safely run more than one publisher instance.
- Bound batches, timeouts, retries, payloads, logs, and retained errors.
- Keep default `skeleton` and Docker-free verification free of DB/MQ connections.
- Prove real MySQL/RabbitMQ behavior through an explicit opt-in integration profile.

**Non-Goals:**

- Consumer queues, manual ACK, `ConsumedEvent`, notifications, dead-letter consumers, timeout
  cancellation, Resource Outbox, automated reconciliation, Redis, Nacos, Feign, Gateway, auth,
  tracing exporters, or exactly-once delivery.
- Changing C10 synchronous User/Resource coordination or compensating release behavior.
- Publishing from inside a Booking database transaction.
- Modifying Booking V001 or any Resource/User migration.

## Decisions

### 1. Add Booking V002 with an explicit lease-based Outbox state machine

`V002__add_booking_outbox.sql` creates only `booking_outbox_event`:

```text
id
event_id                 globally unique UUID
aggregate_type           BOOKING
aggregate_id             bookingNo
event_type               BOOKING_RESERVATION_CONFIRMED | BOOKING_RESERVATION_CANCELLED
event_version            envelope schema version, initially 1
payload                   bounded JSON text
headers                   bounded JSON text
status                    NEW | PUBLISHING | RETRY | PUBLISHED | DEAD
retry_count
next_retry_at
claim_token               nullable UUID
lease_until               nullable
created_at
published_at              nullable
last_error_code           nullable bounded code, never raw exception/body
version                   optimistic version
```

Constraints include unique `event_id`, unique
`(aggregate_type, aggregate_id, event_type, event_version)`, valid status/type checks, nonnegative
retry count, bounded columns, and coherent nullable publish/lease fields where MySQL permits a
portable check. The second uniqueness rule makes accidental duplicate event creation fail
locally while normal HTTP replay performs no state transition and therefore inserts nothing.

Alternative: reuse `booking_idempotency` as an event record. Rejected because publication has a
different lifecycle, retry policy, retention period, and operational ownership.

### 2. Append Outbox rows in the existing short business transactions

`BookingRepository.complete` inserts the reservation, inserts the confirmation event, and marks
idempotency `SUCCEEDED` in one transaction. `BookingRepository.cancel` conditionally changes
`CONFIRMED -> CANCELLED` and inserts the cancellation event in the same transaction only when
the state update wins.

Event serialization happens before entering repository transaction code and is local/bounded;
no HTTP or AMQP call occurs inside either transaction. A failure to insert the Outbox row fails
the whole local transaction. For confirmation this retains C10 compensation behavior; for
cancellation the local state remains `CONFIRMED` after Resource's idempotent release and the
existing replay path can retry the conditional transition.

Alternative: publish after transaction commit through a listener. Rejected because a process
can stop before the listener publishes, losing the event permanently.

### 3. Use a stable versioned event envelope

The serialized JSON envelope contains only:

```json
{
  "eventId": "uuid",
  "eventType": "booking.reservation.confirmed",
  "eventVersion": 1,
  "occurredAt": "UTC instant",
  "producer": "venueflow-booking-service",
  "aggregateType": "BOOKING",
  "aggregateId": "bookingNo",
  "traceId": "bounded optional value",
  "payload": {
    "bookingNo": "...",
    "userId": 1,
    "slotId": 2,
    "quantity": 1,
    "status": "CONFIRMED"
  }
}
```

Cancellation uses `booking.reservation.cancelled` and `CANCELLED`. Routing keys append the
schema suffix: `booking.reservation.confirmed.v1` and
`booking.reservation.cancelled.v1`. Payload and headers are immutable after insert, capped by
configuration and schema, contain UTC timestamps, and exclude collaborator bodies, credentials,
JDBC/AMQP URLs, and internal numeric database IDs.

Alternative: Java-serialized objects or arbitrary maps. Rejected because they make compatibility,
size limits, and leakage hard to audit.

### 4. Claim with short transactions and publish outside transactions

Each scanner tick:

1. In a short transaction, select a bounded batch of `NEW`/due `RETRY` rows and expired
   `PUBLISHING` rows using MySQL 8 `FOR UPDATE SKIP LOCKED`.
2. Set `PUBLISHING`, a new `claim_token`, and `lease_until`; commit.
3. Publish each claimed immutable envelope outside the database transaction.
4. In another short transaction, condition on `event_id`, `PUBLISHING`, and `claim_token` to
   mark `PUBLISHED` or schedule `RETRY`/`DEAD`.

The publisher rechecks ownership during finalization. An old worker cannot overwrite a row
reclaimed after lease expiry. Batch size, scan delay, lease length, and worker concurrency are
bounded configuration values; the lease exceeds the confirm timeout with safety margin.

Alternative: hold `FOR UPDATE` locks while waiting for RabbitMQ. Rejected because network waits
inside database transactions exhaust connections and block other scanners.

### 5. Treat publication as successful only after confirm and routing evidence

The messaging adapter uses a durable topic exchange, persistent messages, publisher confirms,
publisher returns, `mandatory=true`, event ID correlation, and a bounded confirm timeout. A row
is `PUBLISHED` only when the broker positively ACKs it and no returned message is associated
with that correlation.

NACK, return, confirm timeout, connection failure, malformed local envelope, or interruption is
not success. Interrupt status is restored. A return is classified as `UNROUTABLE`; other stored
errors use stable codes such as `BROKER_UNAVAILABLE`, `CONFIRM_NACK`, `CONFIRM_TIMEOUT`, and
`PUBLISH_INTERRUPTED`.

The producer declares only its durable exchange. Consumer-owned queue bindings are not created
by Booking. Until a binding exists, mandatory publication is returned and remains retryable.
The opt-in test declares an isolated bound queue to prove the successful route and an unbound
routing key to prove Publisher Return.

Alternative: consider `RabbitTemplate.convertAndSend` returning normally as success. Rejected
because that proves only client-side handoff, not broker acceptance or routing.

### 6. Use bounded exponential retry and an explicit terminal state

Retry delay is deterministic exponential backoff capped by configuration. `retry_count` advances
once per failed claimed attempt. When the configured maximum is reached, the event becomes
`DEAD`; it is never silently deleted.

An application-level admin command can inspect safe metadata and requeue a `DEAD` event by
clearing lease/error scheduling fields and setting `RETRY`, while preserving event ID, payload,
headers, and retry audit facts. C11 exposes no anonymous mutation HTTP endpoint. The runbook
documents the explicit command, required profile, preview, event ID, and operator reason.

Alternative: infinite retry. Rejected because poison/unroutable events would consume the
publisher indefinitely and conceal operational failure.

### 7. Isolate messaging configuration from skeleton startup

Spring AMQP components are enabled only with an explicit `messaging` profile layered with
`persistence`. RabbitMQ host, port, username, password, virtual host, exchange, timeouts, lease,
batch, and retry settings come from environment variables or safe non-secret numeric/name
defaults. Required connection/credential variables have no usable fallback.

Default `skeleton` creates neither datasource nor RabbitMQ connection factory. The existing
`persistence` profile can still serve synchronous reservation APIs without starting the Outbox
publisher; accumulated `NEW` rows publish when `messaging` is later enabled.

Alternative: enable messaging whenever persistence starts. Rejected because it would make
Booking API availability depend on RabbitMQ and weaken C10's explicit runtime boundary.

### 8. Verification separates deterministic tests from real infrastructure evidence

Default verification uses fake clock, fake publisher, and repository ports to cover envelope
stability, transition matrices, retry/backoff, stale claimant rejection, interrupt preservation,
configuration boundaries, and no DB/MQ connection.

An opt-in `outbox-it` profile uses isolated MySQL 8.4 and RabbitMQ 4.1.8 Testcontainers to prove:

- V001 then V002 migration and constraints;
- reservation/outbox atomic commit and rollback;
- cancellation/outbox atomicity and replay;
- two scanner instances claim one row once per lease;
- expired `PUBLISHING` recovery;
- durable persistent message and stable envelope/routing key;
- ACK + routed result marks `PUBLISHED`;
- mandatory return remains non-published and follows retry/dead policy;
- simulated post-confirm/pre-finalize termination causes a duplicate attempt but no lost event.

No test claims exactly-once delivery. The default Maven lifecycle does not activate
Testcontainers.

## Risks / Trade-offs

- [ACK received but process stops before DB finalization can duplicate publication] → Document
  at-least-once semantics, preserve stable event ID, and require `ConsumedEvent` deduplication in
  the consumer Change.
- [No consumer binding makes mandatory messages unroutable] → Keep them retryable/dead with a
  visible error; deploy a consumer-owned binding before enabling production publishing.
- [Lease too short permits overlapping workers] → Validate lease > confirm timeout + margin and
  condition all finalization on claim token.
- [Large payload increases DB and broker pressure] → Enforce schema and application byte limits;
  events carry facts or references, never large content.
- [Publisher backlog grows while RabbitMQ is unavailable] → Bound batches/workers, expose
  backlog/oldest-age counters through the internal meter registry, and document inspection.
- [Manual replay can repeat a published side effect] → Preserve event ID and require future
  consumers to deduplicate; restrict C11 replay to terminal/non-published rows.

## Migration Plan

1. Apply additive Booking V002 while `messaging` is disabled.
2. Deploy code that atomically appends Outbox rows; synchronous APIs remain available.
3. Deploy consumer-owned queue bindings in a later Change or an integration environment.
4. Enable `persistence,messaging` with environment-only RabbitMQ credentials.
5. Observe backlog, return, confirm, retry, dead, and oldest-age facts before increasing worker
   concurrency.

Rollback disables `messaging` first, then rolls back application code while leaving V002 and
stored events intact. V001/V002 are never edited or automatically dropped. A later correction
uses V003.

## Open Questions

None for C11. Queue ownership, consumer names, dead-letter topology, notification behavior,
timeout cancellation, retention cleanup, and automated reconciliation are deliberately deferred
and must be specified by their owning Changes.
