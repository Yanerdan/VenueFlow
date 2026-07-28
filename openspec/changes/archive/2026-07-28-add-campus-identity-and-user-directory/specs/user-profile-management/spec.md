## ADDED Requirements

### Requirement: User profiles support additive campus identity

User Service SHALL add an immutable V002 migration extending `user_profile` with optional unique
campus ID, bounded identity type, department, phone, and email facts. Existing V001 rows MUST
remain readable and MUST default to `OTHER` identity type without fabricating school identifiers.

#### Scenario: An existing profile is migrated

- **WHEN** V002 runs over a schema containing V001 profiles
- **THEN** every existing profile remains readable with unchanged identity and eligibility
- **AND** its identity type is `OTHER`

### Requirement: Campus profile APIs remain DTO-only and versioned

Profile creation SHALL accept campus identity fields, and `PATCH /api/v1/users/me/campus-profile`
SHALL update them using the trusted external identity and expected version. Requests and
responses MUST remain bounded DTOs and stale versions MUST return the established conflict.

#### Scenario: A new profile includes campus facts

- **WHEN** a caller creates a profile with valid optional campus identity fields
- **THEN** User Service returns those fields without exposing persistence details
