# Reliable Booking Event Publication Specification

## Purpose

Define Booking-owned transactional Outbox persistence, bounded multi-instance publication,
RabbitMQ confirmation, retry, recovery, operational visibility, and verification boundaries.

## Requirements

### Requirement: Booking owns immutable Outbox event facts

Booking Service SHALL add an immutable V002 migration creating only a Booking-owned Outbox
table. Each row MUST contain a globally unique event ID, aggregate type and identifier, bounded
event type and schema version, immutable payload and headers, publication status, retry and
schedule facts, claim lease facts, optimistic version, and audit timestamps.

Status MUST be one of `NEW`, `PUBLISHING`, `RETRY`, `PUBLISHED`, or `DEAD`. Event identity MUST
be unique, and one aggregate MUST NOT persist duplicate event type/version facts for the same
effective state transition. V001 and every other service migration MUST remain unchanged.

#### Scenario: A clean Booking schema receives V002

- **WHEN** Flyway migrates a fresh Booking schema
- **THEN** V001 and V002 apply in order
- **AND** the Outbox schema enforces identity, lifecycle, retry, lease, and size boundaries

#### Scenario: Duplicate business event creation is attempted

- **WHEN** the same booking transition tries to insert the same event type and version again
- **THEN** Booking persists no duplicate Outbox fact

### Requirement: Booking events use a stable bounded envelope

Every published event MUST use UTF-8 JSON containing event ID, event type, positive schema
version, UTC occurrence time, producer, aggregate type and identifier, optional bounded trace
ID, and a typed bounded payload. Booking SHALL publish
`booking.reservation.confirmed.v1`, `booking.reservation.cancelled.v1`,
`booking.reservation.expired.v1`, and `booking.reservation.completed.v1` routing keys with
payload status matching the effective committed Booking state.

Events MUST NOT contain credentials, connection strings, raw collaborator bodies, stack traces,
large binary content, or internal database implementation details. Payload and header bytes MUST
be rejected before persistence when they exceed configured/schema limits.

#### Scenario: A confirmed reservation event is serialized

- **WHEN** Booking confirms a pending reservation and creates its Outbox event
- **THEN** its envelope has the stable event ID, versioned type, UTC time, booking number, user,
  slot, quantity, and `CONFIRMED` status
- **AND** it contains no secret or infrastructure configuration

#### Scenario: An expired reservation event is serialized

- **WHEN** Booking commits a proven timeout expiration
- **THEN** the envelope uses the expiration routing key and `EXPIRED` status
- **AND** it contains only the existing bounded reservation identity and quantity facts

#### Scenario: A completed reservation event is serialized

- **WHEN** Booking commits an eligible check-in
- **THEN** the envelope uses the completion routing key and `COMPLETED` status
- **AND** it contains only the existing bounded reservation identity and quantity facts

#### Scenario: An event exceeds the size limit

- **WHEN** a generated envelope exceeds the configured payload or header limit
- **THEN** the local business transaction fails before an oversized event is committed

### Requirement: Outbox scanning is bounded and multi-instance safe

An enabled Booking publisher SHALL claim only a configured bounded batch of `NEW`, due `RETRY`,
or lease-expired `PUBLISHING` rows in a short local transaction. Each claim MUST set a unique
claim token and lease deadline before commit. Publication MUST occur after that claim
transaction commits.

Finalization MUST condition on event ID, `PUBLISHING` status, and claim token. An expired or
superseded worker MUST NOT overwrite a newer claim. Scanner concurrency, batch size, scan delay,
lease duration, and confirm wait MUST be bounded, and lease duration MUST exceed the maximum
confirm wait with a safety margin.

#### Scenario: Two publisher instances scan the same row

- **WHEN** two instances concurrently scan one eligible Outbox event
- **THEN** only one active lease owns that publication attempt
- **AND** neither instance waits for RabbitMQ while holding the claim transaction

#### Scenario: A publisher stops after claiming

- **WHEN** a `PUBLISHING` lease expires without finalization
- **THEN** a later scanner can reclaim the same immutable event with a new token
- **AND** the stale worker cannot finalize the reclaimed row

### Requirement: Publication requires broker confirm and successful routing

