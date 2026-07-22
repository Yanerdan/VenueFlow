## 1. Maven module and dependency boundary

- [x] 1.1 Inspect the existing Resource Service skeleton, root reactor layout, dependency BOM, and quality-plugin conventions to reuse only the applicable engineering pattern.
- [x] 1.2 Add `venueflow-user-service` to the root Maven reactor and create its independent jar module with the established parent and dependency-management arrangement.
- [x] 1.3 Declare only Spring Web, Actuator, and test dependencies required by the skeleton; add an enforceable boundary that rejects persistence, security, infrastructure, and business-service client dependencies.
- [x] 1.4 Add the User Service application entry point and confirm its packaged jar has an independent main class.

## 2. Deterministic standalone configuration

- [x] 2.1 Add base and `skeleton` profile configuration declaring `spring.application.name=venueflow-user-service` and `SERVER_PORT` with default `8082`.
- [x] 2.2 Configure Actuator Web to expose only liveness and readiness health probes, without component details or sensitive management endpoints.
- [x] 2.3 Verify the default profile contains no datasource, credentials, external-service import, infrastructure client, or tracked secret.

## 3. Docker-free verification

- [x] 3.1 Add a Spring context test that starts User Service with the default skeleton profile without external infrastructure.
- [x] 3.2 Add HTTP probe tests using isolated random ports for liveness/readiness success and rejection or absence of sensitive actuator endpoints.
- [x] 3.3 Add an executable-jar integration test that starts the packaged User Service and verifies the health allowlist.
- [x] 3.4 Add a port-override test that proves `SERVER_PORT` changes the listening port without editing tracked configuration.

## 4. Scope and acceptance review

- [x] 4.1 Inspect the User Service source and resolved dependency tree to confirm no User domain, database/Flyway, authentication/JWT, Gateway, Nacos, Redis, RabbitMQ, Feign, tracing, search, or Booking behavior was introduced.
- [x] 4.2 Update developer-facing documentation with User Service purpose, default startup command, port override, and health endpoints without exposing local secrets.
- [x] 4.3 Run module-level verification and the root `mvn clean verify` path without Docker.
- [x] 4.4 Manually start the User Service jar in skeleton profile, verify both health probes and an overridden port, then review implementation against the User Service skeleton spec.
