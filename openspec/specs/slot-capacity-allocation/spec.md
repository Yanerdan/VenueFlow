## Purpose

Define Resource Service-owned, idempotent, and concurrency-safe capacity allocation
facts for ResourceSlots, providing the durable occupancy foundation that a later Booking
capability will consume.

## Requirements

### Requirement: Resource Service persists an auditable slot allocation ledger

Resource Service SHALL introduce capacity allocation only through an immutable V003 Flyway
migration. It MUST persist a ResourceSlot-owned allocation operation with unique operation
id, operation type, positive quantity, request fingerprint, and audit timestamps, and it
MUST maintain a non-negative occupied quantity for each slot. The migration MUST NOT alter
V001 or V002 or create a Booking table.

#### Scenario: A clean schema receives the allocation migration

- **GIVEN** an empty Resource Service schema with persistence enabled
- **WHEN** Flyway migrates the schema
- **THEN** it applies V001, V002, and V003 in order
- **AND** allocation operation and occupied-capacity facts exist with required constraints

### Requirement: Slot allocation is bounded, idempotent, and concurrency-safe

Resource Service SHALL provide `POST /api/v1/resource-slots/{slotId}/allocations` with a
nonblank operation id and positive quantity. It MUST allocate only to an existing `OPEN`
slot and MUST never increase occupied quantity above the parent Resource's static capacity.
The same operation id and request fingerprint MUST return its prior successful result; a
conflicting reuse MUST return a stable conflict. Concurrent allocations MUST NOT oversubscribe
the slot.

#### Scenario: A valid allocation reduces available capacity

- **GIVEN** an OPEN slot whose Resource capacity is 10 and occupied quantity is 3
- **WHEN** a caller allocates quantity 2 with a new operation id
- **THEN** the occupied quantity becomes 5 and available quantity is 5

#### Scenario: Capacity cannot be oversubscribed

- **GIVEN** an OPEN slot with available quantity 1
- **WHEN** a caller requests quantity 2
- **THEN** the request returns a stable insufficient-capacity error
- **AND** no allocation or occupancy change is persisted

#### Scenario: A replay does not allocate twice

- **GIVEN** a successful allocation operation id
- **WHEN** the identical request is replayed
- **THEN** the original allocation result is returned
- **AND** occupied quantity is unchanged

### Requirement: Slot capacity release is idempotent and cannot become negative

Resource Service SHALL provide `POST /api/v1/resource-slots/{slotId}/releases` with a
nonblank operation id and positive quantity. It MUST atomically lower occupied quantity,
record the release operation, and reject a release larger than current occupied quantity.
Replay and conflicting-reuse semantics SHALL match allocation.

#### Scenario: A valid release restores available capacity

- **GIVEN** a slot has occupied quantity 5
- **WHEN** a caller releases quantity 2 with a new operation id
- **THEN** occupied quantity becomes 3 and the release is auditable

#### Scenario: A release cannot make occupancy negative

- **GIVEN** a slot has occupied quantity 1
- **WHEN** a caller releases quantity 2
- **THEN** the request returns a stable conflict
- **AND** no occupancy change is persisted

### Requirement: Capacity state is queryable through bounded Resource Service DTOs

Resource Service SHALL provide `GET /api/v1/resource-slots/{slotId}/capacity` and a
bounded operation query for one slot. Capacity responses MUST expose static capacity,
occupied quantity, available quantity, and slot status through DTOs only. Operation pages
MUST be deterministic, default to 20, and reject or cap a size greater than 100.

#### Scenario: A caller retrieves current capacity facts

- **GIVEN** a persisted slot with allocation operations
- **WHEN** a caller requests its capacity
- **THEN** Resource Service returns consistent static, occupied, and available quantities

### Requirement: Allocation errors remain safe and this increment excludes Booking

Resource Service MUST return the established `code`, `message`, `details`, `traceId`, and
`timestamp` envelope for missing/closed slot, invalid quantity, insufficient capacity,
negative release, stale or conflicting idempotency operation, and persistence failures.
It MUST NOT expose SQL or secrets. This increment SHALL not introduce Booking, end-user
identity, authentication, payment, cancellation workflow, expiry jobs, Redis, messaging,
Feign, Nacos, or distributed transactions.

#### Scenario: A rejected allocation leaks no implementation detail

- **WHEN** a caller exceeds capacity or reuses an operation id with different facts
- **THEN** Resource Service returns a stable safe error envelope
- **AND** no SQL, credentials, or stack trace is exposed

### Requirement: Allocation verification remains Docker-free by default

Allocation unit/web tests SHALL run in default `mvn clean verify`; opt-in `mysql-it`
verification SHALL prove V003, constraints, idempotent replay, capacity boundaries, and
concurrent allocation safety against isolated MySQL. Default verification MUST NOT require
Docker.

#### Scenario: Opt-in MySQL verification exercises capacity integrity

- **GIVEN** Docker is available and Maven uses `mysql-it`
- **WHEN** the allocation suite runs against fresh MySQL
- **THEN** no test can persist occupied quantity beyond static capacity or below zero
