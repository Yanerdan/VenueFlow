## 1. Persistence and configuration

- [x] 1.1 Add approved security/JDBC/Flyway/MySQL/test dependencies and `auth-it`
- [x] 1.2 Add immutable Auth V001 credentials and refresh-session migration
- [x] 1.3 Add explicit persistence configuration with bounded fail-fast settings and PEM keys
- [x] 1.4 Preserve connection-free and security-free default skeleton startup

## 2. Credential and token lifecycle

- [x] 2.1 Add normalized credential, password policy, BCrypt, and lockout behavior
- [x] 2.2 Add parameter-bound repository transactions for registration and login state
- [x] 2.3 Add RS256 access JWT generation with bounded claims
- [x] 2.4 Add opaque refresh issue, atomic rotation, replay rejection, and idempotent logout

## 3. API and safety

- [x] 3.1 Add bounded register/login/refresh/logout DTO endpoints
- [x] 3.2 Add stable safe success/error envelopes and HTTP mappings
- [x] 3.3 Add profile-only security filter configuration and restricted health access

## 4. Verification

- [x] 4.1 Add Docker-free policy, service, token, configuration, web, and architecture tests
- [x] 4.2 Add MySQL 8.4.10 `auth-it` lifecycle and V001 evidence
- [x] 4.3 Run module/root, dependency, scope, secret, migration, and diff gates

## 5. Documentation and completion

- [x] 5.1 Update environment example, Auth/root READMEs, runbook, and HANDOFF
- [x] 5.2 Run strict OpenSpec validation and record final evidence before sync/archive
