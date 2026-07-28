# User Profile Management Specification

## Purpose

Define User Service-owned profile persistence, bounded profile APIs, and explicit booking-
eligibility facts without introducing authentication or cross-service behavior.

## Requirements

### Requirement: User Service owns a durable minimal profile schema

User Service SHALL add an immutable V001 Flyway migration that creates only its own
`user_profile` facts. Every profile MUST have an internal identifier, unique immutable
external user identifier, nonblank display name, account status limited to `ACTIVE` or
`SUSPENDED`, booking eligibility limited to `ELIGIBLE` or `INELIGIBLE`, optimistic-lock
version, and audit timestamps. The migration MUST NOT create credentials, tokens, roles,
Booking, Resource, allocation, event, or shared-service tables.

#### Scenario: A fresh User Service schema is migrated

- **GIVEN** an empty User Service MySQL schema with persistence enabled
- **WHEN** Flyway runs before User Service accepts persistence requests
- **THEN** V001 creates `user_profile` with its required constraints and indexes
- **AND** no authentication, Booking, Resource, or shared table exists

#### Scenario: A migration remains immutable

- **WHEN** later User Service schema work is required
- **THEN** V001 is preserved and a new forward migration is used

### Requirement: User Service persistence is explicit and secret-safe

User Service MUST enable datasource, MyBatis-Plus, and Flyway only in its `persistence`
profile. That profile MUST obtain its JDBC URL, username, and password only from
`VENUEFLOW_USER_DB_URL`, `VENUEFLOW_USER_DB_USERNAME`, and
`VENUEFLOW_USER_DB_PASSWORD`; it MUST validate migrations, disable Flyway clean, and disable
automatic schema generation. The default `skeleton` profile MUST remain secret-free and make
no database connection.

#### Scenario: Default User Service startup remains infrastructure-independent

- **GIVEN** Docker and MySQL are unavailable
- **WHEN** User Service starts without an active persistence profile
- **THEN** it starts with `skeleton` and creates no datasource connection

#### Scenario: Persistence startup receives only environment configuration

- **WHEN** a developer starts User Service with the `persistence` profile and required local
  environment variables
- **THEN** Flyway validates the User Service schema before profile APIs are available
- **AND** no credential is read from tracked configuration

### Requirement: User profiles have bounded creation and retrieval APIs

User Service SHALL provide DTO-only `POST /api/v1/users`, `GET /api/v1/users/{userId}`, and
`GET /api/v1/users/{userId}/booking-eligibility` APIs to create a user profile, retrieve one
profile by internal identifier, and retrieve its booking eligibility view. Creation MUST validate
nonblank bounded external identifier and display name, assign `ACTIVE` and `ELIGIBLE` by
default, and reject duplicate external identifiers with a stable conflict. Responses MUST NOT
expose persistence entities, credentials, or internal SQL details.

#### Scenario: A valid profile is created and retrieved

- **WHEN** a caller creates a profile with a new valid external identifier and display name
- **THEN** User Service persists and returns its generated identifier, default status,
  eligibility, version, and timestamps
- **AND** a subsequent retrieval returns the same bounded facts

#### Scenario: A duplicate external identifier is rejected

- **GIVEN** a profile already owns an external identifier
- **WHEN** a caller creates another profile with that identifier
- **THEN** User Service returns a stable conflict and persists no second profile

### Requirement: User profile mutations are versioned and preserve identity

User Service SHALL provide bounded `PATCH /api/v1/users/{userId}/profile`,
`PATCH /api/v1/users/{userId}/account-status`, and
`PATCH /api/v1/users/{userId}/booking-eligibility` APIs for display name, account status,
and booking eligibility. Each mutable operation MUST require the current version,
conditionally update the stored version, and return a stable conflict for a stale version.
The external user identifier and audit history MUST NOT be changed by any update API.

#### Scenario: A current profile update advances the version

- **GIVEN** a profile with version 3
- **WHEN** a caller updates an allowed mutable fact with expected version 3
- **THEN** User Service persists the new fact and returns version 4

#### Scenario: A stale mutation cannot overwrite newer state

