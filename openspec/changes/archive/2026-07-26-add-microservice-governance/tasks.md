## 1. Governance dependencies and profiles

- [x] 1.1 Add approved Nacos discovery/config dependencies to executable services
- [x] 1.2 Add LoadBalancer/OpenFeign dependencies only where required
- [x] 1.3 Add connection-free defaults and explicit governance profile configuration

## 2. Gateway and collaborator governance

- [x] 2.1 Add explicit load-balanced Gateway routes without discovery locator
- [x] 2.2 Add governance-only Booking Feign contracts and adapters
- [x] 2.3 Preserve non-retrying writes, bounded timeouts, lookup, and reconciliation semantics
- [x] 2.4 Add canonical trace acceptance and Feign trace propagation

## 3. Deterministic verification

- [x] 3.1 Add configuration, dependency, route, timeout, and profile boundary tests
- [x] 3.2 Add two-instance Resource selection and single-instance failure tests
- [x] 3.3 Run affected module and root verification plus dependency/scope/secret gates

## 4. Documentation and completion

- [x] 4.1 Update environment example, READMEs, governance runbook, and HANDOFF
- [x] 4.2 Run strict OpenSpec validation and record final evidence
