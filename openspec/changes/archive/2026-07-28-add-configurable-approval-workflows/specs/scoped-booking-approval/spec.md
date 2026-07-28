## MODIFIED Requirements

### Requirement: Approval access is scoped by resource assignment

Booking management listing, approval, and rejection SHALL scope an `APPROVER` to bookings whose current approval step is assigned to the trusted external user ID. `SYSTEM_ADMIN` SHALL retain global management access. Other roles SHALL be forbidden.

#### Scenario: Current-stage approver acts
- **WHEN** an approver requests or processes a booking assigned to them at its current stage
- **THEN** Booking permits the scoped operation

#### Scenario: Earlier-stage approver acts again
- **WHEN** an initial approver requests a booking after it advanced to another assigned final approver
- **THEN** Booking returns a bounded forbidden response and preserves the current stage
