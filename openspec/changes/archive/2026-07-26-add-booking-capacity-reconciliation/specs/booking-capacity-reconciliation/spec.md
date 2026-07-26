## ADDED Requirements

### Requirement: Booking persists durable reconciliation facts

Booking Service SHALL add an immutable V003 migration containing only Booking-owned recovery
intent, reconciliation-run, reconciliation-issue, and repair-action tables. Every intent MUST
store a bounded workflow type, request/booking correlation, slot, quantity, deterministic
allocation and release operation IDs, state, version, lease, attempt, next-check, stable outcome,
and timestamps.

Runs, issues, and actions MUST contain only bounded codes and operational metadata. They MUST
NOT store collaborator bodies, SQL, stack traces, credentials, connection values, or facts owned
by another service. Existing migrations MUST remain unchanged.

#### Scenario: A clean Booking schema receives V003

- **WHEN** Flyway migrates a fresh Booking schema
- **THEN** V001, V002, and V003 apply in order
- **AND** the reconciliation tables and their uniqueness, lease, state, and due-work indexes exist

#### Scenario: Reconciliation facts are inspected

- **WHEN** stored intents, issues, runs, and actions are reviewed
- **THEN** they contain sufficient bounded correlation and outcome facts for recovery
- **AND** no raw HTTP response, credential, stack trace, or cross-service entity is stored

### Requirement: Reconciliation claims bounded work with recoverable leases

Only an explicit `persistence,reconciliation` runtime SHALL scan due intents. A worker MUST claim
at most the configured batch using status/version compare-and-set, a unique owner, and an expiring
lease in a short local transaction. Network calls MUST occur after claim commit. Expired leases
MUST be reclaimable, and all batch, lease, delay, attempt, backoff, and timeout settings MUST be
validated and bounded.

#### Scenario: Two workers scan the same due intent

- **WHEN** concurrent workers attempt to claim one due intent
- **THEN** only one lease owner obtains that version
- **AND** the other worker performs no Resource call for it

#### Scenario: A worker stops after claiming

- **WHEN** its lease expires without a terminal outcome
- **THEN** a later worker can reclaim the intent
- **AND** recovery resumes through the same deterministic operation IDs

### Requirement: Allocation reconciliation releases only proven orphan capacity

For an open allocation intent, Booking SHALL query the existing Resource operation API using the
stored slot and allocation operation ID. A definitive absence MAY resolve the intent without a
repair. A matching allocation with a `CONFIRMED` Booking SHALL resolve as consistent. A matching
allocation without a successful Booking SHALL be released only with the stored deterministic
release operation ID and quantity.

An ambiguous release response MUST be resolved through Resource operation lookup and MUST NOT
cause a different release write. Booking SHALL resolve the intent as repaired only after the
release is proven. Conflicting or unknown facts MUST remain unresolved and create/update a
bounded issue.

#### Scenario: Process stops after Resource allocated capacity

- **WHEN** reconciliation proves the allocation exists but no successful Booking exists
- **THEN** it performs one effective deterministic release
- **AND** records the intent and repair outcome without creating a reservation

#### Scenario: Release response is lost

- **WHEN** Resource applies the repair release but Booking receives an ambiguous response
- **THEN** Booking queries the same release operation
- **AND** resolves the intent without issuing another distinct release

#### Scenario: Allocation facts conflict

- **WHEN** Resource returns an operation whose slot, type, or quantity does not match the intent
- **THEN** Booking performs no repair
- **AND** records a stable unresolved issue for operator review

### Requirement: Cancellation reconciliation completes only a proven workflow

An open cancellation intent SHALL use the deterministic release operation as its proof. If the
release is absent, one bounded run MAY invoke the same idempotent release and MUST resolve an
ambiguous response by lookup. Once release is proven, Booking SHALL conditionally transition only
the matching `CONFIRMED` reservation to `CANCELLED`, append exactly one cancellation Outbox event,
and resolve the intent in one local transaction.

A reservation already `CANCELLED` SHALL resolve idempotently. Any other state, version conflict,
or mismatched fact MUST NOT be overwritten and MUST produce a bounded issue.

#### Scenario: Process stops after cancellation release

- **WHEN** Resource release is proven but the reservation remains `CONFIRMED`
- **THEN** reconciliation atomically cancels it, appends one Outbox event, and resolves the intent

#### Scenario: Cancellation was already completed

- **WHEN** reconciliation observes the reservation and intent are already resolved
- **THEN** it creates no additional release or Outbox event

### Requirement: Operator execution is previewable, confirmed, and non-HTTP

Reconciliation SHALL expose no repair HTTP endpoint. Its operator command MUST default to no
action, provide a bounded metadata-only preview, and require a bounded reason plus explicit
confirmation before a repair run. Preview MUST make no state transition or Resource write. A run
MUST process at most one configured batch through the same leased application service used by
automatic execution.

#### Scenario: Operator previews reconciliation

- **WHEN** the preview command is selected
- **THEN** it reports bounded counts, ages, IDs, workflow types, and stable issue codes
- **AND** performs no claim or repair

#### Scenario: Repair confirmation is absent

- **WHEN** a run is requested without confirmation or a valid bounded reason
- **THEN** the command fails before claiming work
- **AND** no Resource call or persistent repair action occurs

### Requirement: Reconciliation is observable and infrastructure-isolated

Safe metrics/logs SHALL expose run, claim, consistent, repaired, unresolved, failed,
lease-reclaimed, due-depth, and oldest-age outcomes without payloads, collaborator bodies,
credentials, or endpoints. Default root verification MUST create no reconciliation scheduler,
database connection, HTTP collaborator, or Testcontainers fixture.

An explicit `reconciliation-it` profile SHALL use isolated MySQL 8.4.10 and bounded HTTP stubs to
prove V003, intent atomicity, lease competition, crash recovery, orphan release, cancellation
completion, ambiguous-response lookup, and idempotent replay.

#### Scenario: Default verification runs

- **WHEN** root `clean verify` runs without Docker or collaborator services
- **THEN** reconciliation tests use only deterministic ports/fakes
- **AND** no runner or external connection is created

#### Scenario: Opt-in reconciliation evidence runs

- **WHEN** the explicit `reconciliation-it` profile runs
- **THEN** real MySQL and HTTP contracts prove the specified recovery and concurrency behavior

### Requirement: C14 preserves milestone scope

C14 MUST NOT add timeout expiration, new Booking states, Resource schema/API changes, Outbox
publication repair, Notification repair, Resource-wide scanning, cross-schema access, Redis,
RabbitMQ consumers, Gateway/security, distributed transactions, or production Compose
application scheduling.

#### Scenario: C14 scope is inspected

- **WHEN** code, migrations, configuration, tests, and deployment files are reviewed
- **THEN** changes are limited to Booking-owned durable capacity reconciliation and documentation
- **AND** existing service ownership and default infrastructure-free behavior remain intact
