## ADDED Requirements

### Requirement: Auth owns durable credential and refresh-session facts

Auth Service SHALL add immutable V001 tables for credentials and refresh sessions in only the
Auth schema. Credentials MUST uniquely bind normalized username and generated user UUID to a
BCrypt hash, failed-attempt/lockout facts, token version, optimistic version, and timestamps.
Refresh rows MUST store only a unique SHA-256 token hash, user identity, token version, expiry,
revocation/replacement facts, and timestamps.

#### Scenario: A clean Auth schema receives V001

- **WHEN** Flyway migrates an empty Auth schema
- **THEN** only Auth-owned credential and refresh-session tables are created
- **AND** no plaintext password, refresh token, User profile, role, or other service table exists

### Requirement: Registration enforces bounded credentials

`POST /api/v1/auth/register` SHALL accept a normalized username and password, enforce username
and 12 to 72 character password policy bounds, generate the user UUID, and persist only a BCrypt
hash. Duplicate usernames MUST return a stable conflict without exposing stored facts.

#### Scenario: A valid registration is submitted

- **WHEN** a new normalized username and policy-compliant password are submitted
- **THEN** Auth returns the generated user UUID and username
- **AND** no password or hash is returned or logged

### Requirement: Login is side-channel-safe and failure-bounded

`POST /api/v1/auth/login` SHALL authenticate with BCrypt and return one generic invalid-credential
error for unknown usernames or wrong passwords. Unknown usernames MUST execute a dummy hash
comparison. Five consecutive failures MUST lock the credential for 15 minutes; a successful login
MUST reset failure facts before issuing tokens.

#### Scenario: Invalid credentials are repeated

- **WHEN** a credential reaches the configured failure threshold
- **THEN** later attempts before lock expiry fail with the same safe authentication envelope
- **AND** no password, hash, username-existence fact, or internal lock detail is exposed

#### Scenario: Valid credentials are supplied

- **WHEN** an unlocked credential matches
- **THEN** failure facts reset and one access/refresh pair is returned

### Requirement: Access tokens are short-lived bounded RS256 JWTs

Successful authentication or refresh SHALL issue an RS256 JWT containing only issuer, subject
UUID, normalized username, token version, random JWT ID, issued-at, and expiry. Access lifetime
MUST default to 15 minutes and be bounded from 1 to 60 minutes. RSA private/public keys MUST come
from untracked environment configuration and invalid keys MUST fail startup.

#### Scenario: An access token is decoded

- **WHEN** a token issued by Auth is verified with the configured public key
- **THEN** its signature, issuer, bounded time claims, subject, username, and token version match
- **AND** it contains no password, refresh token, profile, credential hash, or secret

### Requirement: Refresh tokens rotate once and logout revokes the session

Refresh tokens SHALL be opaque 256-bit random values with a default seven-day lifetime bounded
from one hour to thirty days. `POST /api/v1/auth/refresh` MUST atomically consume one unexpired,
unrevoked hash and create one replacement before returning a new pair. Replay or invalid tokens
MUST return a generic stable error. `POST /api/v1/auth/logout` MUST idempotently revoke the current
refresh hash and return no token.

#### Scenario: A refresh token is rotated

- **WHEN** an active refresh token is submitted once
- **THEN** Auth revokes it and returns one new access/refresh pair
- **AND** replaying the old value returns no token

#### Scenario: A session is logged out twice

- **WHEN** the same refresh token is submitted to logout more than once
- **THEN** each call succeeds without revealing prior session state
- **AND** the token cannot later refresh

### Requirement: Auth APIs use bounded DTOs and safe envelopes

Auth controllers MUST use DTOs and standard success/error envelopes, Bean Validation, JSON-only
bodies, bounded field sizes, and non-200 statuses for validation, duplicate, authentication,
token, and persistence failures. Responses and logs MUST never include passwords, hashes, key
material, raw database errors, or stack traces.

#### Scenario: An Auth request fails

- **WHEN** validation, authentication, token, or persistence rejects a request
- **THEN** the caller receives a stable bounded error code and trace ID
- **AND** no sensitive input or internal detail is exposed

### Requirement: Authentication verification is isolated and bounded

Default verification SHALL cover password policy, lockout, JWT claims, refresh rotation decisions,
DTOs, safe web errors, configuration, and architecture without external connections. Explicit
`auth-it` SHALL use MySQL 8.4.10 and ephemeral RSA keys to prove V001 and the end-to-end lifecycle.

#### Scenario: Default verification runs

- **WHEN** root `clean verify` runs
- **THEN** Auth tests open no database, container, key file, or collaborator connection

#### Scenario: Opt-in Auth verification runs

- **WHEN** `auth-it` runs with Docker
- **THEN** isolated MySQL proves registration, login, lockout reset, refresh rotation, and logout

### Requirement: C18 preserves service boundaries

C18 MUST NOT add User profiles, roles, authorization, Gateway, downstream JWT filters, email,
MFA, password recovery, social login, Redis, Nacos/Feign, messaging, tracing exporters, other
service migrations, or production application containers.

#### Scenario: C18 scope is inspected

- **WHEN** code, dependencies, migrations, configuration, tests, and deployment files are reviewed
- **THEN** changes remain limited to Auth credential/token lifecycle and documentation
