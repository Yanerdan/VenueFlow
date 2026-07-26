# Booking Reservation Management Specification

## Purpose

Define Booking-owned reservation persistence, scoped idempotency, bounded collaborator
coordination, cancellation, compensation, and verification boundaries.
## Requirements
### Requirement: Booking Service owns durable scoped idempotency and reservation facts

Booking Service SHALL add an immutable V001 migration creating only Booking-owned idempotency
and reservation facts. Idempotency MUST be uniquely scoped by user ID, operation, and
idempotency key and MUST store a normalized request hash, request ID, status, version, and
timestamps. A reservation MUST store a unique booking number and request ID, user ID, slot ID,
positive quantity, `CONFIRMED` or `CANCELLED` lifecycle, deterministic Resource allocation and
release operation IDs, optimistic version, and audit timestamps.

The migration MUST NOT create User, Resource, capacity, credential, event, or shared tables.

#### Scenario: A clean Booking schema is migrated

- **WHEN** Flyway runs for a fresh Booking Service persistence profile
- **THEN** V001 creates only the required Booking-owned idempotency and reservation facts
- **AND** scoped idempotency, request, booking, and Resource-operation uniqueness are enforced

#### Scenario: Two users use the same key

- **WHEN** two different users create reservations with the same idempotency key
- **THEN** each user has an independent scoped idempotency fact

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

After allocation is proven, the owner SHALL persist a `PENDING_CONFIRMATION` reservation with a
bounded server-owned expiration deadline. Creation MUST NOT append a confirmation Outbox event;
that event belongs only to the later effective confirmation transition.

#### Scenario: Concurrent identical creation has one executor

- **WHEN** the same user submits the same request and key concurrently
- **THEN** one caller obtains execution ownership
- **AND** one durable allocation recovery intent exists before Resource is called
- **AND** User and Resource receive one effective workflow
- **AND** all completed replays identify the same pending reservation and deadline

#### Scenario: A key is reused with another payload

- **WHEN** the same user and operation reuse a key with a different slot or quantity
- **THEN** Booking returns an idempotency conflict
- **AND** Resource is not called again

### Requirement: Booking creation checks owned external facts without shared database access

The sole create executor SHALL perform a bounded User eligibility read and SHALL continue only
when User reports booking permitted. It SHALL ask Resource to allocate the requested quantity
using the reservation's deterministic operation ID. Booking MUST call neither collaborator's
database and MUST distinguish eligibility denial, capacity rejection, collaborator
unavailability, and malformed collaborator responses through safe stable errors.

#### Scenario: An eligible request reserves capacity

- **WHEN** an eligible user requests an available slot with a new scoped key
- **THEN** Resource capacity is allocated
- **AND** Booking persists and returns one `PENDING_CONFIRMATION` reservation with a deadline

#### Scenario: A rejected external fact creates no reservation

- **WHEN** User denies eligibility or Resource rejects capacity
- **THEN** Booking persists no reservation
- **AND** its idempotency result records a safe failed outcome

### Requirement: Ambiguous allocation outcomes are resolved without repeating the write

Booking Service MUST NOT automatically retry Resource allocation or release writes. If an
allocation response is lost or times out, Booking SHALL query Resource by slot ID and operation
ID with a bounded lookup policy. A matching allocation SHALL be treated as success; a
definitive absence SHALL fail without a reservation; an outcome that remains unknown SHALL
return a distinct stable error and MUST NOT issue a second allocation write.

#### Scenario: Allocation succeeds but its response times out

- **WHEN** Resource persists the allocation but Booking receives no allocation response
- **THEN** Booking finds the matching operation through the Resource lookup
- **AND** completes one reservation without allocating again

#### Scenario: Allocation outcome remains unknown

- **WHEN** neither the write response nor bounded operation lookup establishes an outcome
- **THEN** Booking creates no reservation and returns `BOOKING_ALLOCATION_OUTCOME_UNKNOWN`
- **AND** it does not retry the allocation write

### Requirement: Local finalization failure triggers bounded deterministic compensation

Booking SHALL persist the pending reservation, mark idempotency `SUCCEEDED`, and resolve the
allocation recovery intent in one local final transaction after Resource allocation is proven.
The transaction MUST NOT append a confirmation event. If that transaction fails, Booking MUST
make one release attempt using the deterministic release operation ID. Successful or
already-applied release SHALL be treated as compensated and resolve the intent through a local
transaction.

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

Booking Service SHALL expose cancellation for a `PENDING_CONFIRMATION` or `CONFIRMED`
reservation. Before asking Resource to release the stored quantity, it MUST persist a
cancellation recovery intent containing the stored deterministic release operation facts.
Cancellation MUST reject a live timeout lease. After release is proven, Booking SHALL
conditionally perform the matching state to `CANCELLED`, append exactly one cancellation Outbox
event, write one status audit, and resolve the intent in one local transaction.

The local transition MUST condition on status and version. A cancelled replay SHALL return the
same cancelled reservation. A release failure MUST leave the prior state and intent recoverable;
process termination after release MUST be able to complete through reconciliation. An
`EXPIRED` reservation MUST NOT be changed to cancelled.

#### Scenario: Pending cancellation restores capacity once

