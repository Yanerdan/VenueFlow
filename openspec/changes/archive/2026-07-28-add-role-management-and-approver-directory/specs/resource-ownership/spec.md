## MODIFIED Requirements

### Requirement: Resource owns bounded organizational responsibility

Resource Service SHALL store an optional owning department and assigned approver external user ID for each resource. Authorized management updates MUST be length-bounded, optimistic, and returned through resource DTOs without reading another service database. The management web application MUST select the assigned approver from the joined directory of accounts holding `APPROVER` or `SYSTEM_ADMIN` rather than requiring manual entry of an opaque identifier.

#### Scenario: A manager assigns resource responsibility

- **WHEN** a valid ownership update uses the current resource version and a directory-selected approver
- **THEN** Resource persists the department and approver identifier and advances the version

#### Scenario: An existing resource has no assignment

- **WHEN** a resource created before the additive migration is read
- **THEN** it remains readable with absent ownership fields

#### Scenario: A manager chooses an approver

- **WHEN** the management workspace displays the ownership editor
- **THEN** the approver field lists eligible personnel by readable name, department, and username while submitting their stable external user ID
