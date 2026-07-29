## Purpose

Define Resource Service-owned, time-bounded ResourceSlot facts that establish the
availability foundation for a later Booking capability without creating reservations or
capacity allocation behavior.
## Requirements
### Requirement: Resource Service persists an isolated resource-slot model

When its explicit persistence profile is enabled, Resource Service SHALL own
`resource_slot` in the `venueflow_resource` schema. It MUST introduce the table only
through an immutable `V002__add_resource_slots.sql` Flyway migration; V001 MUST NOT be
rewritten. Each slot MUST reference a Resource, store a UTC start and end timestamp,
store a status limited to `OPEN` or `CLOSED`, include an optimistic-lock version and
creation/update timestamps, and be queryable efficiently by resource and time range.
The model MUST NOT create Booking, allocation, capacity-ledger, or other service-owned
tables.

#### Scenario: A clean catalog schema receives the slot migration

- **GIVEN** an empty `venueflow_resource` schema and the persistence profile
- **WHEN** Flyway runs before Resource Service accepts requests
- **THEN** it applies V001 followed by `V002__add_resource_slots.sql`
- **AND** `resource_slot` has the required resource reference, temporal facts, status, version, timestamps, and indexes
- **AND** no Booking, allocation, or capacity-ledger table is created

#### Scenario: An earlier migration is preserved

- **GIVEN** V001 has already been applied in a shared environment
- **WHEN** the slot model is released
- **THEN** Flyway applies V002 as a new versioned migration
- **AND** V001 remains unchanged

### Requirement: Slot creation is valid, resource-owned, and conflict-safe

Resource Service SHALL provide `POST /api/v1/resources/{resourceId}/slots`. The request
MUST contain ISO-8601 offset date-times `startAt` and `endAt`; the service MUST normalize
them to UTC and require `endAt` to be strictly after `startAt`. A new slot MUST belong to
an existing `ACTIVE` Resource and start in `OPEN` status.

The service MUST reject any slot whose half-open interval `[startAt, endAt)` overlaps an
existing slot of the same Resource, including concurrent create attempts. Slots belonging
to different Resources MAY occupy the same time range. Controllers MUST use request and
response DTOs and MUST NOT expose persistence Entities or invoke Mappers directly.

#### Scenario: A valid slot is created for an active resource

- **GIVEN** an ACTIVE Resource with no overlapping slot
- **WHEN** a client posts a valid start and end time for that Resource
- **THEN** Resource Service persists one `OPEN` slot normalized to UTC
- **AND** returns a response DTO containing its id, resource id, time range, status, version, and timestamps

#### Scenario: An invalid interval is rejected

- **GIVEN** an existing ACTIVE Resource
- **WHEN** a client posts an end time equal to or before the start time
- **THEN** Resource Service returns a stable validation error
- **AND** no slot is stored

#### Scenario: An overlapping slot is rejected

- **GIVEN** a Resource already has a slot from 10:00 UTC to 11:00 UTC
- **WHEN** a client creates a slot for that Resource that intersects that interval
- **THEN** Resource Service returns a stable slot-conflict error
- **AND** it stores no second overlapping slot

#### Scenario: A boundary-adjacent slot is accepted

- **GIVEN** a Resource already has a slot from 10:00 UTC to 11:00 UTC
- **WHEN** a client creates a slot from 11:00 UTC to 12:00 UTC for that Resource
- **THEN** Resource Service persists the new slot

### Requirement: Slot reads are time-bounded and deterministic

Resource Service SHALL provide `GET /api/v1/resource-slots/{slotId}` and `GET
/api/v1/resources/{resourceId}/slots`. A list request MUST require a valid time window,
return only slots that intersect that half-open window, order results by `startAt` then
identifier, default to 20 items, and reject or cap a requested size greater than 100.
It MUST return only Resource-Service-owned fields through response DTOs.

#### Scenario: A caller retrieves a slot by identifier

- **GIVEN** a persisted resource slot
- **WHEN** a client requests its identifier
- **THEN** Resource Service returns the slot response DTO

#### Scenario: A caller retrieves a bounded resource time window

- **GIVEN** one Resource has slots both inside and outside a requested UTC time window
- **WHEN** a client requests that resource's slots with no size
- **THEN** the response contains only intersecting slots in `startAt`, identifier order
- **AND** it reports size 20 and contains at most 20 items

#### Scenario: An unbounded or oversized list is not performed

- **WHEN** a client omits the required time window or requests a page size greater than 100
- **THEN** Resource Service rejects the request or applies the documented maximum
- **AND** it never performs an unbounded slot query

### Requirement: Slot availability transitions are explicit and optimistic

Resource Service SHALL provide `PATCH /api/v1/resource-slots/{slotId}/status` with a
target status and `expectedVersion`. It MUST allow only `OPEN -> CLOSED` and `CLOSED ->
OPEN`, condition the update on the stored version, and advance that version on success.
The transition MUST NOT create a Booking, allocate or release capacity, change the slot's
time range, or mutate the parent Resource.

