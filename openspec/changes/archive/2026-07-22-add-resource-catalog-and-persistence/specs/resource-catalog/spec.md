## ADDED Requirements

### Requirement: Resource Service owns an isolated resource catalog schema

When its persistence profile is enabled, Resource Service SHALL be the sole business owner of the `venueflow_resource` schema and SHALL use its own least-privilege database account. It MUST NOT read from or write to another service's business schema.

The service SHALL establish `resource_category` and `resource` through an immutable Flyway migration named `V001__init_resource_catalog.sql`. The migration MUST create a unique `resource_no`, a Category-to-Resource referential constraint, positive resource capacity enforcement, a status representation limited to `DRAFT`, `ACTIVE`, `SUSPENDED`, and `ARCHIVED`, a `version` field for optimistic locking, and creation/update timestamps. It MUST NOT create Slot, allocation, capacity ledger, or Booking tables.

#### Scenario: A clean resource schema is migrated

- **GIVEN** an empty `venueflow_resource` schema and the Resource Service persistence profile
- **WHEN** Flyway runs before the service accepts requests
- **THEN** it records and applies `V001__init_resource_catalog.sql`
- **AND** the Category and Resource tables, constraints, indexes, version, and timestamps are available
- **AND** no Slot, allocation, or Booking table is created

#### Scenario: A released migration is not rewritten

- **GIVEN** `V001__init_resource_catalog.sql` has been applied in a shared environment
- **WHEN** a later catalog schema change is needed
- **THEN** the change is delivered as a new versioned Flyway migration
- **AND** the released V001 file remains unchanged

### Requirement: Persistent configuration is explicit and credential-safe

The default `skeleton` profile SHALL remain runnable without MySQL, Docker, or external infrastructure. Resource catalog persistence SHALL activate only when an explicit persistence profile is selected. That profile MUST obtain JDBC URL, username, and password from environment variables or an untracked local environment file; tracked configuration MUST NOT contain a usable database password.

The persistence profile MUST enable Flyway before catalog access and MUST disable Hibernate schema creation or update. A missing required persistence setting MUST fail startup clearly rather than silently using an in-memory or fake catalog.

#### Scenario: Default skeleton startup remains infrastructure-independent

- **GIVEN** no MySQL server and no persistence environment variables are available
- **WHEN** the Resource Service starts with its default configuration
- **THEN** its health endpoints remain available
- **AND** no database connection is attempted

#### Scenario: Persistence startup requires explicit database settings

- **GIVEN** a developer explicitly selects the persistence profile
- **WHEN** a required JDBC setting is absent or invalid
- **THEN** startup fails with a configuration or connection error
- **AND** the service does not substitute an in-memory catalog

### Requirement: Resource catalog write APIs validate and preserve facts

Resource Service SHALL provide `POST /api/v1/resource-categories` to create a Category and `POST /api/v1/resources` to create a Resource. A resource creation request MUST include a nonblank caller-supplied resource number, name, existing category identifier, and positive capacity; description and location may be optional. A successfully created Resource MUST start as `DRAFT` and expose its identifier, category, resource number, name, description, location, capacity, status, version, and timestamps through response DTOs.

The HTTP controller MUST use request/response DTOs and MUST NOT expose a persistence Entity or invoke a Mapper directly. Duplicate resource numbers MUST be rejected with a stable conflict error. An unknown category, malformed request, or invalid capacity MUST be rejected with a stable client-visible error that does not reveal SQL or internal stack traces.

#### Scenario: A valid category and resource are created

- **GIVEN** the persistence profile has migrated an empty resource schema
- **WHEN** a client creates a Category and then submits a valid Resource referencing it
- **THEN** the Category and Resource are persisted by Resource Service
- **AND** the Resource response has status `DRAFT`, an initial version, and timestamps
- **AND** the response is a DTO rather than a database Entity

#### Scenario: A duplicate resource number is rejected

- **GIVEN** a Resource already owns resource number `ROOM-A-101`
- **WHEN** a client creates another Resource with `ROOM-A-101`
- **THEN** the API returns a stable conflict response
- **AND** the existing Resource is unchanged

### Requirement: Resource catalog reads are bounded and service-owned

Resource Service SHALL provide `GET /api/v1/resource-categories`, `GET /api/v1/resources/{resourceId}`, and `GET /api/v1/resources`. The resource list MAY filter by `categoryId` and `status`; it MUST paginate deterministically, default to 20 items, and reject or cap a requested page size greater than 100. Detail and list responses MUST contain only Resource Service-owned catalog facts and response DTO fields.

