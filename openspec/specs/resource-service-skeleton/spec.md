## Purpose

Define the runnable, infrastructure-independent Resource Service skeleton introduced in
C03 and the narrow persistence extension accepted by C04.

## Requirements

### Requirement: Reactor-integrated executable Resource Service

The repository MUST provide a `venueflow-resource-service` Spring Boot MVC module
integrated into the root Maven reactor. The module MUST inherit the established JDK 21,
BOM, and quality gates and MUST package an executable jar with an independent application
entry point.

#### Scenario: Build the complete reactor

- **WHEN** a developer runs Maven Wrapper `clean verify` with supported JDK 21
- **THEN** Resource Service is compiled, tested, quality-checked, and packaged with the
  other reactor modules

#### Scenario: Start the packaged service

- **WHEN** the packaged Resource Service jar is started with `java -jar`
- **THEN** it starts as `venueflow-resource-service` and serves HTTP health probes

### Requirement: Minimal service dependency boundary

Resource Service MUST keep its dependency set limited to dependencies accepted by
completed capabilities. It MAY use Spring Web, Spring Boot Actuator, tests, and—in the
explicit resource-catalog persistence profile—Bean Validation, MyBatis-Plus, Flyway, and
MySQL Connector/J. It MUST NOT introduce Nacos, Redis, RabbitMQ, Feign, Sentinel,
JWT/security, tracing, Prometheus clients, or unrelated infrastructure or business clients.

#### Scenario: The service dependency tree is intentionally small

- **WHEN** declared and resolved Resource Service dependencies are inspected
- **THEN** only skeleton dependencies and dependencies accepted for resource-catalog
  persistence are present
- **AND** no Nacos, Redis, RabbitMQ, Feign, Sentinel, JWT/security, tracing, Prometheus,
  or unrelated client dependency is present

### Requirement: Deterministic standalone configuration

Resource Service MUST declare `spring.application.name=venueflow-resource-service` and
MUST use `SERVER_PORT`, defaulting to `8083`, for its listening port. Its default
`skeleton` profile MUST remain secret-free and runnable without Docker, MySQL, Nacos,
Redis, RabbitMQ, or any other external service. An explicit persistence profile MAY enable
accepted resource-catalog database capabilities, but MUST obtain credentials from
environment variables or untracked local configuration rather than tracked secrets.

#### Scenario: An isolated developer can start the default service

- **GIVEN** no Docker daemon or external infrastructure
- **WHEN** Resource Service starts with default configuration
- **THEN** it starts on port 8083 unless `SERVER_PORT` is supplied
- **AND** it does not connect to MySQL, Nacos, Redis, RabbitMQ, or another service

#### Scenario: Override a conflicting local port

- **WHEN** a developer sets a valid `SERVER_PORT` before startup
- **THEN** the service listens on that port without changing tracked configuration

#### Scenario: Persistence is activated explicitly

- **GIVEN** valid resource database environment variables
- **WHEN** a developer explicitly selects the persistence profile
- **THEN** Resource Service enables accepted resource-catalog persistence
- **AND** the default skeleton profile remains available for infrastructure-free startup

### Requirement: Restricted Actuator health surface

Resource Service MUST expose `/actuator/health/liveness` and `/actuator/health/readiness`
through Actuator Web. It MUST NOT anonymously expose `env`, `configprops`, `loggers`,
`mappings`, `metrics`, dangerous write endpoints, or component health details.

#### Scenario: Probe a healthy standalone process

- **WHEN** a running service receives liveness and readiness requests
- **THEN** both respond successfully with status `UP`

#### Scenario: Request a sensitive management endpoint

- **WHEN** an anonymous caller requests an endpoint outside the health allowlist
- **THEN** the service does not provide it or leak environment, configuration, log, or
  mapping information

### Requirement: Infrastructure-independent automated verification

Resource Service startup and HTTP probe tests MUST run in default Maven `clean verify`,
use isolated random ports, and pass without Docker, MySQL, Redis, RabbitMQ, or Nacos.

#### Scenario: Verify without the base profile

- **WHEN** CI or a developer runs default Maven verification without Compose services
- **THEN** tests start a real Spring context, verify health probes and exposure boundaries,
  and complete in bounded time

#### Scenario: Run tests concurrently with another local service

- **WHEN** port 8083 is already occupied while Maven tests use random ports
- **THEN** Resource Service automated tests do not fail due to that fixed-port conflict

### Requirement: C03 scope isolation

C03 MUST NOT itself introduce resource business APIs, Entities, DTOs, Mappers, databases,
database users, Flyway migrations, capacity allocation/release, infrastructure clients,
service discovery, messaging, additional business services, or application-container
orchestration. Later accepted changes MAY extend this skeleton within their own scope.

#### Scenario: Inspect the C03 implementation diff

- **WHEN** C03 source, configuration, dependencies, and deployment files are inspected
- **THEN** its changes contain only the minimal Resource Service shell, Actuator acceptance,
  and corresponding engineering documentation
