## Context

Resource Service now owns Resource, ResourceSlot, and capacity-allocation facts; User
Service is currently an executable but deliberately empty skeleton. A later Booking Service
must determine whether a user can book without taking ownership of user data or reading the
User Service database. This change establishes that User Service-owned source of truth while
keeping authentication and distributed service integration deferred.

## Goals / Non-Goals

**Goals:**

- Add a User Service-owned MySQL schema, immutable Flyway migration, and explicit
  `persistence` profile with environment-only connection settings.
- Persist a minimal profile with immutable external user identifier, display name, account
  status, booking eligibility, optimistic version, and audit timestamps.
- Provide bounded DTO-only HTTP APIs for profile creation/retrieval/update and status or
  eligibility changes, plus a read model that later Booking work can consume.
- Preserve Docker-free default verification and add explicit MySQL integration verification.

**Non-Goals:**

- Login, registration credentials, password hashing, JWT, refresh tokens, roles,
  authorization, Gateway, Nacos, Redis, RabbitMQ, Feign, events, caching, search, or a
  Booking Service/client.
- Shared User entities or cross-service database access.
- Bulk administration, user deletion, self-service identity proof, pagination/search UI,
  or production application containers.

## Decisions

### Reuse the proven Resource persistence pattern within the User boundary

User Service will add MyBatis-Plus, Flyway, MySQL driver, and test-only Testcontainers using
the same dependency-management and `mysql-it` profile conventions already exercised by
Resource Service. Its migration directory, mapper packages, entities, tests, and schema are
User Service-owned. Reusing these engineering mechanisms reduces compatibility risk; sharing
domain classes or tables would break ownership and is rejected.

### Keep skeleton and persistence profiles explicitly separate

`skeleton` remains the default, contains no datasource, and must start without Docker. A
separate `persistence` profile reads `VENUEFLOW_USER_DB_URL`,
`VENUEFLOW_USER_DB_USERNAME`, and `VENUEFLOW_USER_DB_PASSWORD` from the local environment,
enables Flyway validation/migration, and disables automatic schema generation. This is chosen
over making persistence the default so default `clean verify` and standalone startup retain
their existing deterministic behavior.

### Model future booking eligibility as a User-owned explicit fact

`user_profile` will contain an immutable, unique external user identifier; display name;
`ACTIVE`/`SUSPENDED` account status; `ELIGIBLE`/`INELIGIBLE` booking eligibility; version; and
timestamps. A future Booking caller can read a bounded eligibility response, where booking is
permitted only when both status is `ACTIVE` and eligibility is `ELIGIBLE`. This avoids
deriving eligibility from authentication or Resource data while keeping violations and role
models out of scope.

### Use DTO boundaries and optimistic writes from the first mutable state

Creation accepts a validated external identifier and display name. Retrieval returns response
DTOs only. Mutable profile, account-status, and eligibility operations require the stored
version and execute conditionally, returning a stable conflict when stale. The immutable
external identifier is never patched. This preserves safe future administrative behavior
without adding security policy before Auth exists.

### Verify MySQL only by opt-in profile

Default tests prove configuration, validation, HTTP mapping, and no-infrastructure skeleton
startup. `mysql-it` starts isolated MySQL for migration, uniqueness, persistence, and
optimistic-concurrency assertions. This retains an offline/Docker-free default Maven path
while providing database evidence where it matters.

## Risks / Trade-offs

- [Unauthenticated development APIs could be mistaken for production administration] → Document
  that they are temporary bounded service APIs and defer access control to a separate Auth and
  Gateway change; do not imply public deployment readiness.
- [Profile fields may evolve once real identity requirements are known] → Keep the first schema
  intentionally small, immutable migrations only, and defer credentials, roles, and legal
  profile attributes.
- [Testcontainers needs Docker for MySQL evidence] → Keep it behind `mysql-it`; default
  `clean verify` remains independent of Docker.
- [Future Booking needs a remote contract] → Expose only stable User Service DTO semantics now;
  defer Feign/contracts until Booking owns a real caller and timeout/idempotency requirements.

## Migration Plan

1. Add User Service persistence dependencies and an explicit profile without changing the
   default skeleton profile.
2. Add V001 for the User Service schema; Flyway validates it before persistence APIs start.
3. Supply local-only User DB environment variables and run the opt-in MySQL suite against a
   fresh schema.
4. Roll back the application by returning to the skeleton profile. Do not edit or remove an
   applied Flyway migration; later schema changes require new forward migrations.

## Open Questions

- Auth will later decide credential ownership and how it maps an authenticated principal to
  the immutable external user identifier.
- Booking will later decide the service-call contract, timeout budget, and behavior when User
  Service is unavailable; this change introduces no client or fallback.
