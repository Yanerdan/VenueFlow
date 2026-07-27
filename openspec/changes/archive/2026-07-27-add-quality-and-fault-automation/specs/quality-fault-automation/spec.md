## ADDED Requirements

### Requirement: Repository quality policy is deterministic

The repository SHALL provide a connection-free quality command that verifies immutable image
tags, credential signatures, migration immutability conventions, Compose profile boundaries,
OpenSpec validity, and clean patch formatting. CI MUST execute the portable policy gate.

#### Scenario: Policy gate runs in CI

- **WHEN** the repository contains a forbidden secret signature or mutable image tag
- **THEN** the deterministic gate fails without starting infrastructure

### Requirement: Fault scenarios are explicit and bounded

The repository SHALL define consumer outage, Elasticsearch outage, Resource instance outage,
downstream latency, duplicate event, Outbox publisher outage, and Redis failure scenarios. Each
scenario MUST identify an exact target, precondition, observation, recovery action, and timeout.

#### Scenario: Scenario catalog is validated

- **WHEN** deterministic verification reads the catalog
- **THEN** every required scenario has bounded recovery metadata and no wildcard target

### Requirement: Fault execution is safe by default

The fault driver MUST operate in plan mode unless an explicit execution switch is supplied. Live
execution MUST require a local environment file, restrict commands to allowlisted targets, print
recovery before mutation, and MUST NOT delete volumes or schemas.

#### Scenario: Operator omits execution switch

- **WHEN** an operator selects any fault scenario without the execution switch
- **THEN** the driver emits a `PLANNED` manifest and performs no mutation

### Requirement: Evidence is honest and sanitized

Fault evidence SHALL distinguish planned and executed actions and MUST NOT contain credentials,
raw business payloads, fabricated latency, fabricated recovery, or unbounded logs.

#### Scenario: Dry-run evidence is reviewed

- **WHEN** a scenario has not been executed
- **THEN** its evidence status is `PLANNED` and contains no success claim

### Requirement: Recovery and rollback are executable

Every mutating fault scenario SHALL define a bounded recovery command and a post-recovery health
check. Repository documentation MUST state how to stop the exercise without deleting persistent
volumes.

#### Scenario: Fault action fails midway

- **WHEN** a scoped mutation returns non-zero
- **THEN** the driver reports failure, prints recovery, and does not continue to another target
