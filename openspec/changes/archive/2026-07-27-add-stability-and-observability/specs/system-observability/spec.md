## ADDED Requirements

### Requirement: Observability is opt-in and non-blocking

Executable modules SHALL support an explicit `observe` profile for Prometheus metrics and OTLP
traces. Defaults MUST expose health only, create no exporter connection, and telemetry failure
MUST NOT fail business requests or readiness.

#### Scenario: Collector is unavailable

- **WHEN** an observed service cannot export a batch
- **THEN** its business and liveness behavior remains available

### Requirement: Metrics are bounded and useful

Metrics SHALL include standard HTTP/JVM/uptime data and bounded domain counters for booking,
Outbox, cache, messaging, and search where implemented. Labels MUST NOT contain credentials,
tokens, raw queries, payloads, or unbounded user/resource identities.

#### Scenario: Metrics are scraped

- **WHEN** Prometheus reads the protected endpoint
- **THEN** system and bounded domain series are available without sensitive labels

### Requirement: Trace context crosses synchronous and asynchronous boundaries

Canonical request trace identity SHALL continue across Gateway and Feign calls and SHALL be
included in resource event envelopes. OTLP export SHALL use environment-owned endpoints and MUST
NOT include authentication material in tracked configuration.

#### Scenario: Resource change reaches Search

- **WHEN** an observed request causes a Resource event
- **THEN** the event retains correlation identity for the Search projection path

### Requirement: Observe stack is explicit and provisioned

Compose SHALL provide an opt-in `observe` profile containing fixed-version Prometheus, Grafana,
OTel Collector, and Jaeger services with local-only ports, health checks, bounded resources,
provisioned data sources, and at least system/Gateway/Booking/Search dashboards.

#### Scenario: Base infrastructure starts

- **WHEN** only the `base` profile is selected
- **THEN** no observe component starts or consumes its resource budget

### Requirement: Observability verification is deterministic

Default tests SHALL verify profile boundaries, safe exposure, configuration, trace/event
propagation, metric label bounds, Compose provisioning, and timeout relationships without a live
collector, Prometheus, Grafana, Jaeger, or Sentinel dashboard.

#### Scenario: Root verification runs

- **WHEN** root `clean verify` executes
- **THEN** observability checks use local deterministic fixtures only
