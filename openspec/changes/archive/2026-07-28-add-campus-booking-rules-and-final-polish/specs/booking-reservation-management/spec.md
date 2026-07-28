## ADDED Requirements

### Requirement: Reservation creation applies resource time limits

The Booking service SHALL validate the selected slot using the current resource booking rules before requesting capacity allocation.

#### Scenario: Reservation violates a time limit

- **WHEN** a reservation targets a slot outside the allowed advance window or exceeding the duration limit
- **THEN** the Booking service returns a stable validation error and makes no allocation request
