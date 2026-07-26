## Context

C17 created a connection-free Auth skeleton. VenueFlow now needs durable username/password login
and independently verifiable token issuance before Gateway or service authorization can rely on
an identity boundary. Security-sensitive state must stay Auth-owned and default builds must remain
infrastructure-free.

## Goals / Non-Goals

**Goals:**

- Own credentials and refresh sessions in an isolated Auth schema.
- Apply bounded BCrypt password handling and failed-login lockout.
- Issue short-lived RS256 access JWTs with a minimal stable claim set.
- Rotate opaque refresh tokens exactly once and revoke the current session on logout.
- Keep configuration secret-free in Git and fail fast in `persistence`.

**Non-Goals:**

- User profiles, roles, authorization decisions, Gateway, or downstream resource-server changes.
- Email verification/recovery, MFA, social login, device management, or admin provisioning.
- Redis blacklists, Nacos/Feign, messaging, tracing, or production container deployment.

## Decisions

### 1. Auth owns credentials and refresh sessions

V001 creates `auth_credentials` and `auth_refresh_tokens` only. Registration generates the
principal UUID; User Service can later use that value as its external identity without sharing a
schema.

### 2. Use BCrypt and a bounded database lockout

Passwords are accepted only as char arrays at the web boundary, validated at 12–72 characters,
hashed with BCrypt strength 12, and never logged or persisted in plaintext. Five failures lock
the credential for 15 minutes; successful login resets the counters. Unknown usernames use a
dummy BCrypt comparison and the same external error.

### 3. Use RS256 access tokens and opaque rotating refresh tokens

Access tokens contain issuer, subject UUID, username, token version, issued-at, expiry, and a
random JWT ID; TTL is 15 minutes. RSA keys come only from environment-backed PEM configuration.
Refresh tokens are 256-bit random values; only SHA-256 hashes are stored. Refresh consumes one
active row and creates its replacement in one transaction, so replay fails.

### 4. Keep security/persistence explicit

`skeleton` excludes datasource, Flyway, and security auto-configuration. `persistence` enables
the Auth configuration, validates bounded TTL/lockout values and key material, migrates MySQL,
and exposes only the four Auth APIs plus health.

### 5. Separate deterministic and real-infrastructure verification

Default tests use collaborators/fakes for policy, token, service, DTO, web, and configuration
behavior. `auth-it` uses MySQL 8.4.10 and generated ephemeral RSA keys to prove V001 and the full
register/login/refresh/logout lifecycle.

## Risks / Trade-offs

- [Public registration can create an Auth identity before a User profile] → Keep identity facts
  separate and defer cross-service onboarding orchestration to a later change.
- [Offline access JWTs cannot be instantly revoked by logout] → Keep access TTL short and include
  token version for later resource-server enforcement.
- [Database lockout can be abused for denial of service] → Keep the threshold/time bounded and
  return one generic error; distributed adaptive controls remain a later Gateway concern.
- [PEM environment values are operationally awkward] → Accept standard PKCS#8/X.509 PEM and
  document generation without tracking keys.

## Migration Plan

1. Add dependencies and explicit persistence configuration.
2. Apply Auth V001 to the isolated schema.
3. Add credential/token domain, repository, service, JWT adapter, and APIs.
4. Add default and `auth-it` verification plus documentation.
5. Roll back application code by disabling `persistence`; retain V001 tables for forward recovery.

## Open Questions

None for C18. Gateway trust distribution, key rotation overlap, global logout, User onboarding,
roles, MFA, and recovery require later changes.