- **WHEN** a pending reservation is cancelled before expiration owns it
- **THEN** Resource applies one effective release
- **AND** Booking commits one `CANCELLED` transition and event

#### Scenario: Confirmed cancellation is replayed

- **WHEN** a confirmed reservation is cancelled and the request is replayed
- **THEN** Resource applies one effective release
- **AND** Booking remains `CANCELLED`
- **AND** the recovery intent is resolved without another Outbox event

#### Scenario: Release fails during cancellation

- **WHEN** Resource rejects or cannot complete the release
- **THEN** Booking returns a safe downstream error
- **AND** the reservation retains its prior state
- **AND** the durable cancellation intent remains available for reconciliation

#### Scenario: Process stops after release

- **WHEN** Resource has released capacity but Booking has not committed cancellation
- **THEN** reconciliation can prove the release and conditionally complete the local transition

### Requirement: Booking APIs use bounded DTOs and stable envelopes

Booking Service SHALL provide DTO-only create, booking-number retrieval, confirmation, and
cancellation APIs. Controllers MUST NOT accept or return persistence Entities or invoke Mappers.
Successful responses MUST use `code`, `message`, `data`, and `traceId`; failures MUST use only
`code`, `message`, `details`, `traceId`, and `timestamp`. DTO status MUST support
`PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED`, and `EXPIRED`, and pending responses MUST expose
the bounded server-owned expiration time.

Validation, idempotency conflict, in-progress, not found, eligibility, capacity, downstream,
unknown outcome, persistence, compensation, deadline, timeout ownership, and state failures MUST
have distinct stable codes and appropriate non-200 HTTP statuses.

#### Scenario: A caller retrieves a reservation safely

- **WHEN** an existing booking number is requested
- **THEN** Booking returns a bounded reservation DTO in the success envelope
- **AND** no Entity, SQL, collaborator body, or secret is exposed

#### Scenario: A pending reservation is returned

- **WHEN** creation or retrieval returns `PENDING_CONFIRMATION`
- **THEN** the DTO includes its exact server-owned expiration time
- **AND** it exposes no timeout lease or internal retry fact

### Requirement: Reservation verification remains Docker-free by default

Default Maven verification SHALL cover domain, orchestration, concurrency, HTTP, architecture,
configuration, and executable-skeleton behavior without Docker, MySQL, User Service, or
Resource Service. Explicit opt-in verification SHALL use fresh MySQL and HTTP stubs to prove
V001, scoped uniqueness, atomic claim/finalization, collaborator contracts, timeout lookup,
compensation, and cancellation.

#### Scenario: Default verification has no external dependency

- **WHEN** root `clean verify` runs without Docker or collaborator services
- **THEN** all C10 default tests complete without external connections

#### Scenario: Opt-in verification proves real persistence and HTTP coordination

- **WHEN** the explicit C10 integration profiles run
- **THEN** fresh MySQL and bounded HTTP stubs prove the specified data and coordination behavior

### Requirement: C10 preserves service ownership and milestone scope

C10 MUST NOT introduce shared business Entities, cross-service database access, authentication,
authorization, Gateway, Feign, Nacos, Redis, RabbitMQ, Outbox, search, payment, expiry,
check-in/completion, or distributed transactions. Resource remains the capacity source of truth,
User remains the eligibility source of truth, and Booking owns only reservation workflow facts.

#### Scenario: Inspect C10 boundaries

- **WHEN** source, schemas, dependencies, and tests are reviewed
- **THEN** each service accesses only its own schema and DTO contract
- **AND** no deferred infrastructure or workflow is introduced

### Requirement: Effective reservation state changes append one Outbox event atomically

Booking Service SHALL append one immutable Outbox event in the same local transaction that wins
the effective transition to `CONFIRMED`, `CANCELLED`, or `EXPIRED`. Pending reservation creation
MUST append no state event. The event MUST describe the committed state. HTTP replay, a losing
concurrent confirmation/cancellation/expiration, or a failed local transaction MUST NOT append
another effective business event.

No RabbitMQ call, collaborator HTTP call, confirm wait, retry delay, or other network operation
MAY occur inside the reservation transaction. Failure to append the required Outbox event MUST
roll back the associated Booking transaction and follow the established compensation/state
handling.

#### Scenario: Reservation confirmation commits

- **WHEN** Booking atomically transitions a pending reservation to confirmed
- **THEN** the same transaction persists one confirmation Outbox event
- **AND** idempotent confirmation replay inserts no additional event

#### Scenario: Reservation finalization rolls back

- **WHEN** reservation transition or Outbox insertion fails
- **THEN** none of those local facts commit
- **AND** the established deterministic recovery path remains available

#### Scenario: Cancellation transition wins

- **WHEN** one caller wins the conditional transition to `CANCELLED`
- **THEN** the same transaction persists one cancellation Outbox event
- **AND** concurrent or replayed cancellation inserts no additional event

#### Scenario: Expiration transition wins

- **WHEN** one timeout worker wins the conditional transition to `EXPIRED`
- **THEN** the same transaction persists one expiration Outbox event
- **AND** a losing confirmation, cancellation, or timeout worker inserts no event
