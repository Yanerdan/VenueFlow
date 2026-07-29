# Reliable Notification Consumption Specification

## Purpose

Define bounded, durable, and idempotent consumption of Booking lifecycle events by Notification
Service.

## Requirements

### Requirement: Notification owns durable consumer facts

Notification Service SHALL add an immutable V001 migration containing only Notification-owned
consumed-event, in-app notification, and consumption-failure tables. Consumed events MUST be
unique by `(consumer_name,event_id)` and retain event type, version, a canonical payload hash,
result, and consumption time. Notification records MUST be unique by consumer and event ID and
contain only bounded typed Booking facts and generated notification text.

Failure facts MUST contain bounded identifiers, attempt count, terminal state, stable error code,
and timestamps without raw payloads, broker bodies, stack traces, credentials, or connection
values. No other service migration or schema SHALL be changed or accessed.

#### Scenario: A clean Notification schema receives V001

- **WHEN** Flyway migrates a fresh Notification schema
- **THEN** all three Notification-owned tables and their constraints are created
- **AND** no Booking, User, or Resource table is created or referenced

#### Scenario: Duplicate consumed identity is inserted

- **WHEN** the same consumer tries to store the same event ID twice
- **THEN** the database uniqueness boundary prevents a second consumed-event fact

### Requirement: Booking event envelopes are validated before side effects

The consumer SHALL accept only UTF-8 JSON matching the bounded Booking envelope and the exact
`booking.reservation.confirmed.v1`, `booking.reservation.cancelled.v1`,
`booking.reservation.expired.v1`, or `booking.reservation.completed.v1` routing contract. It
MUST validate event ID, type/routing agreement, version, producer, aggregate identity, UTC
occurrence time, typed payload, status, content type, and configured byte limits before creating
a notification.

Unknown versions/types, malformed JSON, mismatched routing or payload status, missing required
facts, oversized content, and event-ID reuse with a different canonical hash MUST be terminal
failures and MUST NOT create a notification.

#### Scenario: A valid confirmation event arrives

- **WHEN** a confirmation envelope and routing key agree and pass all bounds
- **THEN** the consumer derives a confirmation notification from typed fields
- **AND** does not persist or log the raw envelope

#### Scenario: A valid expiration event arrives

- **WHEN** an expiration envelope has the exact routing key and `EXPIRED` payload status
- **THEN** the consumer derives one expiration notification through the existing inbox transaction
- **AND** duplicate delivery creates no second notification

#### Scenario: A valid completion event arrives

- **WHEN** a completion envelope has the exact routing key and `COMPLETED` payload status
- **THEN** the consumer derives one completion notification through the existing inbox transaction
- **AND** duplicate delivery creates no second notification

#### Scenario: An event identity is reused with different content

- **WHEN** an event ID already consumed by this consumer arrives with a different type, version,
  or canonical payload hash
- **THEN** the message is classified as an identity collision
- **AND** no existing consumed event or notification is changed

### Requirement: Consumer topology is durable and consumer-owned

Only the explicit `persistence,messaging` runtime SHALL create a Notification-owned durable work
queue, fixed-backoff retry queue, dead-letter exchange and queue, and exact bindings for the four
Booking routing keys on the existing durable `venueflow.events.v1` topic exchange. Messages and
republished retry/dead-letter messages MUST be persistent.

Queue names, prefetch, concurrency, retry delay, maximum attempts, message byte limit, and
publish-confirm timeout MUST be validated and bounded. Booking Service MUST NOT own or declare
Notification queues, and Notification MUST NOT modify the exchange ownership or event payload
contract.

#### Scenario: Messaging is explicitly enabled

- **WHEN** Notification starts with valid `persistence,messaging` database and RabbitMQ settings
- **THEN** its durable work, retry, and dead-letter topology is declared idempotently
- **AND** confirmation, cancellation, expiration, and completion routing keys are routable

#### Scenario: Default skeleton starts

- **WHEN** Notification starts without explicit persistence and messaging profiles
- **THEN** no datasource, RabbitMQ connection, listener container, queue, or binding is created

### Requirement: Consumption is transactional, idempotent, and manually acknowledged

For each valid delivery, Notification SHALL in one local MySQL transaction insert the consumed
identity and exactly one corresponding in-app notification record, then manually ACK only after
commit. A duplicate with the same consumer, event ID, type, version, and canonical hash SHALL
create no second notification and SHALL be ACKed after verifying the stored identity.

If the process stops after commit but before ACK, redelivery MUST converge through the same
deduplication boundary. A database or transaction failure MUST NOT ACK the original message.
Notification MUST describe this as at-least-once delivery with idempotent side effects, not
exactly-once messaging.

#### Scenario: The same event is delivered twice

- **WHEN** two deliveries with the same valid event identity and content are processed
- **THEN** exactly one consumed-event row and one notification record exist
- **AND** both deliveries can eventually be acknowledged

#### Scenario: The process stops after database commit

- **WHEN** consumed identity and notification commit but the broker has not recorded the ACK
- **THEN** RabbitMQ can redeliver the same event
- **AND** redelivery creates no duplicate notification

### Requirement: Failures use bounded retry and terminal dead-letter handling

