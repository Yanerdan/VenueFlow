## 1. Module and profiles

- [x] 1.1 Register the reactive Gateway module and approved dependencies
- [x] 1.2 Add executable skeleton configuration on port 8080 with restricted health
- [x] 1.3 Add explicit `gateway` profile properties and fail-fast bounds

## 2. Routing and security

- [x] 2.1 Add four explicit static routes with no discovery locator
- [x] 2.2 Add RS256 public-key parsing, issuer validation, and route authorization
- [x] 2.3 Add bounded JSON 401/403 responses without token details
- [x] 2.4 Strip forged identity headers and derive user ID only from JWT subject
- [x] 2.5 Add UUID trace propagation, request size bounds, CORS, and security headers

## 3. Verification

- [x] 3.1 Add skeleton context, dependency, configuration, health, and executable-JAR tests
- [x] 3.2 Add local-stub tests for routes, JWT decisions, headers, CORS, and bounds
- [x] 3.3 Run module/root, dependency, scope, secret, and diff gates

## 4. Documentation and completion

- [x] 4.1 Update environment example, Gateway/root READMEs, runbook, and HANDOFF
- [x] 4.2 Run strict OpenSpec validation and record final evidence before sync/archive