#### Scenario: An open slot is closed with its current version

- **GIVEN** an OPEN slot at version 1
- **WHEN** a client requests CLOSED with expected version 1
- **THEN** the slot becomes CLOSED and its version advances

#### Scenario: A stale availability update is rejected

- **GIVEN** a slot has advanced from version 1 to 2
- **WHEN** a transition uses expected version 1
- **THEN** Resource Service returns a stable version-conflict error
- **AND** the slot remains unchanged

### Requirement: Slot failures use the established safe error envelope

Resource Service MUST report slot validation, Resource/slot-not-found,
inactive-resource, overlap-conflict, illegal status-transition, and optimistic-lock
failures as JSON containing only `code`, `message`, `details`, `traceId`, and `timestamp`.
Stable codes MUST distinguish validation, missing Resource, missing slot, inactive
Resource, time overlap, invalid state transition, and optimistic-lock conflict. The
envelope MUST NOT include SQL, Java stack traces, JDBC URLs, usernames, passwords, or
other secrets.

#### Scenario: A slot request fails without implementation leakage

- **WHEN** a client creates an overlapping slot or references an unknown Resource
- **THEN** Resource Service returns the required safe error envelope with the matching stable code
- **AND** the response includes no SQL, stack trace, connection information, or secret

### Requirement: Slot verification preserves the existing opt-in MySQL boundary

Slot unit and web-layer tests SHALL run in the default Docker-free Maven `clean verify`
path. The existing opt-in `mysql-it` profile SHALL extend its isolated MySQL verification
to prove V002 migration, slot creation, temporal validation, same-resource overlap
rejection, bounded retrieval, and optimistic availability transitions. Default verification
MUST NOT require Docker or execute the MySQL integration suite.

#### Scenario: Default verification remains infrastructure-independent

- **GIVEN** no Docker daemon and no MySQL server
- **WHEN** the repository root runs `mvn clean verify`
- **THEN** slot unit and web-layer tests complete without attempting MySQL integration tests

#### Scenario: Opt-in verification exercises V002 against real MySQL

- **GIVEN** Docker is available and Maven uses profile `mysql-it`
- **WHEN** the integration suite starts an isolated MySQL instance
- **THEN** Flyway applies V001 and V002 before slot API assertions
- **AND** the suite verifies slot persistence, constraints, retrieval, and optimistic transitions against MySQL

### Requirement: This increment remains outside the booking domain

This increment SHALL not introduce Booking, reservation approval, capacity allocation or
release, available-capacity calculation, occupancy counters, cancellation, check-in,
payment, notifications, authentication, authorization, Nacos, Redis, RabbitMQ, Feign,
Sentinel, JWT/security, events, search, or service-container orchestration. Resource
capacity remains a static catalog attribute and slot `OPEN` status MUST NOT be represented
as a current capacity guarantee.

#### Scenario: Slot work creates no reservation dependency

- **WHEN** this increment's implementation, dependencies, schema, and APIs are inspected
- **THEN** Resource Service contains only resource catalog and slot-management behavior
- **AND** no Booking or allocation model, endpoint, client, or messaging dependency exists

### Requirement: Slot detail carries resource booking rules

The Resource service SHALL include the owning resource's booking notice and time limits in the slot-detail collaboration response.

#### Scenario: Booking service retrieves a slot

- **WHEN** the Booking service retrieves an existing slot
- **THEN** the response includes the slot interval, approval policy, and current resource booking rules

### Requirement: Management workspace publishes bounded recurring slots

The management workspace SHALL let an authorized operator derive a bounded number of same-time
opening slots on consecutive weeks and SHALL use the existing conflict-safe slot creation operation
for every occurrence.

#### Scenario: Weekly slots are published

- **WHEN** an operator chooses a valid first interval and a recurrence count within the configured bound
- **THEN** the workspace creates each weekly occurrence and reports the number successfully published

#### Scenario: A generated occurrence conflicts

- **WHEN** one recurring occurrence is rejected by Resource Service
- **THEN** the workspace stops further creation, preserves prior successful occurrences, and identifies the failed occurrence

### Requirement: Browser bulk transitions remain bounded and optimistic
The management browser SHALL orchestrate bulk availability changes only over a selected resource's bounded loaded slot page and SHALL call the existing versioned status transition once per eligible slot.

#### Scenario: Loaded page is bulk transitioned
- **WHEN** an authorized operator confirms a target status
- **THEN** only loaded slots not already in that status are submitted with their individual expected versions

#### Scenario: One optimistic transition conflicts
- **WHEN** Resource Service rejects one submitted slot version
- **THEN** the browser stops the remaining sequence and reloads the selected resource's slot page
