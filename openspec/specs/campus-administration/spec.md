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

### Requirement: Managers maintain public resource facts

The management workspace SHALL expose validated editing controls for a resource's core public facts
and SHALL submit the resource version for optimistic concurrency.

#### Scenario: Manager corrects a resource

- **WHEN** a manager edits valid public facts and saves them
- **THEN** the workspace refreshes the catalogue with the returned or subsequently loaded facts

### Requirement: Managers export loaded operations

The management workspace SHALL export its authorized loaded booking records as UTF-8 CSV with
stable column headers and spreadsheet-safe values.

#### Scenario: Loaded bookings are exported

- **WHEN** a manager requests an export
- **THEN** the browser downloads a CSV containing the loaded booking number, activity, resource, applicant, status, quantity, and review facts

### Requirement: Management schedule is date-oriented
The management workspace SHALL group a selected resource's loaded slots by local calendar date and SHALL summarize total, open, and closed counts.

#### Scenario: Operator selects a resource
- **WHEN** Resource Service returns the bounded slot page
- **THEN** the workspace presents date groups and status counts derived from those rows

### Requirement: Operators maintain loaded schedule availability in bulk
The management workspace SHALL let an authorized operator explicitly confirm opening or closing all eligible slots in the selected loaded page and SHALL use each slot's optimistic status API.

#### Scenario: Operator closes loaded open slots
- **WHEN** the operator confirms bulk closure
- **THEN** the workspace closes eligible rows sequentially and refreshes the schedule

#### Scenario: Bulk transition partially fails
- **WHEN** a slot transition fails after earlier transitions succeeded
- **THEN** further transitions stop and the workspace reports the successful count and refreshes authoritative state

### Requirement: Administrators inspect campus integration readiness
The management workspace SHALL show configured identity providers, non-secret readiness, organization synchronization freshness, last outcome, and bounded counts to `SYSTEM_ADMIN` users.

#### Scenario: Administrator opens integration governance
- **WHEN** provider or directory integration is not configured
- **THEN** the workspace shows an actionable unavailable state without exposing secrets

### Requirement: Administrators synchronize and browse organizations
The management workspace SHALL let `SYSTEM_ADMIN` users submit bounded canonical organization synchronization data, inspect run results, and browse the active hierarchy and memberships.

#### Scenario: Administrator imports a directory batch
- **WHEN** a valid uniquely keyed batch succeeds
- **THEN** the workspace refreshes organization counts, hierarchy, and latest run status

### Requirement: Resource managers configure ordered approval policies
The management workspace SHALL let authorized resource managers create or edit one to five ordered approval stages using eligible accounts and assign an active policy to a resource with optimistic concurrency.

#### Scenario: Manager saves three approval stages
- **WHEN** each stage has a label, sequence, and eligible assignee
- **THEN** the workspace displays the persisted three-stage policy in order
