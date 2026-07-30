## ADDED Requirements

### Requirement: Verified external identities enter the existing token lifecycle
After a verified external identity is bound, Auth Service SHALL issue the same bounded VenueFlow access JWT and rotating refresh session used by local authentication, including current campus role and token version.

#### Scenario: Campus sign-in completes
- **WHEN** a single-use external login completion is exchanged successfully
- **THEN** Auth returns one VenueFlow access/refresh pair governed by the existing refresh, logout, and role-revocation rules
