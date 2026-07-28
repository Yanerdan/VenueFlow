## MODIFIED Requirements

### Requirement: Access tokens are short-lived bounded RS256 JWTs

Successful authentication or refresh SHALL issue an RS256 JWT containing only issuer, subject
UUID, normalized username, bounded campus role, token version, random JWT ID, issued-at, and
expiry. Access lifetime MUST default to 15 minutes and be bounded from 1 to 60 minutes. RSA
private/public keys MUST come from untracked environment configuration and invalid keys MUST fail
startup.

#### Scenario: An access token is decoded

- **WHEN** a token issued by Auth is verified with the configured public key
- **THEN** its signature, issuer, bounded time claims, subject, username, campus role, and token
  version match
- **AND** it contains no password, refresh token, profile, credential hash, or secret
