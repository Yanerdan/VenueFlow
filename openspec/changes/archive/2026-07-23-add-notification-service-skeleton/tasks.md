## 1. Reactor and dependency boundary

- [x] 1.1 Audit the established Resource/User/Booking skeleton patterns before editing.
  - Acceptance: the implementation identifies the exact reactor, POM, configuration, health,
    executable-JAR, and test patterns to reuse without copying domain behavior.
  - Test: scoped source and dependency review.
- [x] 1.2 Add `venueflow-notification-service` to the root reactor with the minimal executable
  module POM.
  - Acceptance: the module inherits the parent/BOM, packages a Spring Boot JAR, and directly
    declares only Web MVC, Actuator, and approved test support.
  - Test: reactor build, executable-JAR inspection, dependency tree, Enforcer, convergence, and
    SBOM.
- [x] 1.3 Enforce Notification Service direct and transitive dependency exclusions.
  - Acceptance: persistence, messaging, mail, discovery, security, cache, search, tracing, and
    other service modules are rejected.
  - Test: positive dependency tree and deliberate/static forbidden-dependency verification.

## 2. Executable skeleton and configuration

- [x] 2.1 Add the Notification Service application entry point and deterministic configuration.
  - Acceptance: application name is `venueflow-notification-service`, default profile is
    `skeleton`, default port is `8085`, and `SERVER_PORT` can override it.
  - Test: context and configuration binding tests.
- [x] 2.2 Add the secret-free, connection-free `skeleton` profile.
  - Acceptance: default startup creates no datasource, RabbitMQ connection factory/listener,
    collaborator client, or outbound delivery connection and reads no external credentials.
  - Test: bean absence, configuration scan, and startup without infrastructure.
- [x] 2.3 Restrict Actuator to liveness and readiness health probes.
  - Acceptance: probes report `UP`; discovery and sensitive management endpoints expose no
    configuration, component details, mappings, logs, or write operations.
  - Test: real HTTP allowlist/denylist matrix on an isolated port.

## 3. Docker-free verification

- [x] 3.1 Add context, configuration-boundary, and architecture tests.
  - Acceptance: tests prove module identity, profile/port defaults, dependency scope, no sensitive
    files, and absence of domain/consumer packages and infrastructure beans.
  - Test: module default Maven test lifecycle without Docker.
- [x] 3.2 Add bounded executable-JAR startup and health verification.
  - Acceptance: the packaged JAR starts on an isolated port, serves only health probes, and is
    always terminated by the test.
  - Test: Failsafe executable-JAR and HTTP suites.
- [x] 3.3 Prove the default lifecycle starts no Testcontainers or external connection.
  - Acceptance: module and root default verification complete with Docker/MySQL/RabbitMQ
    unavailable.
  - Test: module `clean verify` followed by root `clean verify`.

## 4. Documentation and final scope

- [x] 4.1 Add the module README and update root README/HANDOFF.
  - Acceptance: documentation covers port, skeleton startup, probes, verification, current
    exclusions, C11 relationship, and C13 reliable-consumer follow-up without secrets or personal
    paths.
  - Test: documentation review and tracked secret/absolute-path scan.
- [x] 4.2 Run final validation and scope review.
  - Acceptance: module/root verification, Enforcer, Spotless, SpotBugs, SBOM, strict OpenSpec,
    `git diff --check`, and service-boundary scans pass; no database, migration, AMQP listener,
    queue, `ConsumedEvent`, retry/dead-letter, notification record, or Compose app appears.
  - Test: exact commands and scoped diff inventory recorded in HANDOFF.
