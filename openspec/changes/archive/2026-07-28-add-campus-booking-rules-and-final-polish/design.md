## Context

VenueFlow already supports resource catalog management, slot publication, reservation, configurable approvals, notifications, and role administration. The remaining product gap is a small set of school-facing booking constraints and a clearer role-specific management workspace. These changes must preserve the current service boundaries and remain simple enough for local deployment and demonstration.

## Goals / Non-Goals

**Goals**

- Let resource managers configure a notice and bounded booking-time rules per resource.
- Reject reservations that violate those rules before any capacity is allocated.
- Present the same rules to applicants and management users.
- Hide management sections and actions that are irrelevant to the signed-in role.
- Extend automated and persistence-mode acceptance coverage.

**Non-Goals**

- A general-purpose policy engine, holiday calendar, blacklist, or recurring blackout model.
- Organization synchronization, SSO, production hardening, load testing, or release packaging.
- Replacing existing resource suspension and slot closure mechanisms.

## Decisions

### Store rules with the resource aggregate

Four columns are added to the resource table: booking notice, minimum advance hours, maximum advance days, and maximum duration minutes. Existing resources receive permissive defaults (`0`, `90`, and `480`) so the migration does not interrupt current flows.

This keeps ownership, approval, and booking policies together and avoids introducing another service or table.

### Use a focused optimistic update endpoint

Resource managers and system administrators update booking rules through a dedicated resource endpoint with `expectedVersion`. Validation is performed in the Resource service and follows the existing optimistic-concurrency behavior.

### Propagate rules through slot collaboration

The Resource service includes the owning resource's rules in slot detail responses. Booking consumes that existing collaboration response and validates:

- the slot starts after the minimum advance interval;
- the slot starts within the maximum advance window;
- the slot duration does not exceed the maximum duration.

Validation occurs before capacity allocation. The Booking service uses its injected clock so tests remain deterministic.

### Keep the web experience role-driven and compact

Applicant resource cards display the notice and readable rule summary. The management workspace exposes rule editing only to resource managers and system administrators, and shows navigation sections according to the current role. Existing pages and components are reused.

## Risks / Trade-offs

- Rules are evaluated when a booking is submitted rather than snapshotted into a separate policy history. Existing bookings remain valid after a rule change, which is intentional for this phase.
- Day/hour limits use exact elapsed time from the application clock; this is simpler than campus-calendar semantics.
- Hiding unauthorized UI improves clarity but is not an authorization boundary. Existing backend role checks remain authoritative.
