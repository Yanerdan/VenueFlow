## Context

VenueFlow now has independently executable Resource and User services, while booking facts
and workflows remain intentionally unimplemented. The root Maven reactor has no Booking
Service module, so later booking work has no isolated runtime, test, configuration, or
dependency boundary to extend. This change creates that boundary without creating a partial
reservation workflow.

The project baseline requires JDK 21, Maven Wrapper verification, secret-free tracked
configuration, restricted Actuator exposure, and Docker-free default builds. Resource and
User Services demonstrate the established skeleton pattern. Booking Service uses port `8084`
to retain the documented local service-port allocation.

## Goals / Non-Goals

**Goals:**

- Add a reactor-integrated, executable `venueflow-booking-service` Spring Boot MVC module.
- Provide a default `skeleton` profile that starts without a database, Docker, or external
  service and listens on `8084` unless `SERVER_PORT` is supplied.
- Establish only liveness and readiness health probes, a narrow dependency allowlist, and
  executable-JAR plus random-port HTTP verification.
- Provide clear module documentation and a small, separately testable implementation plan.

**Non-Goals:**

- Booking APIs, entities, DTOs, persistence, Flyway migrations, capacity changes, or an
  in-memory substitute for booking state.
- Calls to Resource or User Service, shared entities/tables, Feign, Gateway, service
  discovery, authentication/authorization, Nacos, Redis, RabbitMQ, Kafka, tracing,
  Prometheus, search, application images, or Compose application orchestration.
- Changing existing User or Resource contracts.

## Decisions

### Mirror the proven service-skeleton shape

The module will import the internal dependency BOM and use only Spring Web MVC, Actuator,
and test support. It will have a dedicated `BookingServiceApplication`, `application.yml`,
and `application-skeleton.yml` rather than reusing another service's runtime configuration.
This retains independent artifacts, ports, test reports, and future ownership. Reusing the
Resource Service module or introducing a shared executable base was rejected because it
would blur service ownership before a booking domain exists.

### Make `skeleton` the only runtime profile in this increment

`skeleton` remains the default profile and explicitly excludes datasource, Flyway, and other
external-client auto-configuration. No database properties or placeholders are introduced.
An eventual persistence profile will be proposed separately with migration and MySQL evidence.
Adding persistence now was rejected because it would create booking tables before the booking
state machine and capacity invariants are specified.

### Enforce the boundary in Maven and tests

The module POM will use an explicit dependency allowlist and banned dependency rules.
Configuration-boundary tests will inspect tracked configuration for forbidden imports,
credentials, and infrastructure settings. Context, HTTP, and executable-JAR tests will use
random ports where appropriate. This combination catches both dependency drift and accidental
runtime coupling; documentation-only prohibitions were rejected as insufficiently enforceable.

### Expose only safe operational probes

Actuator Web will expose `health` with liveness and readiness groups while suppressing detail
and discovery. Tests will assert that sensitive endpoints such as `env`, `configprops`,
`loggers`, `mappings`, and `metrics` are absent. Adding Spring Security solely to protect an
otherwise broad Actuator surface was rejected because authentication is not in scope.

## Risks / Trade-offs

- [A future developer may mistake the executable service for a usable booking API] → The
  README, scope spec, dependency rules, and negative tests explicitly state and enforce the
  absence of booking behavior.
- [Port `8084` may already be occupied locally] → Tests use random ports and the packaged
  JAR accepts `SERVER_PORT`.
- [Duplicated skeleton code can drift across services] → Keep the module intentionally small,
  mirror only proven conventions, and defer extraction until a separately justified common
  abstraction exists.
- [A future persistence change might alter default startup] → Preserve the configuration
  boundary tests and require any profile or schema addition in a separate Change.

## Migration Plan

1. Add the module to the root reactor and implement its default skeleton only.
2. Run module and root `clean verify` without Docker or external services.
3. Deploying this increment is additive: no database, data migration, external configuration,
   or running-service upgrade is required.
4. Rollback consists of reverting the additive module and reactor entry; no data cleanup or
   destructive operation is involved.

## Open Questions

None for this skeleton. Booking command semantics, persistence, capacity interactions,
authentication, and cross-service communication are intentionally deferred to future Changes.
