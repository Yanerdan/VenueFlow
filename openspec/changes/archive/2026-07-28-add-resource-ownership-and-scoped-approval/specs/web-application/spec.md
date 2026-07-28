## ADDED Requirements

### Requirement: Management workspace configures and presents responsibility

The zero-build management workspace SHALL provide bounded resource ownership controls and SHALL
present assignment facts in resource records and booking review details. It MUST show unassigned
facts explicitly rather than inventing responsibility.

#### Scenario: A system administrator assigns a resource

- **WHEN** the administrator selects an approver and saves the resource
- **THEN** the refreshed resource card and later booking review show the persisted assignment
