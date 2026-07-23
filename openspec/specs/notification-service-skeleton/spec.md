# Notification Service Skeleton Specification

## Purpose

Define the minimal, independently executable Notification Service boundary before reliable event
consumption and notification domain capabilities are introduced.

## Requirements

### Requirement: Notification Service is an executable Maven reactor module

The repository SHALL provide `venueflow-notification-service` as a Spring Boot MVC module in the
root Maven reactor. It MUST inherit the JDK 21 baseline, dependency BOM, and quality gates,
package an executable JAR, and declare an independent application entry point.

#### Scenario: Build the complete reactor with Notification Service

- **WHEN** a developer runs Maven Wrapper `clean verify` with JDK 21
- **THEN** Notification Service compiles, tests, quality-checks, and packages with the other
  reactor modules

#### Scenario: Start the Notification Service JAR

- **WHEN** the packaged Notification Service JAR is started
- **THEN** it starts as `venueflow-notification-service`
- **AND** serves only its allowed HTTP health probes

### Requirement: Notification Service keeps a minimal dependency boundary

Notification Service MUST limit direct dependencies to Spring Boot Web MVC, Actuator, and test
support accepted by the engineering baseline. It MUST NOT introduce persistence, Flyway, MySQL,
Spring AMQP, RabbitMQ clients, mail clients, Feign, Nacos, security/JWT, Redis, Kafka, tracing
exporters, Elasticsearch, Gateway, shared business entities, or another service implementation
module.

#### Scenario: Inspect the Notification dependency tree

- **WHEN** declared and resolved Notification Service dependencies are inspected
- **THEN** only the approved skeleton dependencies are present
- **AND** no consumer, persistence, security, discovery, or business-service dependency is
  resolved

### Requirement: Notification Service has deterministic standalone configuration

Notification Service SHALL declare `spring.application.name=venueflow-notification-service`,
obtain its listening port from `SERVER_PORT` with default `8085`, and use `skeleton` as its
default profile. Tracked configuration MUST contain no usable credential or external endpoint.
Default startup MUST create no datasource, RabbitMQ connection factory, listener container,
collaborator client, or outbound notification connection.

#### Scenario: Start without external infrastructure

- **WHEN** Docker, MySQL, RabbitMQ, and every other service are unavailable
- **THEN** Notification Service starts with `skeleton` on port `8085`
- **AND** attempts no external connection

#### Scenario: Override a conflicting local port

- **WHEN** a developer supplies a valid `SERVER_PORT`
- **THEN** Notification Service listens on that port without changing tracked configuration

### Requirement: Notification Service exposes only restricted health management endpoints

Notification Service SHALL expose `/actuator/health/liveness` and
`/actuator/health/readiness` through Actuator Web. It MUST NOT anonymously expose `env`,
`configprops`, `loggers`, `mappings`, `metrics`, component details, or any management write
endpoint.

#### Scenario: Probe a healthy standalone Notification Service

- **WHEN** liveness and readiness probes are sent to a running default Notification Service
- **THEN** both respond successfully with status `UP`

#### Scenario: Request a sensitive management endpoint

- **WHEN** an anonymous caller requests an endpoint outside the health allowlist
- **THEN** Notification Service does not provide it or leak configuration, mappings, logs, or
  component details

### Requirement: Notification Service verification remains infrastructure-free

Default Maven verification SHALL cover application context, configuration boundaries, dependency
and architecture rules, HTTP probes, executable packaging, and real JAR startup without Docker,
MySQL, RabbitMQ, or another external service. Tests that start an application MUST use isolated
ports and bounded timeouts.

#### Scenario: Verify Notification Service without infrastructure

- **WHEN** CI or a developer runs default module or root Maven verification
- **THEN** all Notification Service tests complete without external connections
- **AND** no Testcontainers suite or broker/database fixture starts

### Requirement: C12 establishes no notification domain or consumer behavior

C12 MUST NOT create notification records, templates, delivery attempts, consumer queues,
bindings, dead-letter topology, `ConsumedEvent`, manual acknowledgement listeners, retries,
replay endpoints, email simulation, cross-service calls, or Compose application containers.
Those capabilities require later explicit Changes and profiles.

#### Scenario: Inspect the C12 scope

- **WHEN** Notification Service source, configuration, dependencies, APIs, and deployment files
  are reviewed
- **THEN** they contain only the executable skeleton, restricted health surface, and tests
- **AND** no event consumption or notification side effect exists
