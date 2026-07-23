## Purpose

Define the Resource Service-owned Category and Resource catalog, its explicit MySQL
persistence boundary, safe HTTP contract, and opt-in real-MySQL verification.

## Requirements

### Requirement: Resource Service owns an isolated resource catalog schema

When its persistence profile is enabled, Resource Service SHALL be the sole business
owner of the `venueflow_resource` schema and SHALL use its own least-privilege database
account. It MUST NOT read from or write to another service's business schema.

The service SHALL establish `resource_category` and `resource` through an immutable
Flyway migration named `V001__init_resource_catalog.sql`. The migration MUST create a
unique `resource_no`, a Category-to-Resource referential constraint, positive resource
capacity enforcement, statuses limited to `DRAFT`, `ACTIVE`, `SUSPENDED`, and
`ARCHIVED`, an optimistic-lock `version`, and creation/update timestamps. It MUST NOT
create Slot, allocation, capacity-ledger, or Booking tables.

#### Scenario: A clean resource schema is migrated

- **GIVEN** an empty `venueflow_resource` schema and the persistence profile
- **WHEN** Flyway runs before the service accepts requests
- **THEN** it records and applies `V001__init_resource_catalog.sql`
- **AND** the catalog tables, constraints, indexes, version, and timestamps exist
- **AND** no Slot, allocation, or Booking table is created

#### Scenario: A released migration is not rewritten

- **GIVEN** V001 has been applied in a shared environment
- **WHEN** a later catalog schema change is needed
- **THEN** it is delivered as a new versioned migration
- **AND** V001 remains unchanged

### Requirement: Persistent configuration is explicit and credential-safe

The default `skeleton` profile SHALL remain runnable without MySQL, Docker, or external
infrastructure. Catalog persistence SHALL activate only with an explicit `persistence`
profile. That profile MUST obtain JDBC URL, username, and password only from environment
variables or an untracked local environment file; tracked configuration MUST NOT contain
a usable database password.

The persistence profile MUST enable Flyway before catalog access and MUST disable Hibernate
schema creation or update. A missing required persistence setting MUST fail startup clearly
rather than substituting an in-memory or fake catalog.

#### Scenario: Default skeleton startup remains infrastructure-independent

- **GIVEN** no MySQL server and no persistence environment variables
- **WHEN** Resource Service starts with default configuration
- **THEN** health endpoints remain available
- **AND** no database connection is attempted

#### Scenario: Persistence startup requires explicit database settings

- **GIVEN** a developer selects the persistence profile
- **WHEN** a JDBC setting is absent or invalid
- **THEN** startup fails with a configuration or connection error
- **AND** no in-memory catalog is substituted

### Requirement: Resource catalog write APIs validate and preserve facts

Resource Service SHALL provide `POST /api/v1/resource-categories` and
`POST /api/v1/resources`. A resource creation request MUST contain a nonblank
caller-supplied resource number and name, an existing Category identifier, and positive
capacity; description and location may be optional. A created Resource MUST start as
`DRAFT` and expose its catalog fields, version, and timestamps through response DTOs.

Controllers MUST use request/response DTOs and MUST NOT expose persistence Entities or
invoke Mappers directly. Duplicate resource numbers, unknown Categories, malformed
requests, and invalid capacity MUST return stable client-visible errors without SQL or
stack traces.

#### Scenario: A valid category and resource are created

- **GIVEN** an empty migrated catalog schema
- **WHEN** a client creates a Category then a valid Resource referencing it
- **THEN** both facts are persisted by Resource Service
- **AND** the Resource response is a DTO with `DRAFT`, initial version, and timestamps

#### Scenario: A duplicate resource number is rejected

- **GIVEN** a Resource owns `ROOM-A-101`
- **WHEN** another Resource is created with that number
- **THEN** the API returns a stable conflict response
- **AND** the original Resource remains unchanged

### Requirement: Resource catalog reads are bounded and service-owned

Resource Service SHALL provide `GET /api/v1/resource-categories`,
`GET /api/v1/resources/{resourceId}`, and `GET /api/v1/resources`. Resource pages MAY
filter by `categoryId` and `status`; they MUST be deterministic, default to 20 items, and
reject or cap a requested size greater than 100. Responses MUST contain only
Resource-Service-owned catalog facts and DTO fields.

