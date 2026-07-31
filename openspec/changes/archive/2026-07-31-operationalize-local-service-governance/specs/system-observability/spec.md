## ADDED Requirements

### Requirement: OTLP metrics are strictly opt-in

Every executable service SHALL disable OTLP metric publication unless the explicit `observe`
profile is active. The observation profile SHALL enable the exporter using an environment-owned
endpoint without making collector availability a business dependency.

#### Scenario: A service starts without observation

- **WHEN** any default, persistence, messaging, cache, search, gateway, governance, or stability
  profile combination starts without `observe`
- **THEN** no OTLP meter registry publisher attempts a collector connection

#### Scenario: A service starts with observation

- **WHEN** `observe` is active and the collector endpoint is configured
- **THEN** bounded application metrics may be exported
- **AND** exporter failure does not fail liveness or business requests
