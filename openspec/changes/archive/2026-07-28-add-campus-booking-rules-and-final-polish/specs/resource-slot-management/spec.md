## ADDED Requirements

### Requirement: Slot detail carries resource booking rules

The Resource service SHALL include the owning resource's booking notice and time limits in the slot-detail collaboration response.

#### Scenario: Booking service retrieves a slot

- **WHEN** the Booking service retrieves an existing slot
- **THEN** the response includes the slot interval, approval policy, and current resource booking rules
