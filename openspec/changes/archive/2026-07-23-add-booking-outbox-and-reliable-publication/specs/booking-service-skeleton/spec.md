## MODIFIED Requirements

### Requirement: Booking Service keeps an intentionally minimal dependency boundary

Booking Service MUST limit dependencies to its existing Web MVC, Actuator, validation,
MyBatis-Plus, Flyway, MySQL, JDK Java HTTP client, and test support, plus Spring AMQP and
RabbitMQ Testcontainers required by the explicit Outbox messaging profile. It MUST NOT introduce
Spring Cloud Stream, Feign, Nacos, security/JWT, Redis, Kafka, tracing exporters, search,
Gateway, shared entities, another service implementation module, or cross-service database
access.

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

### Requirement: This increment establishes no Booking domain behavior

The default `skeleton` profile MUST contain no active Booking domain, persistence, collaborator,
or messaging behavior. Booking reservation and Outbox behavior MAY exist only behind their
explicit profiles. Authentication, authorization, consumers, timeout jobs, caching, search,
Gateway, service discovery, and application-container orchestration remain outside this
capability.

#### Scenario: Inspect the default Booking Service scope

- **WHEN** Booking Service starts with only `skeleton`
- **THEN** it exposes the restricted health surface and no Booking business API or worker
- **AND** it attempts no database, collaborator, or broker connection