Booking SHALL publish persistent messages to a durable topic exchange using `mandatory=true`,
event ID correlation, Publisher Confirm, and Publisher Return. It MUST mark an event
`PUBLISHED` only after a positive broker ACK within the configured timeout and when no returned
message exists for that correlation.

A broker NACK, mandatory return, confirm timeout, connection failure, malformed local message,
or interrupted wait MUST NOT mark the row published. The adapter MUST preserve thread interrupt
status and MUST store only a stable bounded failure code rather than raw broker bodies or
exceptions.

#### Scenario: Broker accepts and routes an event

- **WHEN** RabbitMQ durably accepts the persistent message, ACKs it, and routes it
- **THEN** Booking conditionally marks the claimed Outbox row `PUBLISHED`
- **AND** records the publication timestamp

#### Scenario: Mandatory publication is returned

- **WHEN** no queue binding accepts the routing key
- **THEN** Publisher Return classifies the attempt as `UNROUTABLE`
- **AND** the Outbox row remains non-published

#### Scenario: Confirm wait is interrupted

- **WHEN** the publisher thread is interrupted while waiting for confirmation
- **THEN** Booking records a retryable interrupted outcome
- **AND** restores the thread interrupt flag

### Requirement: Failed publication has bounded retry and terminal handling

Booking SHALL schedule failed attempts with deterministic bounded exponential backoff and SHALL
increment retry count once per finalized failed claim. When the configured maximum attempt count
is reached, the row MUST become `DEAD` and MUST NOT be silently deleted or automatically retried.

An explicit application-level operator command MAY inspect safe event metadata and requeue only
non-published terminal events. Requeue MUST preserve event ID, payload, headers, original
creation time, and prior retry audit facts; it MUST require an event ID, operator reason, and
preview/confirmation flow. C11 MUST expose no anonymous HTTP replay endpoint.

#### Scenario: A transient broker failure occurs

- **WHEN** a claimed attempt fails below the maximum attempt count
- **THEN** Booking marks it `RETRY` with a future bounded retry time and stable error code

#### Scenario: Retry budget is exhausted

- **WHEN** the maximum publication attempt count is reached without proven success
- **THEN** Booking marks the event `DEAD`
- **AND** later automatic scans do not claim it

#### Scenario: An operator requeues a dead event

- **WHEN** an operator explicitly confirms replay of a `DEAD` event with a reason
- **THEN** Booking makes the same immutable event eligible for a later attempt
- **AND** does not create a new event ID or mutate its business payload

### Requirement: Publication remains at-least-once and operationally visible

Booking MUST document that a process can stop after broker ACK and before MySQL finalization,
causing the same event ID to be published again. C11 MUST NOT claim exactly-once delivery.
Backlog count, oldest eligible age, claim count, confirmed count, returned count, retry count,
dead count, and outcome code MUST be available through internal application metrics/logs without
exposing payloads, credentials, connection values, or unrestricted Actuator endpoints.

#### Scenario: Process stops after broker confirmation

- **WHEN** RabbitMQ accepted an event but Booking stopped before marking it published
- **THEN** lease recovery can publish the same event ID again
- **AND** no event is lost or falsely marked published

#### Scenario: An operator inspects publisher health

- **WHEN** safe Outbox operational facts are inspected
- **THEN** backlog, age, outcome, retry, and dead facts are available
- **AND** event payloads and secrets are not exposed

### Requirement: Default verification remains infrastructure-free

Default Maven verification SHALL cover envelopes, state transitions, leasing, claim-token
guards, retry/backoff, confirm outcome mapping, interruption, configuration, and architecture
without Docker, MySQL, or RabbitMQ. An explicit `outbox-it` profile SHALL use fresh isolated
MySQL 8.4 and RabbitMQ 4.1.8 to prove V002, transaction atomicity, concurrent claiming, lease
recovery, persistent routed publication, mandatory return, and duplicate-attempt recovery.

#### Scenario: Default verification runs without RabbitMQ

- **WHEN** root `clean verify` runs with no Docker or external services
- **THEN** no Outbox test opens a MySQL or RabbitMQ connection

#### Scenario: Opt-in verification exercises real publication

- **WHEN** the explicit Outbox integration profile runs
- **THEN** fresh MySQL and RabbitMQ prove confirmed routing and failure recovery
- **AND** the evidence does not assert exactly-once delivery
