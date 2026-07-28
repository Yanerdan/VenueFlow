## ADDED Requirements

### Requirement: Resource catalog maintains booking rules

The Resource service SHALL persist validated booking rules with each resource and SHALL update them through a version-checked management operation.

#### Scenario: Existing resource is migrated

- **WHEN** the booking-rules migration is applied to an existing resource
- **THEN** the resource receives non-null permissive default time limits

#### Scenario: Concurrent rule update is stale

- **WHEN** an authorized user updates rules with an outdated expected version
- **THEN** the Resource service rejects the update as a version conflict
