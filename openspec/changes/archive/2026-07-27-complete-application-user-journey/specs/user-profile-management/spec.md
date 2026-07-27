## ADDED Requirements

### Requirement: Current profile resolves from trusted external identity

User Service SHALL expose `GET /api/v1/users/me` and require a bounded `X-User-Id` external
identity header. It MUST validate the header using the existing external identity value object,
query only User-owned data, and return the existing bounded profile DTO or the established
not-found error.

#### Scenario: Authenticated user resolves their profile

- **WHEN** Gateway forwards a valid JWT subject as `X-User-Id`
- **THEN** User Service returns the profile owning that immutable external identity
- **AND** no credential or Auth database access occurs
