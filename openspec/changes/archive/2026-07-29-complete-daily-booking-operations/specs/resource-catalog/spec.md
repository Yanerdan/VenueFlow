## ADDED Requirements

### Requirement: Resource managers edit core public facts
Resource Service SHALL expose a versioned mutation for the resource name, description, location, category, and capacity, SHALL reapply catalog validation, and SHALL reject stale versions without partially changing the resource.

#### Scenario: Core facts are updated
- **WHEN** an authorized manager submits valid core facts with the current resource version
- **THEN** Resource Service persists the facts, increments the version, and returns the updated resource DTO

#### Scenario: Stale edit is rejected
- **WHEN** an edit carries an obsolete resource version
- **THEN** Resource Service rejects the edit and preserves the newer stored facts
