## MODIFIED Requirements

### Requirement: Notification Service keeps a minimal dependency boundary

Notification Service MUST limit direct dependencies to Spring Boot Web MVC, Actuator, and test
support for its base skeleton, plus only the persistence, Flyway, MySQL, validation, Spring AMQP,
and isolated integration-test dependencies required by the explicitly profiled reliable
consumer capability. It MUST NOT introduce mail clients, Feign, Nacos, security/JWT, Redis,
Kafka, tracing exporters, Elasticsearch, Gateway, shared business entities, or another service
implementation module.

Persistence and messaging dependencies MUST remain inactive in the default `skeleton` profile
and MUST be guarded by module dependency convergence, architecture, configuration, and
connection-absence tests.

#### Scenario: Inspect the Notification dependency tree

- **WHEN** declared and resolved Notification Service dependencies are inspected
- **THEN** only the skeleton and approved C13 persistence, AMQP, and test dependencies are present
- **AND** no mail, security, discovery, cache, search, tracing, or business-service dependency is
  resolved

#### Scenario: Verify the default dependency boundary

- **WHEN** Notification starts or is tested with only the default `skeleton` profile
- **THEN** approved persistence and messaging libraries create no infrastructure bean or
  external connection