A retryable processing failure SHALL be transferred as a persistent message to the durable retry
queue with an incremented bounded attempt header and fixed backoff. A malformed, unsupported,
identity-collision, or retry-exhausted message SHALL be transferred to the durable dead-letter
queue with a stable bounded failure code.

Notification MUST positively confirm and prove routing of the retry/dead-letter publication
before ACKing the original delivery. If transfer cannot be proven, it MUST NACK and requeue the
original. Retry count MUST never exceed the configured maximum, and automatic consumption MUST
never loop a terminal message back to the work queue.

#### Scenario: A transient database failure occurs

- **WHEN** processing fails below the maximum attempt count
- **THEN** the original is ACKed only after its persistent retry copy is confirmed and routed
- **AND** the retry queue returns it after the configured backoff

#### Scenario: A poison event arrives

- **WHEN** an event is malformed or exhausts its retry budget
- **THEN** it reaches the durable dead-letter queue with a stable failure classification
- **AND** it no longer blocks work-queue progress

#### Scenario: Retry transfer loses broker confirmation

- **WHEN** Notification cannot prove that the retry or dead-letter copy was accepted and routed
- **THEN** it does not ACK the original delivery
- **AND** RabbitMQ retains responsibility for redelivery

### Requirement: Dead-letter replay is explicit and idempotency-aware

Notification SHALL provide no anonymous replay HTTP endpoint. An application-level operator
command MAY inspect only bounded metadata for the next dead-letter message without removing it.
Replay MUST require preview, an expected event ID and SHA-256 fingerprint, a bounded operator
reason, and explicit confirmation.

A confirmed replay SHALL preserve the original event identity, routing key, and payload, reset
only the bounded delivery-attempt header, positively confirm routing to the work path, and ACK
the dead-letter source only after that confirmation. A failure after republish but before source
ACK MAY duplicate delivery and MUST remain safe through consumed-event deduplication.

#### Scenario: An operator previews a dead-letter message

- **WHEN** the replay command runs without confirmation
- **THEN** it shows only safe identity, routing, size, attempt, and failure metadata
- **AND** leaves the dead-letter message available

#### Scenario: An operator confirms replay

- **WHEN** expected identity, fingerprint, reason, and confirmation match the previewed message
- **THEN** the same event is returned to the work path with a reset bounded attempt count
- **AND** the dead-letter source is removed only after confirmed routing

### Requirement: Consumer operation is bounded and observable

Notification SHALL bound listener concurrency, prefetch, transaction duration, message size,
retry attempts, retry delay, publish-confirm wait, and replay inspection. Internal metrics/logs
MUST expose received, consumed, duplicate, retry, dead-letter, replay, ACK/NACK, outcome-code,
queue-depth, and oldest-message facts without payloads, generated notification bodies, secrets,
or unrestricted management endpoints.

#### Scenario: An operator inspects consumer health

- **WHEN** safe consumer metrics and logs are inspected
- **THEN** throughput, duplicate, retry, dead-letter, replay, and backlog outcomes are visible
- **AND** message bodies, notification text, credentials, and connection values are absent

### Requirement: Verification separates deterministic and real infrastructure evidence

Default Maven verification SHALL cover envelope validation, hashing, transaction decisions,
deduplication, notification derivation, ACK/NACK mapping, retry/dead-letter transfer, replay
guards, configuration, and architecture without Docker, MySQL, or RabbitMQ. An explicit
`consumer-it` profile SHALL use isolated MySQL 8.4 and RabbitMQ 4.1.8 to prove V001, topology,
manual ACK, duplicate redelivery, commit-before-ACK recovery, retry backoff, poison dead-letter,
confirmed replay, and connection recovery.

#### Scenario: Default verification runs without infrastructure

- **WHEN** root `clean verify` runs with Docker, MySQL, and RabbitMQ unavailable
- **THEN** Notification tests create no external connection or Testcontainers fixture

#### Scenario: Opt-in verification exercises reliable consumption

- **WHEN** the explicit `consumer-it` profile runs
- **THEN** isolated MySQL and RabbitMQ prove durable routing, idempotency, retry, dead-letter,
  replay, and recovery behavior
- **AND** the evidence makes no exactly-once delivery claim

### Requirement: Existing notifications form a bounded read-only inbox

Notification Service SHALL expose a DTO-only newest-first inbox query over existing
`notification_record` facts for one positive internal user ID. `pageNumber` MUST be zero-based,
`pageSize` MUST default to 20 and be limited to 100, and the response SHALL include page metadata
without consumed-event hashes, failure facts, broker metadata, SQL, or credentials.

#### Scenario: A user opens their inbox

- **WHEN** a valid user ID requests a bounded page
- **THEN** Notification returns only that user's records ordered by creation time and ID descending

#### Scenario: Inbox is unavailable in the default profile

- **WHEN** Notification runs only its connection-free skeleton
- **THEN** no inbox controller or database connection is created

### Requirement: Browser inbox tracks local attention state

The applicant workspace SHALL track read notification identifiers per signed-in identity in
browser-local storage and SHALL allow a notification carrying a booking reference to navigate to
the corresponding booking history.

#### Scenario: Notification is marked read

- **WHEN** an applicant opens a notification
- **THEN** that notification is visually marked read for the same identity in the same browser

#### Scenario: Notification references a booking

- **WHEN** an applicant opens a notification with a booking reference
- **THEN** the workspace presents booking history focused on that booking
