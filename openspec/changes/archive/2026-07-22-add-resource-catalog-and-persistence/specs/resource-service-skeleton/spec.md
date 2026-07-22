## MODIFIED Requirements

### Requirement: Minimal service dependency boundary

Resource Service MUST keep its dependency set limited to dependencies accepted by completed capabilities. It MAY use Spring Web, Spring Boot Actuator, tests, and—in the explicit resource-catalog persistence profile—Bean Validation, MyBatis-Plus, Flyway, and MySQL Connector/J. It MUST NOT introduce Nacos, Redis, RabbitMQ, Feign, Sentinel, JWT/security, tracing, Prometheus clients, or other unrelated infrastructure or business clients.

#### Scenario: The service dependency tree is intentionally small

- **WHEN** the service POM dependency tree is inspected
- **THEN** it contains only the skeleton dependencies and the dependencies explicitly accepted for the resource-catalog persistence profile
- **AND** it contains no Nacos, Redis, RabbitMQ, Feign, Sentinel, JWT/security, tracing, Prometheus, or unrelated client dependency

### Requirement: Deterministic standalone configuration

The Resource Service MUST declare `spring.application.name=venueflow-resource-service` and MUST use `SERVER_PORT`, defaulting to `8083`, for its listening port. Its default `skeleton` profile MUST remain secret-free and runnable without Docker, MySQL, Nacos, Redis, RabbitMQ, or any other external service. An explicit persistence profile MAY enable the accepted resource-catalog database dependencies, but it MUST obtain its datasource credentials from environment variables or untracked local configuration rather than tracked secrets.

#### Scenario: An isolated developer can start the default service

- **GIVEN** a developer has no Docker daemon and no external infrastructure running
- **WHEN** they start Resource Service with its default configuration
- **THEN** the application starts on port `8083` unless `SERVER_PORT` is provided
- **AND** it does not attempt to connect to MySQL, Nacos, Redis, RabbitMQ, or any other external service

#### Scenario: Persistence is activated explicitly

- **GIVEN** valid resource database environment variables are available
- **WHEN** a developer explicitly selects the persistence profile
- **THEN** Resource Service enables the accepted resource-catalog persistence capability
- **AND** the default skeleton profile remains available for infrastructure-free startup
