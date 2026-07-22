## Context

C04 established the Resource Service as the owner of a MySQL-backed Category and
Resource catalog. It deliberately excluded `ResourceSlot`, scheduling, allocation, and
Booking. The next vertical slice must add durable, resource-owned time facts while
preserving the service's explicit `persistence` profile, default Docker-free startup,
and narrow dependency boundary.

## Goals / Non-Goals

**Goals:**

- Persist individually created resource slots with a resource reference, UTC time range,
  lifecycle status, optimistic-lock version, and audit timestamps.
- Provide service-owned DTO APIs for slot creation, detail lookup, bounded time-window
  listing, and explicit open/close transitions.
- Prevent invalid or overlapping slots for the same resource and retain a verification
  path against a clean MySQL schema.

**Non-Goals:**

- Booking, approval, capacity allocation or release, occupancy counters, payment,
  cancellation, check-in, notifications, and cross-service orchestration.
- Recurring schedules, time templates, bulk generation, automatic gap filling, or
  calendar UI concerns.
- Authentication/authorization, Redis, messaging, Nacos, Feign, or new runtime
  infrastructure.

## Decisions

### Persist slots in Resource Service with an additive Flyway migration

`V002__add_resource_slots.sql` will create `resource_slot` in `venueflow_resource` and
reference `resource`. It will store `start_at` and `end_at` as UTC `DATETIME(3)`, status
(`OPEN` or `CLOSED`), an optimistic-lock `version`, and audit timestamps. It will use a
unique resource/time-range key and lookup indexes. V001 remains untouched.

The slot is owned by Resource Service because it is a statement of a resource's intended
availability window. A future Booking capability will reference it rather than recreate
time facts. A separate Scheduling service was rejected because no independent scheduling
workflow or cross-service boundary exists yet.

### Use explicit, individually created slots and UTC API values

The write API accepts one slot at a time and requires an ISO-8601 instant with an offset
for both endpoints; the service normalizes the values to UTC. `endAt` must be strictly
after `startAt`. This avoids an implicit local-time zone and keeps the first slice small.
Recurring/template endpoints and date-only semantics are deferred until actual timetable
requirements exist.

### Serialize writes per resource and reject temporal overlap

Creation requires an existing `ACTIVE` resource. Within one transaction, the application
will lock that resource row, then test whether an existing slot overlaps the half-open
interval `[startAt, endAt)` using `existing.startAt < requested.endAt` and
`existing.endAt > requested.startAt`. A conflict is rejected with a stable business
error. The row lock makes the read-then-insert invariant reliable for concurrent writes
without introducing database-specific exclusion constraints.

Slots remain historical facts when the parent Resource later becomes suspended or
archived; this change does not cascade status changes. Future booking eligibility will
require its own explicit rule over both resource and slot state.

### Model availability as an explicit optimistic lifecycle

New slots begin `OPEN`; `PATCH /api/v1/resource-slots/{slotId}/status` accepts a target
status and `expectedVersion`, permits `OPEN <-> CLOSED`, and advances the stored version.
This gives future booking work a stable availability signal without interpreting static
Resource capacity as current availability. Deleting slots was rejected because audit-safe
history and future references are more valuable than premature edit semantics.

### Keep read APIs bounded and error responses consistent

`GET /api/v1/resource-slots/{slotId}` returns one DTO. `GET
/api/v1/resources/{resourceId}/slots` requires a bounded time window, supports paging,
orders by `startAt` then id, defaults to 20 items, and rejects or caps sizes over 100.
Controllers use DTOs and application services only. Slot failures reuse the existing safe
`code`, `message`, `details`, `traceId`, `timestamp` error envelope; no persistence or
secret detail is exposed.

## Risks / Trade-offs

- [Resource-row locking serializes slot creation for one resource] → Slot writes are
  administrative and low-volume; the lock is short and preserves correctness. Revisit
  only if measured scheduling write contention warrants a different model.
- [UTC instants can be less convenient for a future local calendar UI] → Preserve offsets
  at the API boundary and defer venue time-zone policy until a real consumer needs it.
- [No capacity snapshot exists on a slot] → This avoids duplicating the catalog's static
  capacity; a later booking design will explicitly choose whether to reference or snapshot
  capacity.
- [Closed slots are retained] → This makes history and future references safe, at the cost
  of no delete/edit API in this increment.

## Migration Plan

1. Deliver V002 with the application code that consumes it; Flyway applies it before
   persistence-profile traffic is accepted.
2. Deploy Resource Service with the existing explicit persistence settings. Existing V001
   catalog data remains valid because the new table is additive and empty.
3. Roll back application code only before any caller relies on slot endpoints. The schema
   migration is not removed or rewritten; a corrective follow-up requires a new versioned
   migration.

## Open Questions

- None for this increment. Venue-local display time zones, recurring schedules, bulk slot
  generation, and booking capacity semantics are intentionally deferred.
