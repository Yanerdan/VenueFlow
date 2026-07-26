## ADDED Requirements

### Requirement: Booking persists completion as an additive lifecycle fact

Booking Service SHALL add an immutable V005 migration that permits `COMPLETED` reservations and
adds a nullable UTC completion timestamp. The migration MUST preserve V001-V004 and all existing
reservation states, deadlines, leases, audits, and Outbox facts. A completed reservation MUST
retain its allocation and MUST NOT become an expiration or reconciliation release candidate.

#### Scenario: A clean Booking schema receives V005

- **WHEN** Flyway migrates a fresh Booking schema
- **THEN** V001 through V005 apply in order
- **AND** the lifecycle permits `COMPLETED` with a bounded completion timestamp

#### Scenario: Existing reservations are upgraded

- **WHEN** V005 is applied to existing Booking data
- **THEN** each existing lifecycle state remains unchanged
- **AND** no existing reservation is marked completed

### Requirement: Check-in uses bounded Resource-owned slot time

Booking SHALL expose `POST /api/v1/bookings/{bookingNo}/check-in`. For a currently
`CONFIRMED` reservation it MUST perform one bounded read of the existing Resource slot API,
validate the returned slot identity and UTC interval, and permit check-in only from the slot
start minus the configured early window through the slot end plus the configured late window.
Both windows and HTTP timeouts MUST be positive or zero, fail-fast, and bounded.

Resource unavailability, malformed facts, identity mismatch, or a request outside the window
MUST return a distinct stable error and MUST leave Booking, audit, Outbox, and capacity facts
unchanged.

#### Scenario: A confirmed reservation is inside its check-in window

- **WHEN** Resource returns the matching slot and current time is inside the configured window
- **THEN** Booking may attempt the local completion transition

#### Scenario: Check-in is too early or too late

- **WHEN** current time is outside the bounded check-in window
- **THEN** Booking returns a stable window error
- **AND** no reservation state, audit, event, or capacity fact changes

#### Scenario: Resource slot lookup fails

- **WHEN** Booking cannot obtain valid matching slot facts within its timeout
- **THEN** it returns a safe downstream or contract error
- **AND** the confirmed reservation remains retryable

### Requirement: Completion is atomic state-idempotent and race-safe

Booking MUST condition `CONFIRMED -> COMPLETED` on booking identity, status, and optimistic
version. The winning local transaction SHALL set the UTC completion time, append one status
audit, and append one completion Outbox event. It MUST perform no Resource write and MUST NOT
release capacity.

A completed replay SHALL return the same reservation without another slot read, audit, or event.
Pending, cancelled, or expired reservations MUST reject check-in. Concurrent cancellation and
completion MUST have one winner, and the losing path MUST append no event.

#### Scenario: Completion transition wins

- **WHEN** an eligible confirmed reservation wins the conditional update
- **THEN** Booking atomically commits `COMPLETED`, its timestamp, one audit, and one event

#### Scenario: Completed check-in is replayed

- **WHEN** the check-in endpoint is called for an already completed reservation
- **THEN** Booking returns the same completed reservation
- **AND** no collaborator call, audit, or event is repeated

#### Scenario: Cancellation races check-in

- **WHEN** cancellation and eligible completion compete for one confirmed reservation
- **THEN** exactly one terminal state wins
- **AND** only the winning transition appends its matching event

### Requirement: Completion verification is isolated and bounded

Default Maven verification SHALL cover configuration, temporal boundaries, state transitions,
replay, races, envelopes, web errors, and architecture without Docker, MySQL, Resource, or
RabbitMQ. An explicit `checkin-it` profile SHALL use MySQL 8.4.10 and a bounded HTTP stub to prove
V005, Resource slot validation, transition/event/audit atomicity, replay, and cancellation race
outcomes.

#### Scenario: Default verification runs

- **WHEN** root `clean verify` runs without infrastructure
- **THEN** no check-in test opens an external connection or Testcontainers fixture

#### Scenario: Opt-in check-in verification runs

- **WHEN** the explicit `checkin-it` profile runs
- **THEN** isolated MySQL and HTTP facts prove the completion contract

### Requirement: C16 preserves milestone boundaries

C16 MUST NOT add QR codes, authentication, authorization, Gateway, payment, refunds, reviews,
Redis, Elasticsearch, Nacos/Feign migration, Resource schema/API changes, cross-schema access,
capacity release on completion, production Compose application scheduling, or distributed
transactions.

#### Scenario: C16 scope is inspected

- **WHEN** code, migrations, dependencies, tests, and deployment files are reviewed
- **THEN** changes remain limited to Booking completion, additive lifecycle event handling,
  Notification consumption, and documentation
