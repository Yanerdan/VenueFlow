## Why

Auth Service is executable but cannot authenticate users, so the v0.2 login requirement remains
open. C18 adds the smallest durable credential and token lifecycle that is safe to build on before
Gateway and service-level authorization.

## What Changes

- Add Auth-owned V001 credentials and rotating refresh-token persistence.
- Add bounded username/password registration and login with BCrypt and generic credential errors.
- Add bounded failed-login lockout and reset on successful authentication.
- Issue short-lived RS256 access JWTs and opaque single-use refresh tokens.
- Add refresh rotation and explicit current-session logout/revocation.
- Keep all persistence and security behavior behind the explicit `persistence` profile; default
  `skeleton` remains connection-free.
- Add Docker-free unit/web tests and an opt-in MySQL 8.4.10 lifecycle suite.
- Exclude Gateway, cross-service authorization, roles, email recovery, MFA, Nacos, Redis, and
  application-container deployment.

## Capabilities

### New Capabilities

- `auth-credential-token-lifecycle`: Defines credential ownership, password/lockout policy,
  access JWT issuance, refresh rotation/revocation, APIs, and verification.

### Modified Capabilities

- `auth-service-skeleton`: Permits the narrowly profiled security and persistence dependencies
  while preserving connection-free default startup.

## Impact

- Extends `venueflow-auth-service` dependencies, configuration, code, migration, tests, and docs.
- Adds Auth environment examples for its database and RSA key material.
- Adds no change to User, Resource, Booking, Notification, Gateway, Compose, or another schema.
