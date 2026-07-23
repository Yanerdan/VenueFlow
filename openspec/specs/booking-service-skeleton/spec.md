# Booking Service Skeleton Specification

## Purpose

Define the independently executable, infrastructure-free Booking Service bootstrap boundary
for VenueFlow before Booking domain behavior is introduced.

## Requirements

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

Booking Service MUST limit dependencies to its existing Web MVC, Actuator, validation,
MyBatis-Plus, Flyway, MySQL, JDK Java HTTP client, and test support, plus Spring AMQP and
RabbitMQ Testcontainers required by the explicit Outbox messaging profile. It MUST NOT introduce
Spring Cloud Stream, Feign, Nacos, security/JWT, Redis, Kafka, tracing exporters, search,
Gateway, shared entities, another service implementation module, or cross-service database
access.

#### Scenario: Inspect the reservation dependency tree

- **WHEN** Booking Service dependencies are inspected
- **THEN** only the approved reservation dependencies are resolved
- **AND** no prohibited infrastructure or business-service implementation is resolved

#### Scenario: Inspect the Outbox publisher dependency tree

- **WHEN** Booking Service dependencies are inspected
- **THEN** only the approved reservation and reliable publisher dependencies are resolved
- **AND** no prohibited infrastructure or business-service implementation is resolved

### Requirement: Booking Service has deterministic standalone configuration

Booking Service MUST preserve its secret-free `skeleton` profile on port `8084`. Its explicit
`persistence` profile MUST continue to obtain Booking DB variables and User/Resource base URLs
from environment variables. Its explicit `messaging` profile MUST be layered with persistence
and MUST obtain required RabbitMQ connection credentials from environment variables, while
exchange name and bounded publisher timing/batch/retry settings use safe reviewed defaults or
environment overrides.

The service MUST NOT silently enable messaging, configure discovery, include usable credentials,
or substitute an in-memory broker/database. Starting with only `persistence` MUST accumulate
durable `NEW` Outbox rows without opening a RabbitMQ connection.

#### Scenario: Start the skeleton without collaborators

- **WHEN** Docker, MySQL, RabbitMQ, User Service, and Resource Service are unavailable
- **THEN** Booking Service starts with `skeleton`
- **AND** it creates no datasource, RabbitMQ connection factory, or outbound collaborator
  connection

#### Scenario: Start reservation persistence explicitly

- **WHEN** required local variables and the persistence profile are supplied
- **THEN** Booking Service enables only its own database and bounded collaborator adapters
- **AND** Flyway validates its own schema before reservation APIs are available

#### Scenario: Persistence configuration is incomplete

- **WHEN** a required Booking DB or collaborator URL variable is absent
- **THEN** persistence startup fails clearly
- **AND** no in-memory database, fake collaborator, or skeleton fallback is substituted

#### Scenario: Start reservation persistence without messaging

- **WHEN** required DB/collaborator variables and only `persistence` are supplied
- **THEN** Booking serves reservation APIs and records Outbox rows
- **AND** no Outbox publisher or RabbitMQ connection starts

#### Scenario: Start reliable publication explicitly

- **WHEN** `persistence,messaging` and all required DB/collaborator/RabbitMQ variables are
  supplied
- **THEN** Booking enables its bounded Outbox publisher
- **AND** validates lease duration against confirm timeout before scanning

#### Scenario: Messaging configuration is incomplete

- **WHEN** `messaging` is active without required RabbitMQ credentials or persistence
- **THEN** startup fails clearly
- **AND** no fake broker or skeleton fallback is substituted

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

The default `skeleton` profile MUST contain no active Booking domain, persistence, collaborator,
or messaging behavior. Booking reservation and Outbox behavior MAY exist only behind their
explicit profiles. Authentication, authorization, consumers, timeout jobs, caching, search,
Gateway, service discovery, and application-container orchestration remain outside this
capability.

#### Scenario: Inspect the Booking Service skeleton scope

- **WHEN** Booking Service source, configuration, dependencies, and deployment files are
  inspected
- **THEN** it exposes the restricted health surface and no Booking business API or worker
- **AND** it attempts no database, collaborator, or broker connection
