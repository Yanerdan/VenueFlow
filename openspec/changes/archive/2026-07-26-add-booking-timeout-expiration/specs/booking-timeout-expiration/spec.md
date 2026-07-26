## ADDED Requirements

### Requirement: Booking persists bounded pending and timeout facts

Booking Service SHALL add an immutable V004 migration that expands its owned reservation
lifecycle with `PENDING_CONFIRMATION` and `EXPIRED`, a non-null deadline for new pending
reservations, bounded terminal reasons, timeout lease/attempt/next-check facts, and status audit.
Existing confirmed reservations MUST remain confirmed and MUST NOT become timeout candidates.
V001-V003 and every other service schema MUST remain unchanged.

#### Scenario: A clean Booking schema receives V004

- **WHEN** Flyway migrates a fresh Booking schema
- **THEN** V001 through V004 apply in order
- **AND** pending deadlines, timeout leases, due-work indexes, lifecycle constraints, and status
  audit facts exist

#### Scenario: Existing data is upgraded

- **WHEN** V004 is applied to reservations created before C15
- **THEN** existing `CONFIRMED` or `CANCELLED` states remain unchanged
- **AND** no historical confirmed reservation is assigned an expiration deadline

### Requirement: Confirmation is deadline-bound and idempotent

Booking SHALL expose an explicit bounded confirmation operation for a pending reservation. It
MUST condition `PENDING_CONFIRMATION -> CONFIRMED` on booking identity, optimistic version,
current time before `expireAt`, and absence of a live timeout lease. The winning transaction MUST
append one confirmation Outbox event and one status audit fact.

A confirmed replay SHALL return the same reservation. A request after the deadline or after
`CANCELLED` or `EXPIRED` MUST perform no state transition, Resource write, or Outbox insertion and
MUST return a stable result or conflict.

#### Scenario: Pending reservation is confirmed before its deadline

- **WHEN** confirmation wins before `expireAt`
- **THEN** Booking atomically commits `CONFIRMED`, one confirmation event, and one status log

#### Scenario: Confirmation is replayed

- **WHEN** confirmation is repeated for an already confirmed reservation
- **THEN** the same confirmed reservation is returned
- **AND** no additional event or status transition is created

#### Scenario: Confirmation arrives after the deadline

- **WHEN** current time is not before the reservation deadline
- **THEN** Booking does not confirm the reservation
- **AND** timeout recovery retains exclusive authority to release and expire it

### Requirement: Timeout workers claim bounded recoverable leases

Only an explicit `persistence,expiration` runtime SHALL scan expired pending reservations. A
worker MUST claim at most the configured batch through state/version compare-and-set, a unique
owner, and an expiring lease in a short local transaction. Resource calls MUST occur after claim
commit, expired leases MUST be reclaimable, and batch, deadline, lease, delay, attempt, backoff,
lookup, and HTTP settings MUST be fail-fast and bounded.

#### Scenario: Two workers scan one expired reservation

- **WHEN** concurrent workers attempt to claim the same version
- **THEN** only one obtains the timeout lease
- **AND** the other performs no Resource release

#### Scenario: A timeout worker stops after claim

- **WHEN** its lease expires without terminal completion
- **THEN** another worker can reclaim the reservation
- **AND** it resumes with the same deterministic release operation

### Requirement: Expiration commits only after capacity release is proven

For a leased expired reservation, Booking SHALL resolve the stored deterministic release
operation through the existing Resource lookup. A matching prior release is proof. A definitive
absence MAY permit one idempotent release call for that run; an ambiguous response MUST be
resolved by lookup.

Only proven release SHALL allow one local transaction to conditionally commit
`PENDING_CONFIRMATION -> EXPIRED`, append one expiration Outbox event, write one status audit,
and clear the lease. Mismatch, outage, unknown outcome, lease loss, or persistence failure MUST
retain bounded retry/issue facts and MUST NOT mark the reservation expired.

#### Scenario: Deadline passes and release succeeds

- **WHEN** timeout processing proves the deterministic release
- **THEN** Booking atomically marks the reservation expired and appends one expiration event

#### Scenario: Release response is lost

- **WHEN** Resource applies release but Booking receives no successful response
- **THEN** Booking proves the same release through operation lookup
- **AND** it emits no distinct release operation

#### Scenario: Resource facts conflict

- **WHEN** the release operation type or quantity conflicts with the reservation
- **THEN** Booking performs no local expiration
- **AND** records a stable bounded issue for later review

### Requirement: Confirmation cancellation and expiration have one winner

Confirmation, cancellation, and expiration MUST use conditional state/version transitions and
the live timeout lease boundary. At most one terminal path SHALL win. The shared deterministic
release operation MUST make pending cancellation and timeout expiration converge on one
effective capacity release, while confirmation performs no Resource write.

#### Scenario: Confirmation races timeout claim

- **WHEN** confirmation and timeout claim compete for one pending reservation
- **THEN** exactly one transition authority wins
- **AND** a confirmed reservation is never released by the losing timeout worker

#### Scenario: Cancellation races expiration

- **WHEN** cancellation and expiration compete after the deadline
- **THEN** one terminal state and one effective release result
- **AND** no duplicate Outbox event is inserted for the losing transition

### Requirement: Expiration execution is guarded observable and isolated

Automatic expiration MUST run only when both the explicit profile and enable flag are selected.
A non-HTTP command MUST default to no action, provide bounded metadata-only `PREVIEW`, and require
a bounded reason plus explicit confirmation for one bounded `RUN`.

Metrics/logs SHALL expose due depth/age, claim, confirmation, expiration, cancellation, retry,
mismatch, lease-reclaimed, release-action outcome, and shutdown without payloads, endpoints, or
secrets. Default root verification MUST create no scheduler, database, collaborator, broker, or
Testcontainers fixture. An explicit `expiration-it` profile SHALL use MySQL 8.4.10 and bounded
HTTP stubs to prove V004, races, lease recovery, ambiguous release, and event atomicity.

#### Scenario: Default verification runs

- **WHEN** root `clean verify` runs without Docker or collaborators
- **THEN** no expiration worker or external connection is created

#### Scenario: Operator omits confirmation

- **WHEN** an expiration run is requested without valid reason and confirmation
- **THEN** it fails before claiming work
- **AND** no Resource call or timeout transition occurs

### Requirement: C15 preserves milestone boundaries

C15 MUST NOT add payment, refunds, delayed-message plugins, Redis, Resource schema/API changes,
cross-schema access, authentication/Gateway, search, check-in/completion, production Compose
application scheduling, distributed transactions, or historical expiration.

#### Scenario: C15 scope is inspected

- **WHEN** code, schemas, dependencies, tests, and deployment files are reviewed
- **THEN** changes remain limited to Booking timeout lifecycle, additive event handling,
  Notification consumption, and documentation
- **AND** Resource and existing migrations remain unchanged
