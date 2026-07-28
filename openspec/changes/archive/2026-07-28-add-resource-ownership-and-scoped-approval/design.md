## Context

Resource currently owns catalog facts but no organizational responsibility. Booking stores only a
slot identifier, and every approver receives the same global queue. Gateway already forwards
trusted credential identity and role headers, so scope can be enforced without a new identity
system.

## Goals / Non-Goals

**Goals:**

- Configure one owning department and one assigned approver for a resource.
- Snapshot that assignment when a booking is created.
- Enforce assigned approval reads and actions in Booking.
- Keep historical bookings readable and system administrators unrestricted.

**Non-Goals:**

- A general workflow/BPM engine, delegation calendars, parallel approvals, or organization trees.
- Cross-service database access or a new infrastructure component.

## Decisions

- Resource adds nullable `owner_department` and string `approver_external_user_id` columns through
  additive V005/V006 migrations.
  Nullable fields preserve existing catalog rows; an unassigned resource routes only to system
  administrators.
- Resource slot detail includes the parent resource ownership. Booking reads that bounded DTO
  during creation and stores `resource_id`, `owner_department`, and
  `assigned_approver_external_user_id` through Booking V007.
- Booking management endpoints accept trusted `X-User-Id` and `X-Role`. `SYSTEM_ADMIN` sees all;
  `APPROVER` sees and acts only on matching assigned bookings. Historical unassigned bookings stay
  system-admin-only.
- This is the first useful approval level. A later change can add a second stage without changing
  the ownership contract.

## Risks / Trade-offs

- [Approver identifiers become stale after account replacement] → ownership remains explicitly
  editable and bookings retain an auditable snapshot.
- [Extra Resource read during creation] → reuse the existing bounded slot collaborator read and
  avoid any shared storage.
- [Existing rows are unassigned] → system administrators can configure them incrementally.

## Migration Plan

Apply Resource V005/V006, configure ownership, then apply Booking V007/V008. The follow-up
migrations align approver identity with Gateway's UUID subject. Older clients and historical
records remain readable because all fields are additive.

## Open Questions

None for this bounded milestone.
