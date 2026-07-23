# User Service Skeleton Specification

## Purpose

Define the independently executable, infrastructure-free User Service bootstrap boundary for VenueFlow.

## Requirements

### Requirement: User Service is an executable Maven reactor module

The repository SHALL provide `venueflow-user-service` as a Spring Boot MVC module in the root Maven reactor. It MUST inherit the established JDK 21 baseline and quality gates, package an executable jar, and declare an independent application entry point.

#### Scenario: Build the complete reactor with User Service

- **WHEN** a developer runs Maven Wrapper `clean verify` with JDK 21
- **THEN** User Service compiles, tests, quality-checks, and packages with the other modules

#### Scenario: Start the User Service jar

- **WHEN** the packaged User Service jar is started
- **THEN** it starts as `venueflow-user-service` and serves its allowed HTTP health probes

### Requirement: User Service keeps an intentionally minimal dependency boundary

User Service MUST limit its dependencies to Spring Web, Spring Boot Actuator, validation,
and test support accepted by the engineering baseline, plus MyBatis-Plus, Flyway, MySQL
driver, and test-only MySQL verification dependencies required by the explicit User Service
`persistence` profile. It MUST NOT introduce security/JWT, Nacos, Redis, RabbitMQ, Feign,
Sentinel, tracing, Prometheus, Elasticsearch, or any Resource, Booking, Auth, or other
business-service client.

#### Scenario: Inspect the User Service dependency tree

- **WHEN** declared and resolved User Service dependencies are inspected
- **THEN** only the accepted skeleton, validation, and User Service persistence dependencies
  are present
- **AND** no external infrastructure client, security, or business client is resolved

### Requirement: User Service has deterministic standalone configuration

User Service MUST declare `spring.application.name=venueflow-user-service` and obtain its
listening port from `SERVER_PORT`, defaulting to `8082`. Its default `skeleton` profile MUST
be secret-free and start without Docker, MySQL, Nacos, Redis, RabbitMQ, or another external
service. Its explicit `persistence` profile MUST obtain User Service database configuration
only from environment variables and MUST not alter default skeleton startup behavior.

#### Scenario: Start with no external infrastructure

- **GIVEN** no Docker daemon or external service is available
- **WHEN** User Service starts with its default profile
- **THEN** it starts on port 8082 and does not attempt an external connection

#### Scenario: Override a conflicting default port

- **WHEN** a developer supplies a valid `SERVER_PORT`
- **THEN** User Service listens on that port without changing tracked configuration

#### Scenario: Start persistence only with explicit environment configuration

- **WHEN** a developer explicitly activates the `persistence` profile and supplies User
  Service database environment variables
- **THEN** User Service connects only to its configured User Service schema
- **AND** skeleton configuration remains unchanged

### Requirement: User Service exposes only restricted health management endpoints

User Service SHALL expose `/actuator/health/liveness` and `/actuator/health/readiness` through Actuator Web. It MUST NOT anonymously expose `env`, `configprops`, `loggers`, `mappings`, `metrics`, dangerous write endpoints, or component health details.

#### Scenario: Probe a healthy standalone User Service

- **WHEN** liveness and readiness probes are sent to a running default User Service
- **THEN** both respond successfully with status `UP`

#### Scenario: Request a sensitive management endpoint

- **WHEN** an anonymous caller requests an endpoint outside the health allowlist
- **THEN** User Service does not provide it or leak configuration, mappings, or log details

### Requirement: User Service verification remains Docker-free by default

User Service startup, packaging, and HTTP probe tests MUST run in default Maven `clean verify`, use isolated random ports where tests start an application, and pass without Docker, MySQL, Redis, RabbitMQ, Nacos, or any other external service.

#### Scenario: Verify User Service without Compose infrastructure

- **WHEN** CI or a developer runs default Maven verification without Compose services
- **THEN** User Service tests start a real Spring context, verify health exposure boundaries,
- **AND** complete in bounded time

### Requirement: User Service scope excludes authentication and cross-service behavior

User Service MUST retain local ownership of profile and eligibility facts and MUST NOT introduce registration credentials, login, access or refresh tokens, JWT validation, roles, authorization, Gateway, service discovery, messaging, caching, search, Booking behavior, shared tables, or cross-service database access.

#### Scenario: Inspect the User Service skeleton scope

- **WHEN** the User Service source, dependencies, configuration, and deployment files are inspected
- **THEN** profile ownership remains local to User Service and health exposure remains restricted
- **AND** no security implementation, cross-service behavior, or external infrastructure client exists
