# Auth Service Skeleton Specification

## Purpose

Define the minimal independently executable Auth Service boundary before credentials, login,
tokens, JWT, and authorization capabilities are introduced.

## Requirements

### Requirement: Auth Service is an executable Maven reactor module

The repository SHALL provide `venueflow-auth-service` as a Spring Boot MVC module in the root
Maven reactor. It MUST inherit the JDK 21 baseline, dependency BOM, and quality gates, package an
executable JAR, and declare an independent application entry point.

#### Scenario: Build the complete reactor with Auth Service

- **WHEN** a developer runs Maven Wrapper `clean verify` with JDK 21
- **THEN** Auth Service compiles, tests, quality-checks, and packages with the other reactor modules

#### Scenario: Start the Auth Service JAR

- **WHEN** the packaged Auth Service JAR is started
- **THEN** it starts as `venueflow-auth-service`
- **AND** serves only its allowed HTTP health probes

### Requirement: Auth Service keeps a minimal dependency boundary

Auth Service MUST limit direct dependencies to Spring Boot Web MVC, Actuator, validation,
Security, JDBC, Flyway, MySQL runtime, and test support required by the explicitly profiled
credential/token capability. It MUST NOT introduce JPA, MyBatis, Feign, Nacos, messaging, Redis,
Elasticsearch, tracing exporters, Gateway, shared business entities, or another service
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

### Requirement: Auth Service exposes only restricted health management endpoints

Auth Service SHALL expose `/actuator/health/liveness` and `/actuator/health/readiness` through
Actuator Web. It MUST NOT anonymously expose `env`, `configprops`, `loggers`, `mappings`,
`metrics`, component details, or any management write endpoint.

#### Scenario: Probe a healthy standalone Auth Service

- **WHEN** liveness and readiness probes are sent to a running default Auth Service
- **THEN** both respond successfully with status `UP`

#### Scenario: Request a sensitive management endpoint

- **WHEN** an anonymous caller requests an endpoint outside the health allowlist
- **THEN** Auth Service does not provide it or leak configuration, mappings, logs, or component details

### Requirement: Auth Service verification remains infrastructure-free

Default Maven verification SHALL cover application context, skeleton configuration boundaries,
dependency and architecture rules, HTTP probes, executable packaging, and real JAR startup without
Docker or another external service. Profile-specific tests MUST be excluded by default and tests
that start an application MUST use isolated ports and bounded timeouts.

#### Scenario: Verify Auth Service without infrastructure

- **WHEN** CI or a developer runs default module or root Maven verification
- **THEN** skeleton and Docker-free credential/token tests complete without external connections
- **AND** no Testcontainers suite or database fixture starts

### Requirement: C17 establishes no authentication behavior

C17 MUST NOT create credentials, password hashes, login/logout endpoints, access or refresh
tokens, JWT keys, roles, lockout state, revocation, Gateway rules, User collaboration, persistence,
service discovery, messaging, caching, or Compose application containers.

#### Scenario: Inspect the C17 scope

- **WHEN** Auth Service source, configuration, dependencies, APIs, and deployment files are reviewed
- **THEN** they contain only the executable skeleton, restricted health surface, and tests
- **AND** no authentication or authorization side effect exists
