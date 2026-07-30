## ADDED Requirements

### Requirement: Administrators inspect campus integration readiness
The management workspace SHALL show configured identity providers, non-secret readiness, organization synchronization freshness, last outcome, and bounded counts to `SYSTEM_ADMIN` users.

#### Scenario: Administrator opens integration governance
- **WHEN** provider or directory integration is not configured
- **THEN** the workspace shows an actionable unavailable state without exposing secrets

### Requirement: Administrators synchronize and browse organizations
The management workspace SHALL let `SYSTEM_ADMIN` users submit bounded canonical organization synchronization data, inspect run results, and browse the active hierarchy and memberships.

#### Scenario: Administrator imports a directory batch
- **WHEN** a valid uniquely keyed batch succeeds
- **THEN** the workspace refreshes organization counts, hierarchy, and latest run status

### Requirement: Resource managers configure ordered approval policies
The management workspace SHALL let authorized resource managers create or edit one to five ordered approval stages using eligible accounts and assign an active policy to a resource with optimistic concurrency.

#### Scenario: Manager saves three approval stages
- **WHEN** each stage has a label, sequence, and eligible assignee
- **THEN** the workspace displays the persisted three-stage policy in order
