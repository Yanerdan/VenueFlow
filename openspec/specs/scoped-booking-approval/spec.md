# Scoped Booking Approval Specification

## Purpose

Define Booking-owned responsibility snapshots and server-enforced approval scope.

## Requirements

### Requirement: Booking snapshots resource responsibility

Booking Service SHALL snapshot resource ID, owning department, and assigned approver external user
ID for a new reservation from Resource-owned slot facts. Historical bookings without those facts
MUST remain readable.

#### Scenario: A reservation is created for an assigned resource

- **WHEN** Resource returns responsibility for the selected slot
- **THEN** Booking persists and returns the same assignment with the reservation

### Requirement: Approval access is scoped by resource assignment

Booking management listing, approval, and rejection SHALL scope an `APPROVER` to
bookings whose current approval step is assigned to the trusted external user ID. `SYSTEM_ADMIN`
SHALL retain global management access. Other roles SHALL be forbidden.

#### Scenario: Current-stage approver acts

- **WHEN** an approver requests or processes a booking assigned to them at its current stage
- **THEN** Booking permits the scoped operation

#### Scenario: Earlier-stage approver acts again

- **WHEN** an initial approver requests a booking after it advanced to another assigned final approver
- **THEN** Booking returns a bounded forbidden response and preserves the current stage
