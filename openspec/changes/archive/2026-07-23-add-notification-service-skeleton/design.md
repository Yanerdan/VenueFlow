## Context

C11 completed the Booking-owned Outbox producer and intentionally created no consumer queue.
The repository currently contains Resource, User, and Booking services, but no owner for
notification consumption. Adding a reliable consumer directly to a newly created module would
mix bootstrap, persistence, broker topology, acknowledgement semantics, deduplication, retries,
and side effects in one Change. C12 first establishes a small independently testable service
boundary consistent with the existing skeleton pattern.

## Goals / Non-Goals

**Goals:**

- Add an executable Notification Service module to the root reactor.
- Preserve a secret-free, connection-free default profile on port `8085`.
- Reuse the established restricted Actuator and Maven quality baseline.
- Make dependency and scope boundaries executable through tests and Enforcer.
- Leave a stable base for C13 reliable consumption.

**Non-Goals:**

- RabbitMQ exchanges, queues, bindings, listeners, manual ACK, retry, dead letters, or replay.
- Notification persistence, Flyway, `ConsumedEvent`, templates, delivery records, email, or HTTP
  notification APIs.
- Nacos, Feign, Gateway, authentication, Redis, tracing exporters, Compose application
  containers, or cross-service calls.
- Changing the C11 producer, exchange, event envelope, or Booking migrations.

## Decisions

### 1. Use a dedicated service module and port 8085

Create `venueflow-notification-service` with its own Spring Boot entry point and add it to the
root reactor after Booking. Port `8085` follows the frozen service port sequence
Auth/User/Resource/Booking/Notification/Search = `8081` through `8086`.

Alternative: place notification consumption in Booking. Rejected because notification failure
must not block the reservation service and because consumer persistence/retry ownership belongs
to Notification.

### 2. Keep C12 limited to Web MVC, Actuator, and tests

The module declares only the smallest centrally managed dependencies required for executable
health verification. Module Enforcer rules whitelist those direct dependencies and reject
persistence, messaging, discovery, security, cache, search, tracing, and other service modules.

Alternative: add AMQP and MySQL now but leave them unused. Rejected because their auto-
configuration weakens proof that default startup is connection-free and expands the failure
surface before any consumer behavior is specified.

### 3. Reuse the explicit skeleton configuration pattern

`application.yml` defines only application name, default `skeleton` profile, port, and restricted
health management. `application-skeleton.yml` excludes datasource and Flyway auto-configuration
defensively, matching the existing services. No environment variable other than `SERVER_PORT`
is needed in C12.

Alternative: copy future broker/database placeholders into the skeleton. Rejected because
tracked unused configuration becomes misleading and can accidentally activate infrastructure.

### 4. Prove the boundary at source, context, HTTP, and packaged-JAR levels

Tests cover:

- default profile, name, port, and absence of datasource/RabbitMQ beans;
- configuration and sensitive-file scanning;
- liveness/readiness success and denial of management endpoints;
- executable JAR startup on an isolated port;
- dependency/architecture exclusions and root reactor integration.

Default verification remains Docker-free and has bounded process cleanup.

Alternative: rely only on a context-load test. Rejected because it would not prove executable
packaging, actual HTTP exposure, or connection-free configuration.

### 5. Defer the consumer contract to C13

C13 should add Notification-owned V001 persistence, a consumer-owned queue/binding, manual ACK,
transactional `(consumer_name,event_id)` deduplication, bounded retry/dead-letter behavior,
safe notification records, replay controls, and real RabbitMQ/MySQL verification. Separating it
keeps C12 reversible and gives the consumer state machine a dedicated design review.

Alternative: implement the complete consumer in C12. Rejected because failures could be
misattributed to module bootstrap rather than acknowledgement, persistence, or broker semantics.

## Risks / Trade-offs

- [Booking events remain unroutable after C12 alone] → Keep C11 retry/dead visibility and enable
  production publishing only after C13 installs the consumer-owned binding.
- [A skeleton Change adds no end-user feature] → Keep it small and immediately follow with the
  reliable consumer Change.
- [Boilerplate diverges across services] → Mirror established structure and verification without
  introducing shared service abstractions.
- [Future dependencies can leak into the default profile] → Preserve explicit profiles and tests
  asserting absence of datasource, RabbitMQ, listeners, and external connections.

## Migration Plan

1. Add the new module to the Maven reactor.
2. Add application/configuration files and restricted health behavior.
3. Add module dependency enforcement and Docker-free verification.
4. Update root/module documentation and HANDOFF.
5. Run module and root `clean verify`, dependency/SBOM checks, strict OpenSpec validation, and
   scope scans.

Rollback removes the new reactor entry and module directory. C11 Booking data and events are
unchanged; no database or broker migration is introduced.

## Open Questions

None for C12. Queue names, dead-letter routing, notification schema, delivery channels, retry
budget, and operator replay authorization are intentionally deferred to C13.
