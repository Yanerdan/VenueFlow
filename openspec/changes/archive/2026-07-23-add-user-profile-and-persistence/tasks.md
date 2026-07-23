## 1. Persistence boundary and configuration

- [x] 1.1 Inspect the Resource Service persistence module, V001 migration, MySQL suite, and
  error-envelope conventions; record only the reusable engineering pattern for User Service.
- [x] 1.2 Import the internal dependency BOM into User Service and add only validation,
  MyBatis-Plus, Flyway, Flyway MySQL, MySQL runtime driver, and test-only MySQL verification
  dependencies required by this Change.
- [x] 1.3 Revise User Service dependency-boundary enforcement to permit the declared
  User-owned persistence dependencies while continuing to reject security, Nacos, Redis,
  messaging, Feign, tracing, search, and other business-service clients.
- [x] 1.4 Add a `persistence` profile that reads only `VENUEFLOW_USER_DB_URL`,
  `VENUEFLOW_USER_DB_USERNAME`, and `VENUEFLOW_USER_DB_PASSWORD`; enable validated Flyway
  migration, disable clean and automatic DDL, and configure User Service mapper discovery.
- [x] 1.5 Preserve the default `skeleton` profile and update configuration-boundary tests to
  prove it contains no datasource or secret while persistence credentials are not tracked.
- [x] 1.6 Add an opt-in `mysql-it` Failsafe profile that selects only User Service MySQL suite
  classes and leaves default `clean verify` Docker-free.

## 2. User-owned schema and persistence model

- [x] 2.1 Add immutable `V001__init_user_profile.sql` with `user_profile`, required check or
  enum constraints, unique external-user identifier, optimistic version, timestamps, and
  query indexes; do not edit the migration after it is used.
- [x] 2.2 Define User Service domain types for account status, booking eligibility, profile
  identity, and safe eligibility evaluation without shared Resource or Booking classes.
- [x] 2.3 Implement User Service persistence entity and mapper/repository boundary for
  `user_profile`, including conditional versioned updates and duplicate-key translation.
- [x] 2.4 Add focused persistence tests for mapping, unique external identity, permitted
  status values, and zero-row stale-version outcomes without requiring Docker.

## 3. Profile application and HTTP APIs

- [x] 3.1 Define validated request and response DTOs for profile creation, retrieval, display
  name update, account-status update, booking-eligibility update, and eligibility view.
- [x] 3.2 Implement application services for creating and retrieving profiles; assign
  `ACTIVE` and `ELIGIBLE` defaults and keep the external user identifier immutable.
- [x] 3.3 Implement conditional display-name, account-status, and booking-eligibility
  updates that require expected version and advance version exactly once.
- [x] 3.4 Implement eligibility evaluation so booking is permitted only for `ACTIVE` plus
  `ELIGIBLE`, without any Resource, Booking, Auth, or external-service call.
- [x] 3.5 Add `POST /api/v1/users`, `GET /api/v1/users/{userId}`, and
  `GET /api/v1/users/{userId}/booking-eligibility` controller mappings with DTO-only
  responses.
- [x] 3.6 Add the three versioned `PATCH` mappings for profile, account status, and booking
  eligibility; ensure immutable identifier and persistence entity are never exposed.
- [x] 3.7 Add or extend global exception handling to return the established safe envelope for
  invalid input, missing profile, duplicate identifier, stale version, and persistence
  failures without SQL, secret, or stack-trace leakage.

## 4. Docker-free and MySQL verification

- [x] 4.1 Add default-profile unit and HTTP tests for validation, default profile creation,
  profile retrieval, eligibility evaluation, optimistic conflicts, and sensitive error
  boundaries without Docker.
- [x] 4.2 Retain and rerun the C07 executable-JAR, health allowlist, and `SERVER_PORT`
  override tests to prove the skeleton contract remains intact.
- [x] 4.3 Add a `UserProfileMysqlSuite` that uses isolated MySQL to verify V001, profile
  persistence, unique external identity, state changes, eligibility read behavior, and stale
  version protection.
- [x] 4.4 Verify the MySQL suite leaves no reliance on a developer's local `.env`, MySQL
  schema, Docker Compose state, or pre-existing User Service data.

## 5. Documentation and acceptance

- [x] 5.1 Update User Service developer documentation with skeleton versus persistence
  startup, required local-only User DB variables, profile API examples, MySQL verification,
  and the explicit absence of authentication.
- [x] 5.2 Update `.env.example` and relevant runbook material with User Service database
  placeholders only; do not add real credentials or a tracked `.env`.
- [x] 5.3 Run User Service module verification and root `mvn clean verify` with Docker
  unavailable; record the default Docker-free result.
- [x] 5.4 Run `-pl venueflow-user-service -Pmysql-it verify` with Docker available and review
  Flyway, persistence, uniqueness, and stale-write results.
- [x] 5.5 Review the implementation and resolved dependency tree against both C08 specs;
  confirm no authentication, shared entity/table, cross-service database access, client,
  caching, messaging, or Booking behavior was introduced.
