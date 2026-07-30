## ADDED Requirements

### Requirement: Directory-owned campus facts cannot be overwritten by self-service
User Service SHALL identify campus ID, identity type, and organization facts owned by an active directory binding and SHALL reject ordinary profile updates that attempt to change those authoritative values, while allowing bounded user-owned contact fields.

#### Scenario: A directory-bound user edits their profile
- **WHEN** the user changes phone or email without changing authoritative fields
- **THEN** User Service saves the user-owned facts and retains synchronized campus facts

#### Scenario: A directory-bound user changes their organization
- **WHEN** self-service submits a different organization or campus identity
- **THEN** User Service rejects the authoritative-field change
