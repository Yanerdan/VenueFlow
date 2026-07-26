## Context

The reservation lifecycle is complete, but VenueFlow has no authentication service boundary.
Implementing credentials, token issuance, Gateway validation, persistence, and revocation in one
change would mix several security-critical concerns. C17 establishes the independently executable
Auth Service first, following the proven service-skeleton baseline.

## Goals / Non-Goals

**Goals:**

- Add an executable Auth Service module on the frozen port `8081`.
- Keep default startup secret-free, connection-free, and Docker-free.
- Reuse restricted Actuator exposure and root Maven quality gates.
- Enforce dependency and architecture boundaries with tests.

**Non-Goals:**

- Registration, credentials, password hashing, login/logout, roles, or account lockout.
- Access/refresh tokens, JWT signing/validation, keys, rotation, revocation, or cookies.
- Gateway, User collaboration, persistence, Nacos/Feign, messaging, Redis, or tracing.
- Compose application containers or changes to existing service behavior.

## Decisions

### 1. Add a dedicated MVC service module on port 8081

Create `venueflow-auth-service` with its own Spring Boot entry point and register it before User
in the root reactor. Port `8081` follows the frozen Auth/User/Resource/Booking/Notification/Search
sequence.

Alternative: put login in User Service. Rejected because the full-chain boundary assigns
credentials and tokens to Auth while User owns profile and eligibility facts.

### 2. Keep direct dependencies to Web MVC, Actuator, and tests

C17 needs no security dependency because it defines no authentication behavior yet. The module
will inherit the root BOM, Enforcer, formatting, static analysis, coverage, packaging, and SBOM
gates, with a module whitelist for direct dependencies.

Alternative: add Spring Security and JWT libraries unused. Rejected because unused
auto-configuration and key expectations weaken the proof of a safe skeleton.

### 3. Use an explicit connection-free skeleton profile

Tracked configuration defines application identity, default `skeleton` profile, port, and the
health-only management surface. Datasource and Flyway auto-configuration are excluded
defensively; no credential or key environment variable is introduced.

Alternative: add future database and key placeholders now. Rejected because C17 has no consumer
of those values and committed placeholders can encourage unsafe defaults.

### 4. Verify source, context, HTTP, and packaged-JAR behavior

Tests will prove configuration bounds, absence of infrastructure/security beans, restricted
health endpoints, architecture rules, and real executable JAR startup on an isolated port.

Alternative: context-load verification only. Rejected because it would not prove executable
packaging or actual HTTP exposure.

## Risks / Trade-offs

- [The module provides no login yet] → Keep C17 small and make credential authentication the next
  dedicated change.
- [Skeleton code can drift across modules] → Mirror the existing tested structure without adding
  a premature shared abstraction.
- [Future security dependencies may accidentally activate in skeleton mode] → Retain explicit
  profile and absence tests when authentication is added.

## Migration Plan

1. Register the new module in the root reactor.
2. Add application entry point and bounded configuration.
3. Add dependency enforcement and Docker-free tests.
4. Update documentation and handoff.
5. Run module/root verification, dependency, scope, secret, diff, and strict OpenSpec checks.

Rollback removes the reactor entry and Auth module. No database, API, or external state changes.

## Open Questions

None for the skeleton. Credential schema, password algorithm parameters, token format, key source,
refresh rotation, and revocation are intentionally deferred.