An unknown resource identifier MUST return a stable not-found response. Pagination, filtering, and response shaping MUST be implemented inside Resource Service and MUST NOT depend on another service's database or API.

#### Scenario: A client retrieves a bounded filtered page

- **GIVEN** Resources of more than one category and status exist
- **WHEN** a client requests a page filtered by one category and status without a page size
- **THEN** the response contains only matching resources
- **AND** the response reports a page size of 20
- **AND** it contains no more than 20 items

#### Scenario: An oversized page is not accepted as unbounded

- **WHEN** a client requests `GET /api/v1/resources` with a page size greater than 100
- **THEN** the service rejects the request or applies the documented maximum of 100
- **AND** it never performs an unbounded catalog query

### Requirement: Resource status transitions are explicit and optimistic

Resource Service SHALL provide `PATCH /api/v1/resources/{resourceId}/status` with a target status and `expectedVersion`. It MUST allow only `DRAFT -> ACTIVE | ARCHIVED`, `ACTIVE -> SUSPENDED | ARCHIVED`, and `SUSPENDED -> ACTIVE | ARCHIVED`; `ARCHIVED` is terminal. The update MUST use the expected version as an optimistic-lock condition and advance the stored version after a successful transition.

An illegal transition, absent Resource, or stale expected version MUST produce a stable client-visible error and MUST NOT change the stored Resource. This requirement governs directory lifecycle only; it MUST NOT allocate capacity, create slots, or inspect Booking data.

#### Scenario: A resource becomes active with the current version

- **GIVEN** a `DRAFT` Resource at version 1
- **WHEN** a client requests status `ACTIVE` with expected version 1
- **THEN** the Resource becomes `ACTIVE`
- **AND** its version advances

#### Scenario: A stale status update does not overwrite newer data

- **GIVEN** a Resource version has advanced from 1 to 2
- **WHEN** a client requests a status transition with expected version 1
- **THEN** the service returns a stable version-conflict response
- **AND** the Resource remains at version 2

### Requirement: Catalog errors use a stable safe envelope

All catalog validation, duplicate, not-found, illegal-transition, and version-conflict failures SHALL use a JSON error envelope containing `code`, `message`, `details`, `traceId`, and `timestamp`. `code` MUST be stable enough for a caller to distinguish validation, duplicate resource number, category not found, resource not found, invalid state transition, and optimistic-lock conflict.

The envelope MUST NOT include SQL statements, Java stack traces, JDBC URLs, usernames, passwords, or other secrets.

#### Scenario: An invalid resource request is safely reported

- **WHEN** a client sends a Resource creation request with a blank resource number or non-positive capacity
- **THEN** the service returns a validation error envelope with the required fields
- **AND** no implementation detail or secret appears in the response

### Requirement: Real MySQL verification is opt-in and isolated

The repository SHALL provide an explicit `mysql-it` Maven profile that runs Flyway and API integration verification against a fresh, isolated real MySQL instance. The profile MAY require Docker, but default `mvn clean verify` MUST remain Docker-free and MUST NOT run that integration suite.

The opt-in integration suite MUST prove that V001 migrates an empty schema and that Category/Resource creation, resource-number uniqueness, category referential integrity, catalog retrieval, and optimistic status transition behavior work against MySQL.

#### Scenario: Default verification does not require Docker

- **GIVEN** a developer has no Docker daemon
- **WHEN** the repository root runs `mvn clean verify`
- **THEN** the default verification completes without attempting the MySQL integration suite

#### Scenario: Opt-in MySQL integration exercises the real migration

- **GIVEN** Docker is available and a developer runs Maven with profile `mysql-it`
- **WHEN** the integration suite starts its isolated MySQL instance
- **THEN** Flyway migrates a clean schema before API assertions run
- **AND** the suite verifies the catalog persistence and constraint behavior against MySQL

### Requirement: C04 remains limited to resource catalog facts

C04 SHALL not introduce ResourceSlot, capacity allocation or release, Booking, approval, authentication, authorization, Nacos, Redis, RabbitMQ, Feign, Sentinel, JWT/security, events, search, or service-container orchestration. Resource `capacity` is a static catalog attribute in this Change and MUST NOT be treated as currently available capacity.

#### Scenario: Catalog work does not create a reservation dependency

- **WHEN** C04 is implemented and its dependencies, schema, and APIs are inspected
- **THEN** no Booking or Slot data model, endpoint, client, or messaging dependency is present
- **AND** Resource Service still owns only the Resource catalog facts defined by this Change
