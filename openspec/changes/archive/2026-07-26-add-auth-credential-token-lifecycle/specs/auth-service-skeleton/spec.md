## MODIFIED Requirements

### Requirement: Auth Service keeps a minimal dependency boundary

Auth Service MUST limit direct dependencies to Spring Boot Web MVC, Actuator, validation,
Security, JDBC, Flyway, MySQL runtime, and test support required by the explicitly
profiled credential/token capability. It MUST NOT introduce JPA, MyBatis, Feign, Nacos, messaging,
Redis, Elasticsearch, tracing exporters, Gateway, shared business entities, or another service
implementation module.

Persistence and security behavior MUST remain inactive in the default `skeleton` profile and MUST
be guarded by dependency convergence, architecture, configuration, and connection-absence tests.

#### Scenario: Inspect the Auth dependency tree

- **WHEN** declared and resolved Auth Service dependencies are inspected
- **THEN** only skeleton and approved C18 security, persistence, and isolated test dependencies exist
- **AND** no JPA, discovery, messaging, cache, search, tracing, or business-service dependency resolves

### Requirement: Auth Service has deterministic standalone configuration

Auth Service SHALL declare `spring.application.name=venueflow-auth-service`, obtain its listening
port from `SERVER_PORT` with default `8081`, and use `skeleton` as its default profile. Tracked
configuration MUST contain no usable credential, JWT key, token, or external endpoint. Default
startup MUST create no datasource, security filter, collaborator client, broker connection, cache
client, or key loader. The explicit `persistence` profile MUST fail fast when its database, RSA
key, or bounded security configuration is missing or invalid.

#### Scenario: Start without external infrastructure

- **WHEN** Docker, databases, keys, and every other service are unavailable
- **THEN** Auth Service starts with `skeleton` on port `8081`
- **AND** attempts no external connection or key parsing

#### Scenario: Override a conflicting local port

- **WHEN** a developer supplies a valid `SERVER_PORT`
- **THEN** Auth Service listens on that port without changing tracked configuration

### Requirement: Auth Service verification remains infrastructure-free

Default Maven verification SHALL cover application context, skeleton configuration boundaries,
dependency and architecture rules, HTTP probes, executable packaging, and real JAR startup without
Docker or another external service. Profile-specific tests MUST be excluded by default and tests
that start an application MUST use isolated ports and bounded timeouts.

#### Scenario: Verify Auth Service without infrastructure

- **WHEN** CI or a developer runs default module or root Maven verification
- **THEN** skeleton and Docker-free credential/token tests complete without external connections
- **AND** no Testcontainers suite or database fixture starts
