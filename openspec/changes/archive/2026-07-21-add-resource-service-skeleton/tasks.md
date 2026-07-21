## 1. Maven Module and Application Entry

- [x] 1.1 Add `venueflow-resource-service` to the root reactor and create a child POM that inherits the existing parent, declares only Web, Actuator and test starters, and applies the Spring Boot repackage goal.
- [x] 1.2 Add the `com.yanerdan.venueflow.resource` Spring Boot application entry point without empty future-layer packages, placeholder domain types or business endpoints.
- [x] 1.3 Add a build-time dependency boundary guard that rejects JDBC/MyBatis/Flyway/MySQL, Nacos, Redis, AMQP, Feign, Sentinel, security/JWT, tracing and Prometheus client dependencies from the C03 module.

## 2. Standalone Configuration and Health Contract

- [x] 2.1 Add secret-free `application.yml` configuration for application name `venueflow-resource-service`, default port 8083 and standard `SERVER_PORT` override, with no external config import or infrastructure connection.
- [x] 2.2 Enable Actuator liveness/readiness groups, expose only Web health endpoints, hide component details from anonymous requests and keep all dangerous or sensitive management endpoints unavailable.

## 3. Automated Verification

- [x] 3.1 Add a real Spring Boot context test that verifies the fixed application name and proves the module starts without Docker, `.env`, a data source or a configuration server.
- [x] 3.2 Add a random-port HTTP integration test that verifies liveness and readiness return successful `UP` responses and remains independent of port 8083 availability.
- [x] 3.3 Extend the HTTP integration test to verify representative sensitive endpoints such as `env`, `configprops`, `loggers`, `mappings` and `metrics` are not exposed and do not leak management data.
- [x] 3.4 Build the executable jar and run a bounded `java -jar` smoke on an overridden local port, verifying both probes before terminating only the process started by the test.

## 4. Documentation and Quality Gates

- [x] 4.1 Update README with module purpose, JDK 21 build command, packaged/local startup, `SERVER_PORT` override, health URLs and the explicit C03 non-goals.
- [x] 4.2 Confirm the existing GitHub Actions verify job automatically covers the new reactor module and preserves its test diagnostics without coupling Maven verification to the infrastructure job; adjust CI only if this acceptance is not already true.
- [x] 4.3 Run module-scoped `clean verify`, inspect the resolved dependency tree and executable jar, then run full-repository `clean verify` while the C02 containers are stopped.
- [x] 4.4 Run OpenSpec strict validation, repository diff/secret/artifact hygiene checks, and update `.agent/HANDOFF.md` with measured results and the next independent Change before marking C03 complete.
