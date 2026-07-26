## MODIFIED Requirements

### Requirement: Creation obtains idempotency execution ownership before collaborator calls

Booking Service SHALL accept creation idempotency only from an `Idempotency-Key` HTTP header,
validate it, normalize the request, and atomically create or load the scoped idempotency fact
before calling User or Resource.

Exactly one concurrent caller MAY own a new `PROCESSING` request. The owner MUST persist a
bounded allocation recovery intent containing the deterministic Resource operation facts before
issuing the allocation call. A matching `SUCCEEDED` replay MUST return the existing reservation,
a matching `PROCESSING` request MUST NOT call collaborators again, and a matching `FAILED`
request MUST return its recorded safe failure. Reuse of the same scoped key with another request
hash MUST return a stable conflict before any collaborator call.

#### Scenario: Concurrent identical creation has one executor

- **WHEN** the same user submits the same request and key concurrently
- **THEN** one caller obtains execution ownership
- **AND** one durable allocation recovery intent exists before Resource is called
- **AND** User and Resource receive one effective workflow
- **AND** all completed replays identify the same reservation

#### Scenario: A key is reused with another payload

- **WHEN** the same user and operation reuse a key with a different slot or quantity
- **THEN** Booking returns an idempotency conflict
- **AND** Resource is not called again

### Requirement: Local finalization failure triggers bounded deterministic compensation

Booking SHALL persist the reservation, mark idempotency `SUCCEEDED`, append its confirmation
Outbox event, and resolve the allocation recovery intent in one local final transaction after
Resource allocation is proven. If that transaction fails, Booking MUST make one release attempt
using the deterministic release operation ID. Successful or already-applied release SHALL be
treated as compensated and resolve the intent through a local transaction.

Failed, ambiguous, or interrupted compensation MUST leave the durable intent available for
reconciliation and return `BOOKING_COMPENSATION_REQUIRED`. Process termination at any point after
intent creation MUST remain recoverable from stored operation facts.

#### Scenario: Booking persistence fails after allocation

- **WHEN** Resource allocation succeeds and the final Booking transaction fails
- **THEN** Booking attempts one deterministic release
- **AND** unresolved compensation remains represented by a durable recovery intent
- **AND** no successful reservation response is returned

#### Scenario: Compensation cannot be confirmed

- **WHEN** the deterministic release fails or remains ambiguous
- **THEN** Booking returns the compensation-required error
- **AND** reconciliation can resume from the stored request and operation correlations
- **AND** no secret or collaborator body is stored

#### Scenario: Process stops after allocation

- **WHEN** the request process ends after intent persistence and before local finalization
- **THEN** the intent remains eligible for bounded reconciliation
- **AND** recovery does not depend on the original process or its logs

### Requirement: Reservation cancellation is state-idempotent and optimistic

Booking Service SHALL expose cancellation for a `CONFIRMED` reservation. Before asking Resource
to release the stored quantity, it MUST persist a cancellation recovery intent containing the
stored deterministic release operation facts. After release is proven, Booking SHALL
conditionally perform `CONFIRMED -> CANCELLED`, append exactly one cancellation Outbox event, and
resolve the intent in one local transaction.

The local transition MUST condition on status and version. A cancelled replay SHALL return the
same cancelled reservation. A release failure MUST leave the reservation confirmed and the
intent recoverable; process termination after release MUST be able to complete through
reconciliation.

#### Scenario: Cancellation restores capacity once

- **WHEN** a confirmed reservation is cancelled and the request is replayed
- **THEN** Resource applies one effective release
- **AND** Booking remains `CANCELLED`
- **AND** the recovery intent is resolved without another Outbox event

#### Scenario: Release fails during cancellation

- **WHEN** Resource rejects or cannot complete the release
- **THEN** Booking returns a safe downstream error
- **AND** the reservation remains `CONFIRMED`
- **AND** the durable cancellation intent remains available for reconciliation

#### Scenario: Process stops after release

- **WHEN** Resource has released capacity but Booking has not committed cancellation
- **THEN** reconciliation can prove the release and conditionally complete the local transition
