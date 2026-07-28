## MODIFIED Requirements

### Requirement: Resource owns bounded organizational responsibility

Resource Service SHALL store an optional owning department, initial approver external user ID, approval mode, and optional final approver external user ID for each resource. Authorized management updates MUST be length-bounded, optimistic, and returned through resource DTOs without reading another service database. The management web application MUST select assigned approvers from the joined eligible account directory.

#### Scenario: A manager assigns direct responsibility
- **WHEN** a valid ownership update selects direct mode, one approver, and the current resource version
- **THEN** Resource persists the policy and advances the version

#### Scenario: A manager assigns two-stage responsibility
- **WHEN** a valid ownership update selects two-stage mode and two distinct approvers
- **THEN** Resource persists both stage identities and advances the version

#### Scenario: An existing resource has no policy
- **WHEN** a resource created before the additive migration is read
- **THEN** it remains readable as direct approval with absent ownership fields