- **GIVEN** another successful mutation has advanced a profile version
- **WHEN** a caller submits an update using the earlier version
- **THEN** User Service returns a stable conflict and preserves the newer facts

### Requirement: Booking eligibility is an explicit User Service read fact

User Service SHALL expose a bounded eligibility response for one existing profile. It MUST
report booking as permitted only when account status is `ACTIVE` and booking eligibility is
`ELIGIBLE`; `SUSPENDED` or `INELIGIBLE` profiles MUST be reported as not permitted. The
endpoint MUST only read User Service-owned data and MUST NOT call Resource, Booking, Auth, or
another external service.

#### Scenario: An active eligible user can be reported as bookable

- **GIVEN** a profile with `ACTIVE` status and `ELIGIBLE` booking eligibility
- **WHEN** its eligibility view is requested
- **THEN** the response reports that booking is permitted

#### Scenario: Suspended or ineligible users cannot be reported as bookable

- **GIVEN** a profile is suspended or marked ineligible
- **WHEN** its eligibility view is requested
- **THEN** the response reports that booking is not permitted

### Requirement: Profile failures use safe errors and exclude authentication behavior

User Service MUST return the established `code`, `message`, `details`, `traceId`, and
`timestamp` error envelope for validation failure, missing profile, duplicate external
identifier, and stale-version conflict. It MUST NOT expose SQL, credentials, stack traces,
or persistence entities. This increment SHALL not introduce authentication, passwords,
credentials, JWT, tokens, roles, authorization, Gateway, Nacos, Redis, RabbitMQ, Feign,
messaging, caching, search, Booking behavior, or cross-service database access.

#### Scenario: A rejected profile request leaks no implementation detail

- **WHEN** a caller submits invalid data, requests a missing profile, or uses a stale version
- **THEN** User Service returns a stable safe error envelope
- **AND** no SQL, configuration value, or stack trace is exposed

### Requirement: User profile verification preserves a Docker-free default path

User profile unit and HTTP tests MUST run in default Maven `clean verify` without Docker or
external infrastructure. An explicit `mysql-it` profile SHALL run isolated MySQL integration
tests covering V001, profile persistence, uniqueness, eligibility behavior, and optimistic
concurrency.

#### Scenario: Default verification needs no external service

- **WHEN** CI or a developer runs default User Service or root Maven verification
- **THEN** skeleton and profile tests complete without Docker, MySQL, or another external
  service

#### Scenario: Opt-in MySQL verification proves database facts

- **GIVEN** Docker is available and Maven uses the `mysql-it` profile
- **WHEN** the User Service MySQL suite runs against a fresh database
- **THEN** Flyway, uniqueness, persisted profile state, and stale-version protection are
  verified against MySQL

### Requirement: Current profile resolves from trusted external identity

User Service SHALL expose `GET /api/v1/users/me` and require a bounded `X-User-Id` external
identity header. It MUST validate the header using the existing external identity value object,
query only User-owned data, and return the existing bounded profile DTO or the established
not-found error.

#### Scenario: Authenticated user resolves their profile

- **WHEN** Gateway forwards a valid JWT subject as `X-User-Id`
- **THEN** User Service returns the profile owning that immutable external identity
- **AND** no credential or Auth database access occurs

### Requirement: User profiles support additive campus identity

User Service SHALL add an immutable V002 migration extending `user_profile` with optional unique
campus ID, bounded identity type, department, phone, and email facts. Existing V001 rows MUST
remain readable and MUST default to `OTHER` identity type without fabricating school identifiers.

#### Scenario: An existing profile is migrated

- **WHEN** V002 runs over a schema containing V001 profiles
- **THEN** every existing profile remains readable with unchanged identity and eligibility
- **AND** its identity type is `OTHER`

### Requirement: Campus profile APIs remain DTO-only and versioned

Profile creation SHALL accept campus identity fields, and `PATCH /api/v1/users/me/campus-profile`
SHALL update them using the trusted external identity and expected version. Requests and
responses MUST remain bounded DTOs and stale versions MUST return the established conflict.

#### Scenario: A new profile includes campus facts

- **WHEN** a caller creates a profile with valid optional campus identity fields
- **THEN** User Service returns those fields without exposing persistence details
