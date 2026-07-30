## ADDED Requirements

### Requirement: Campus identity providers are explicitly configured and observable
Auth Service SHALL expose enabled OIDC providers with public display name and readiness state, SHALL keep client secrets outside tracked configuration, and MUST NOT advertise an incomplete provider as usable.

#### Scenario: Provider configuration is incomplete
- **WHEN** an administrator or login page reads provider readiness
- **THEN** the provider is reported unavailable with a bounded non-secret reason

### Requirement: Campus sign-in uses authorization code security controls
Auth Service SHALL initiate OIDC Authorization Code sign-in with PKCE, state, and nonce, SHALL validate issuer, signature, audience, expiry, state, and nonce on completion, and SHALL reject replayed or mismatched completions.

#### Scenario: Verified campus callback completes
- **WHEN** the configured provider returns a valid authorization response
- **THEN** Auth maps the verified identity and creates a short-lived single-use VenueFlow login completion

#### Scenario: State or nonce is invalid
- **WHEN** the callback does not match the initiated transaction
- **THEN** Auth returns a stable authentication failure and issues no VenueFlow token

### Requirement: External identities bind safely to VenueFlow accounts
Auth Service SHALL uniquely bind provider issuer and subject to one VenueFlow user, SHALL default a newly provisioned account to `APPLICANT`, and MUST reject automatic linking when a unique username or campus identity conflicts with another account.

#### Scenario: A new verified subject signs in
- **WHEN** no binding or identity conflict exists
- **THEN** Auth creates one account binding and issues the existing VenueFlow token pair

#### Scenario: A verified identity conflicts
- **WHEN** mapped identity facts already belong to a different VenueFlow account
- **THEN** Auth refuses automatic linking and records no new binding

### Requirement: Local authentication remains an explicit deployment fallback
Local password authentication SHALL remain available for local demonstration and explicitly enabled emergency administration, and production configuration SHALL be able to disable public local login without disabling external sign-in.

#### Scenario: Local login is disabled
- **WHEN** a caller submits password login in an external-only deployment
- **THEN** Auth rejects the method without revealing credential existence
