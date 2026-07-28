## ADDED Requirements

### Requirement: Campus administration exposes resource responsibility

The management workspace SHALL let authorized resource operators view and update a resource's
owning department and assigned approver using Resource-owned optimistic APIs.

#### Scenario: An operator updates an active resource

- **WHEN** the operator saves a bounded department and approver assignment
- **THEN** the workspace refreshes and displays the persisted responsibility

### Requirement: Approval workspace identifies assigned responsibility

The management workspace SHALL display each booking's owning department and assigned approver and
SHALL rely on Booking to return only work authorized for the current management identity.

#### Scenario: An approver opens the queue

- **WHEN** the workspace loads management bookings
- **THEN** visible rows identify their assigned department and approver responsibility
