# service-stability-controls Specification

## Purpose

Define opt-in overload controls and nested timeout budgets without inventing production capacity.

## Requirements

### Requirement: Stability controls are opt-in and scope-limited

Gateway, Booking, and Search SHALL enable Sentinel only with an explicit `stability` profile.
Default startup MUST remain connection-free, and other services MUST NOT gain Sentinel
dependencies without a separate requirement.

#### Scenario: Default verification runs

- **WHEN** root `clean verify` starts executable modules
- **THEN** no Sentinel dashboard or rule source is contacted

### Requirement: Overload never becomes false business success

Gateway and Search SHALL reject protected bounded reads with `RATE_LIMITED` or explicit
unavailable responses. Booking writes MUST only reject/queue on overload and MUST NOT return a
fabricated success, repeat a capacity write, or silently fall back to stale state.

#### Scenario: Booking creation is blocked

- **WHEN** the stability boundary rejects a create request
- **THEN** the caller receives a bounded non-success response and no downstream write occurs

### Requirement: Timeout budgets are strictly nested

Tracked configuration SHALL keep the Gateway downstream response budget greater than Booking's
request budget, which SHALL be greater than each Feign collaborator read/connect budget. Tests
MUST fail if this ordering is inverted.

#### Scenario: Configuration is changed

- **WHEN** a collaborator timeout exceeds its caller budget
- **THEN** deterministic configuration verification fails

### Requirement: Stability rules are versioned without invented thresholds

The repository SHALL version Gateway, Booking, and Search Sentinel rule documents. Initial
traffic thresholds MUST be disabled or clearly marked as requiring measured load evidence.

#### Scenario: Rule templates are reviewed

- **WHEN** no reproducible pressure result exists
- **THEN** tracked rules contain no claimed production capacity threshold
