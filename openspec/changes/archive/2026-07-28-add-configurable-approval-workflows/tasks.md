## 1. Resource approval policy

- [x] 1.1 Add additive resource approval-policy migration and domain/DTO fields
- [x] 1.2 Extend ownership updates with direct/two-stage validation and optimistic persistence
- [x] 1.3 Add focused Resource tests

## 2. Booking approval chain

- [x] 2.1 Add additive booking approval snapshot and action-history migration
- [x] 2.2 Snapshot policy at creation and authorize the current approval step
- [x] 2.3 Advance first-stage approval, finalize last-stage approval, and persist rejection/approval actions
- [x] 2.4 Return approval progress and history in booking APIs with focused tests

## 3. Web workflow

- [x] 3.1 Add direct/two-stage resource policy controls using eligible approver selects
- [x] 3.2 Show current approval stage and approval trajectory in applicant and management details
- [x] 3.3 Extend frontend contract tests

## 4. Acceptance

- [x] 4.1 Extend seed/smoke and runbook for two-stage approval
- [x] 4.2 Run targeted tests, live persistence smoke, browser verification, and strict OpenSpec validation
