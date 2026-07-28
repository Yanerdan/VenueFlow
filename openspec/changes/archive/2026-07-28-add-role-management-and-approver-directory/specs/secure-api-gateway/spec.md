## MODIFIED Requirements

### Requirement: Gateway validates Auth-issued access JWTs

Auth registration, login, refresh, and logout APIs and liveness/readiness SHALL remain public. Auth management APIs and every business route MUST require an RS256 JWT whose signature matches the configured Auth public key and whose issuer, expiry, not-before and subject claims are valid. Missing, malformed, expired, wrong-issuer, or invalid-signature tokens MUST return a bounded JSON 401 and MUST NOT reach a downstream service.

#### Scenario: A valid business request arrives

- **WHEN** a valid Auth-issued JWT accompanies an explicit business or Auth management route
- **THEN** Gateway authenticates the subject and permits routing

#### Scenario: An invalid token arrives

- **WHEN** JWT verification fails for any reason
- **THEN** Gateway returns `GATEWAY_UNAUTHORIZED` without token or key details
- **AND** no downstream request occurs

#### Scenario: An unauthenticated account-management request arrives

- **WHEN** a request without a valid access JWT targets an Auth management route
- **THEN** Gateway returns a bounded 401 before calling Auth Service
