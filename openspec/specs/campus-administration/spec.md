# Campus Administration Specification

## Purpose

Define the bounded campus roles, school management workspace, booking approval operations, and
resource administration foundation.

## Requirements

### Requirement: Campus identities use bounded roles

The system SHALL support `APPLICANT`, `APPROVER`, `RESOURCE_MANAGER`, and `SYSTEM_ADMIN` campus
roles. New self-registered credentials MUST default to `APPLICANT`, and management actions MUST
accept only the bounded management roles defined for that action.

#### Scenario: A public user registers

- **WHEN** a new credential is created through public registration
- **THEN** its role is `APPLICANT`
- **AND** the caller cannot select a management role

### Requirement: Management workspace presents live operations

The Web application SHALL provide a directly runnable campus management workspace that reads live
Gateway APIs and presents bounded overview counts, pending approvals, booking records, resources,
and resource slots without embedding credentials or mock business results.

#### Scenario: A manager opens the workspace

- **WHEN** an authenticated management user opens the administration page
- **THEN** current resource and booking facts are loaded through Gateway
- **AND** failures remain visible instead of being represented as successful data

### Requirement: Managers operate the existing reservation lifecycle

An authorized manager SHALL be able to approve a pending application, reject or cancel an active
application, and check in an eligible confirmed booking from the management workspace. These
actions MUST reuse Booking-owned lifecycle transitions and stable response envelopes.

#### Scenario: A pending application is approved

- **WHEN** an authorized manager confirms a `PENDING_CONFIRMATION` booking
- **THEN** the booking becomes `CONFIRMED`
- **AND** the administration list refreshes from the server result

### Requirement: Resource operators maintain campus inventory

An authorized resource operator SHALL be able to create categories, resources, and resource
slots and change their supported statuses through the management workspace using Resource-owned
APIs.

#### Scenario: An operator publishes a reservable slot

- **WHEN** an authorized operator creates a valid slot for an active resource
- **THEN** Resource Service persists the slot
- **AND** the management workspace shows the new server-owned record

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
