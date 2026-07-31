## Why

Cross-layer acceptance found that actionable synthetic reservations do not own matching capacity facts, oversized Gateway requests can leave request buffers unreleased, and repeated local verification leaves obsolete records visible in the demonstration workspace.

## What Changes

- Make synthetic pending and confirmed reservations consistent with Resource slot state and allocation ledger facts.
- Remove obsolete non-showcase local acceptance records during deterministic reseeding.
- Safely drain rejected oversized request bodies and cover the behavior with regression tests.
- Refresh web asset versions and document the Windows stop-before-build requirement.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `semester-showcase-data`: Preserve cross-service invariants and clean local acceptance residue.
- `secure-api-gateway`: Release rejected request buffers safely.
- `engineering-baseline`: Clarify verification while local executable JARs are running.

## Impact

- Local showcase SQL and smoke workflow.
- Gateway boundary filter and integration tests.
- Static web cache keys and README verification instructions.