#### Scenario: A client retrieves a bounded filtered page

- **GIVEN** Resources of more than one Category and status
- **WHEN** a page is requested with one Category and status and no size
- **THEN** it contains only matching Resources
- **AND** reports size 20 and contains at most 20 items

#### Scenario: An oversized page is not accepted as unbounded

- **WHEN** a client requests a page size greater than 100
- **THEN** the service rejects it or applies the documented maximum
- **AND** it never performs an unbounded catalog query

### Requirement: Resource status transitions are explicit and optimistic

Resource Service SHALL provide `PATCH /api/v1/resources/{resourceId}/status` with target
status and `expectedVersion`. It MUST allow only `DRAFT -> ACTIVE | ARCHIVED`,
`ACTIVE -> SUSPENDED | ARCHIVED`, and `SUSPENDED -> ACTIVE | ARCHIVED`; `ARCHIVED` is
terminal. The update MUST condition on and advance the stored version.

Illegal transitions, absent Resources, and stale expected versions MUST return stable
client-visible errors and MUST NOT change stored state. This lifecycle MUST NOT allocate
capacity, create slots, or inspect Booking data.

#### Scenario: A resource becomes active with the current version

- **GIVEN** a `DRAFT` Resource at version 1
- **WHEN** a client requests `ACTIVE` with expected version 1
- **THEN** the Resource becomes `ACTIVE` and its version advances

#### Scenario: A stale status update does not overwrite newer data

- **GIVEN** a Resource has advanced from version 1 to 2
- **WHEN** a transition uses expected version 1
- **THEN** a stable version-conflict response is returned
- **AND** the Resource remains at version 2

### Requirement: Catalog errors use a stable safe envelope

The service SHALL return JSON containing only `code`, `message`, `details`, `traceId`, and
`timestamp` for all catalog validation, duplicate, not-found, illegal-transition, and version-conflict
failures. Stable codes MUST distinguish validation, duplicate resource number,
Category/Resource not found, invalid state transition, and optimistic-lock conflict.
The envelope MUST NOT include SQL, Java stack traces, JDBC URLs, usernames, passwords,
or other secrets.

#### Scenario: An invalid resource request is safely reported

- **WHEN** a client sends a blank resource number or non-positive capacity
- **THEN** the service returns the required validation error envelope
- **AND** it contains no implementation detail or secret

### Requirement: Real MySQL verification is opt-in and isolated

The repository SHALL provide an explicit `mysql-it` Maven profile that runs Flyway and API
integration verification against a fresh isolated MySQL instance. The profile MAY require
Docker, but default `mvn clean verify` MUST remain Docker-free and MUST NOT run that suite.

The opt-in suite MUST prove V001 migration, Category/Resource creation, resource-number
uniqueness, Category referential integrity, catalog retrieval, and optimistic status
transitions against MySQL.

#### Scenario: Default verification does not require Docker

- **GIVEN** no Docker daemon
- **WHEN** the repository root runs `mvn clean verify`
- **THEN** verification completes without attempting the MySQL integration suite

#### Scenario: Opt-in MySQL integration exercises the real migration

- **GIVEN** Docker is available and Maven uses profile `mysql-it`
- **WHEN** the suite starts an isolated MySQL instance
- **THEN** Flyway migrates its clean schema before API assertions
- **AND** the suite verifies catalog persistence and constraints against MySQL

### Requirement: C04 remains limited to resource catalog facts

C04 SHALL not introduce ResourceSlot, capacity allocation or release, Booking, approval,
authentication, authorization, Nacos, Redis, RabbitMQ, Feign, Sentinel, JWT/security,
events, search, or service-container orchestration. Resource `capacity` is a static catalog
attribute and MUST NOT be treated as currently available capacity.

#### Scenario: Catalog work does not create a reservation dependency

- **WHEN** C04 implementation, dependencies, schema, and APIs are inspected
- **THEN** no Booking or Slot model, endpoint, client, or messaging dependency exists
- **AND** Resource Service owns only the catalog facts defined here
