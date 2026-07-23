## MODIFIED Requirements

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
