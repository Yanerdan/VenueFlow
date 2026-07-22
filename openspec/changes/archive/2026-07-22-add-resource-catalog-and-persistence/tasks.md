## 1. Persistence profile and build boundary

- [x] 1.1 Inspect the current Resource Service POM and keep the C03 skeleton dependencies intact; add only Bean Validation, MyBatis-Plus, Flyway, MySQL Connector/J, and test dependencies needed by C04.
- [x] 1.2 Add a secret-free default `skeleton` configuration that still starts Resource Service on `SERVER_PORT` (default `8083`) without attempting a database connection.
- [x] 1.3 Add an explicit `persistence` profile whose JDBC URL, username, and password come only from documented environment variables or an untracked local `.env` file; make missing settings fail clearly.
- [x] 1.4 Configure Flyway for the persistence profile and disable Hibernate schema creation/update so application startup validates or uses only Flyway-managed schema.
- [x] 1.5 Document the required local environment-variable names and both startup commands without committing a usable database credential.

## 2. Resource schema and migration

- [x] 2.1 Create immutable `V001__init_resource_catalog.sql` for the Resource Service-owned `venueflow_resource` schema.
- [x] 2.2 In V001, create `resource_category` with its primary key, name/code constraints, and audit timestamps required by the catalog API.
- [x] 2.3 In V001, create `resource` with `resource_no`, `category_id`, name, description, location, positive `capacity`, constrained status, `version`, timestamps, a unique resource-number index, and a Category foreign key.
- [x] 2.4 Review the migration against an empty MySQL schema; verify it creates no Slot, allocation, capacity-ledger, or Booking table, and do not edit V001 after it has been exercised.

## 3. Catalog persistence and application layer

- [x] 3.1 Add Resource Service-local persistence Entities and MyBatis-Plus Mappers for Category and Resource; do not move these types into `venueflow-common`.
- [x] 3.2 Implement a catalog application service that creates and lists Categories and translates duplicate-category failures into stable domain errors.
- [x] 3.3 Implement Resource creation in the application service: validate Category existence, enforce a unique caller-supplied `resourceNo`, persist `DRAFT` as the initial status, and map persistence failures safely.
- [x] 3.4 Implement Resource detail and deterministic bounded page queries with optional `categoryId` and `status` filters, default page size 20, and maximum page size 100.
- [x] 3.5 Implement the declared Resource status state machine and an optimistic conditional update using `expectedVersion`; distinguish not-found, invalid-transition, and stale-version outcomes.

## 4. HTTP API and error contract

- [x] 4.1 Add request/response DTOs and Bean Validation for Category creation, Resource creation, page parameters, and status transition; do not bind HTTP requests to Entities.
- [x] 4.2 Add `POST /api/v1/resource-categories` and `GET /api/v1/resource-categories`, delegating only to the catalog application service.
- [x] 4.3 Add `POST /api/v1/resources`, `GET /api/v1/resources/{resourceId}`, and paginated `GET /api/v1/resources` endpoints with documented DTO response shapes.
- [x] 4.4 Add `PATCH /api/v1/resources/{resourceId}/status`, requiring target status and `expectedVersion` and exposing the updated Resource DTO on success.
- [x] 4.5 Add a global catalog error handler returning only `code`, `message`, `details`, `traceId`, and `timestamp`; assign stable codes for validation, duplicate resource number, category/resource not found, invalid transition, and optimistic-lock conflict.

## 5. Automated verification

- [x] 5.1 Add Docker-free unit tests for input validation, Category existence, duplicate resource number handling, the status state machine, and stale-version behavior.
- [x] 5.2 Add Docker-free MVC/API-boundary tests proving the public endpoints use DTOs and return the stable success and error envelope shapes without leaking Entity, SQL, stack traces, or credentials.
- [x] 5.3 Add tests for pagination defaults, maximum page size, and category/status filtering so no unbounded catalog query is exposed.
- [x] 5.4 Add a `mysql-it` Maven profile that runs a separately named real-MySQL integration suite and is excluded from default `mvn clean verify`.
- [x] 5.5 Implement the real-MySQL integration suite against a fresh isolated database (for example Testcontainers): prove V001 migration, Category/Resource creation, unique resource-number enforcement, category referential integrity, retrieval, and optimistic status transition behavior.
- [x] 5.6 Run `mvn clean verify` with Docker unavailable or disabled and confirm the default skeleton verification remains Docker-free.
- [x] 5.7 With Docker available, run the `mysql-it` profile and retain the command/output needed to show that the real MySQL migration and API suite passed.

## 6. Manual acceptance and scope review

- [x] 6.1 Start Resource Service in default `skeleton` mode and verify `GET /actuator/health` is `UP` on port 8083 without MySQL.
- [x] 6.2 Start it with the explicit persistence profile and a local `venueflow_resource` database; create a Category, create a Resource, retrieve its detail and first page, then transition it from `DRAFT` to `ACTIVE` with the current version.
- [x] 6.3 Manually verify duplicate resource number, unknown Category/Resource, invalid page/capacity, invalid status transition, and stale version responses use the safe stable error envelope.
- [x] 6.4 Review the final dependency tree, schema, and endpoints to confirm C04 did not introduce Slot, capacity allocation, Booking, authentication/authorization, Nacos, Redis, RabbitMQ, Feign, messaging, search, or container orchestration.
