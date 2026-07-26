## 1. Persistence and domain

- [x] 1.1 Add immutable Booking V005 for `COMPLETED` and `completed_at`, preserving V001-V004
- [x] 1.2 Extend Booking domain/entity/DTO mapping and status audit for completion
- [x] 1.3 Add Notification V003 only where required to admit the completion event type

## 2. Check-in orchestration

- [x] 2.1 Add bounded check-in window and Resource slot-read configuration with fail-fast validation
- [x] 2.2 Extend the Resource client with one validated bounded slot-time lookup
- [x] 2.3 Implement completed replay and confirmed-reservation temporal eligibility decisions
- [x] 2.4 Implement conditional `CONFIRMED -> COMPLETED` with timestamp, audit, and Outbox event
- [x] 2.5 Expose the DTO-only check-in endpoint and stable safe error mappings

## 3. Event and notification

- [x] 3.1 Add the bounded `booking.reservation.completed.v1` Outbox envelope
- [x] 3.2 Bind and strictly decode the completion route in Notification
- [x] 3.3 Derive one deterministic inbox-idempotent completion notification

## 4. Verification

- [x] 4.1 Add Docker-free unit/web/configuration tests for windows, replay, states, races, and envelopes
- [x] 4.2 Add `checkin-it` MySQL 8.4.10 and HTTP-stub tests for V005 and atomic completion
- [x] 4.3 Extend Booking Outbox and Notification `consumer-it` evidence for routed completion duplicates
- [x] 4.4 Run module and root `clean verify`, opt-in profiles, dependency, scope, secret, and diff checks

## 5. Documentation and completion

- [x] 5.1 Update environment examples, Booking/Notification READMEs, root README, runbook, and HANDOFF
- [x] 5.2 Run strict OpenSpec validation and record final evidence before sync/archive
