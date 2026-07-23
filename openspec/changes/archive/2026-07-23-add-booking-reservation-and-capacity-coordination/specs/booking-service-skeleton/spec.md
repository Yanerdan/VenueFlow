## MODIFIED Requirements

### Requirement: Booking Service keeps an intentionally minimal dependency boundary

Booking Service MUST limit dependencies to its existing Web MVC, Actuator, and test support,
plus validation, MyBatis-Plus, Flyway, MySQL, Testcontainers, and JDK Java HTTP client support
required by the explicit reservation persistence profile. It MUST NOT introduce Feign, Nacos,
security/JWT, Redis, RabbitMQ, Kafka, tracing, Prometheus exporters, search, Gateway, shared
entities, another service implementation module, or cross-service database access.

#### Scenario: Inspect the reservation dependency tree

- **WHEN** Booking Service dependencies are inspected
- **THEN** only the approved reservation dependencies are resolved
- **AND** no prohibited infrastructure or business-service implementation is resolved

### Requirement: Booking Service has deterministic standalone configuration

Booking Service MUST preserve its secret-free `skeleton` profile on port `8084`. Its explicit
`persistence` profile MUST obtain Booking DB variables and User/Resource base URLs from
environment variables and MUST use bounded, configurable connect/request/lookup timeouts. It
MUST NOT alter skeleton startup, configure discovery, include usable credentials, or silently
substitute an in-memory database or fake collaborator.

#### Scenario: Start the skeleton without collaborators

- **WHEN** Docker, MySQL, User Service, and Resource Service are unavailable
- **THEN** Booking Service starts with `skeleton`
- **AND** it creates no datasource or outbound collaborator connection

#### Scenario: Start reservation persistence explicitly

- **WHEN** required local variables and the persistence profile are supplied
- **THEN** Booking Service enables only its own database and bounded collaborator adapters
- **AND** Flyway validates its own schema before reservation APIs are available

#### Scenario: Persistence configuration is incomplete

- **WHEN** a required Booking DB or collaborator URL variable is absent
- **THEN** persistence startup fails clearly
- **AND** no in-memory database, fake collaborator, or skeleton fallback is substituted
