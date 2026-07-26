## 1. Module and runtime

- [x] 1.1 Register `venueflow-auth-service` in the root reactor
- [x] 1.2 Add the minimal managed Web MVC, Actuator, and test dependencies with Enforcer boundaries
- [x] 1.3 Add the Auth application entry point and executable JAR packaging

## 2. Configuration and health

- [x] 2.1 Add deterministic `skeleton` configuration, application identity, and port 8081 override
- [x] 2.2 Restrict Actuator Web exposure to liveness and readiness
- [x] 2.3 Prove default startup creates no infrastructure, collaborator, or security connection

## 3. Verification

- [x] 3.1 Add context, configuration, architecture, and sensitive-file tests
- [x] 3.2 Add HTTP health allowlist tests
- [x] 3.3 Add bounded executable-JAR startup verification
- [x] 3.4 Run module/root verification, dependency, scope, secret, and diff checks

## 4. Documentation and completion

- [x] 4.1 Add Auth README and update root README and HANDOFF
- [x] 4.2 Run strict OpenSpec validation and record final evidence before sync/archive
