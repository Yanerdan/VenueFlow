## ADDED Requirements

### Requirement: Booking Service is an executable Maven reactor module

The repository SHALL provide `venueflow-booking-service` as a Spring Boot MVC module in the
root Maven reactor. The module MUST inherit the established JDK 21 baseline, dependency BOM,
and quality gates, package an executable JAR, and declare an independent application entry
point.

#### Scenario: Build the complete reactor with Booking Service

- **WHEN** a developer runs Maven Wrapper `clean verify` with JDK 21
- **THEN** Booking Service compiles, tests, quality-checks, and packages with the other
  reactor modules

#### Scenario: Start the Booking Service JAR

- **WHEN** the packaged Booking Service JAR is started
- **THEN** it starts as `venueflow-booking-service` and serves only its allowed HTTP health
  probes

### Requirement: Booking Service keeps an intentionally minimal dependency boundary

Booking Service MUST limit its direct dependencies to Spring Web MVC, Spring Boot Actuator,
and test support accepted by the engineering baseline. It MUST NOT introduce persistence,
security/JWT, Nacos, Redis, RabbitMQ, Kafka, Feign, Sentinel, tracing, Prometheus,
Elasticsearch, Gateway, or Resource, User, Auth, Notification, or other business-service
clients.

#### Scenario: Inspect the Booking Service dependency tree

- **WHEN** declared and resolved Booking Service dependencies are inspected
- **THEN** only the accepted skeleton dependencies are present
- **AND** no persistence, external infrastructure, security, or business client is resolved

### Requirement: Booking Service has deterministic standalone configuration

Booking Service MUST declare `spring.application.name=venueflow-booking-service` and obtain
its listening port from `SERVER_PORT`, defaulting to `8084`. Its default `skeleton` profile
MUST remain secret-free and start without Docker, MySQL, Nacos, Redis, RabbitMQ, or another
external service. It MUST not declare datasource, external configuration imports, discovery,
or tracked credential configuration.

#### Scenario: Start with no external infrastructure

- **GIVEN** Docker and all external services are unavailable
- **WHEN** Booking Service starts with its default profile
- **THEN** it starts with `skeleton` on port `8084`
- **AND** it attempts no external connection

#### Scenario: Override a conflicting default port

- **WHEN** a developer supplies a valid `SERVER_PORT`
- **THEN** Booking Service listens on that port without changing tracked configuration

### Requirement: Booking Service exposes only restricted health management endpoints

Booking Service SHALL expose `/actuator/health/liveness` and
`/actuator/health/readiness` through Actuator Web. It MUST NOT anonymously expose `env`,
`configprops`, `loggers`, `mappings`, `metrics`, dangerous write endpoints, discovery, or
component health details.

#### Scenario: Probe a healthy standalone Booking Service

- **WHEN** liveness and readiness probes are sent to a running default Booking Service
- **THEN** both respond successfully with status `UP`

#### Scenario: Request a sensitive management endpoint

- **WHEN** an anonymous caller requests an endpoint outside the health allowlist
- **THEN** Booking Service does not provide it or leak configuration, mappings, logs, or
  operational details

### Requirement: Booking Service verification remains Docker-free by default

Booking Service context, HTTP probe, packaging, and executable-JAR tests MUST run in default
Maven `clean verify`, use isolated random ports where tests start an application, and pass
without Docker, MySQL, Redis, RabbitMQ, Nacos, or any other external service.

#### Scenario: Verify Booking Service without Compose infrastructure

- **WHEN** CI or a developer runs default Maven verification without Compose services
- **THEN** Booking Service tests start a real Spring context, verify management exposure and
  executable packaging, and complete in bounded time

### Requirement: This increment establishes no Booking domain behavior

This increment MUST NOT introduce booking APIs, entities, DTOs, persistence, database users,
Flyway migrations, capacity allocation or release, booking state transitions, Resource or User
calls, shared tables, authentication, authorization, messaging, caching, search, Gateway,
service discovery, application containers, or Compose application definitions.

#### Scenario: Inspect the Booking Service skeleton scope

- **WHEN** Booking Service source, configuration, dependencies, and deployment files are
  inspected
- **THEN** they contain only the executable skeleton, restricted health exposure, and
  corresponding tests
- **AND** no Booking business fact or external infrastructure behavior exists
