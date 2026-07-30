# Campus User Directory Specification

## Purpose

Define bounded campus identity facts, self-service profile maintenance, and authorized personnel
directory access owned by User Service.
## Requirements
### Requirement: Campus identity fields are bounded

User Service SHALL own optional campus ID, identity type, department, phone, and email profile
facts. Identity type MUST be one of `STUDENT`, `STAFF`, or `OTHER`; all text fields MUST be
trimmed and bounded, and a nonblank campus ID MUST be unique.

#### Scenario: A user records a campus identity

- **WHEN** valid bounded campus identity fields are submitted
- **THEN** User Service persists and returns those fields with the profile

### Requirement: Users maintain their own campus profile

An authenticated user SHALL be able to update the mutable campus identity fields of the profile
identified by the trusted external identity header. The update MUST require the current optimistic
version and MUST NOT change internal or external identity.

#### Scenario: A user completes an incomplete profile

- **WHEN** the trusted user submits valid campus fields with the current version
- **THEN** the profile is updated and its version advances once

### Requirement: Authorized staff browse a bounded user directory

User Service SHALL expose a newest-first bounded profile directory to `APPROVER`,
`RESOURCE_MANAGER`, and `SYSTEM_ADMIN` role contexts. The directory MUST support optional bounded
keyword matching over display name, campus ID, and department, with zero-based pages and a maximum
page size of 100.

#### Scenario: An approver searches a department

- **WHEN** an approver submits a bounded department keyword
- **THEN** User Service returns only the bounded matching profile page

#### Scenario: A resource manager loads assignment candidates

- **WHEN** a resource manager requests the bounded directory
- **THEN** User Service returns profiles with their stable external user IDs for responsibility assignment

#### Scenario: An applicant requests the directory

- **WHEN** an applicant requests the management directory
- **THEN** User Service returns a stable forbidden response

### Requirement: Directory-owned campus facts cannot be overwritten by self-service
User Service SHALL identify campus ID, identity type, and organization facts owned by an active directory binding and SHALL reject ordinary profile updates that attempt to change those authoritative values, while allowing bounded user-owned contact fields.

#### Scenario: A directory-bound user edits their profile
- **WHEN** the user changes phone or email without changing authoritative fields
- **THEN** User Service saves the user-owned facts and retains synchronized campus facts

#### Scenario: A directory-bound user changes their organization
- **WHEN** self-service submits a different organization or campus identity
- **THEN** User Service rejects the authoritative-field change
