## Why

VenueFlow currently gives every approver a global queue and does not identify which school unit
owns a resource. A usable campus administration product needs clear responsibility and must route
applications to the staff member accountable for the requested space.

## What Changes

- Add bounded owning-department and assigned-approver facts to each resource.
- Let resource managers update ownership with optimistic locking.
- Snapshot resource ownership onto each new booking so historical routing remains stable.
- Restrict approver queues and approval actions to the assigned approver while retaining global
  access for system administrators.
- Show ownership in resource administration and assigned responsibility in the review workspace.

## Capabilities

### New Capabilities

- `resource-ownership`: Resource-owned department and assigned-approver configuration.
- `scoped-booking-approval`: Booking-owned assignment snapshots and server-enforced approver scope.

### Modified Capabilities

- `campus-administration`: Resource configuration and approval queues become responsibility-aware.
- `web-application`: The management workspace edits ownership and presents scoped assignments.

## Impact

Resource and Booking schemas receive additive migrations and bounded DTO fields. Resource slot
metadata, Booking management queries/actions, local smoke coverage, and the zero-build management
workspace are updated. No workflow engine, shared database access, or new infrastructure is added.
