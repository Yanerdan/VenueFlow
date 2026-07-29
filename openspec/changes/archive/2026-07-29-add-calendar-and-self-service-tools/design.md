## Context

C35 added date-first discovery, reusable application facts, recurring slot creation, and focused resource administration. C36 builds personal and schedule-management shortcuts entirely on existing browser-visible facts and optimistic APIs.

## Goals / Non-Goals

**Goals:**

- Reduce repeated navigation for applicants who reuse the same spaces.
- Make approved bookings portable to common calendar applications.
- Provide an honest, guided reschedule workflow using existing cancellation and creation semantics.
- Make a resource's loaded opening schedule easier to scan and maintain.

**Non-Goals:**

- Server-synchronized favorites, calendar provider integrations, or an atomic reschedule domain operation.
- A new batch Resource API, database migration, or background maintenance scheduler.
- Security, load, release, or real-user trial work.

## Decisions

1. Favorites use identity-scoped local storage and store resource IDs only. The catalogue remains server authoritative, and stale favorite IDs disappear naturally when resources are unavailable.
2. Calendar export is generated as UTF-8 RFC 5545-compatible `.ics` text from a confirmed booking's resolved resource and slot facts. Direct Google/Microsoft calendar integrations were rejected because they introduce external identity and privacy scope.
3. Rescheduling first requires explicit user confirmation, then invokes the existing cancellation action, stores reusable fields as a draft, and opens the same resource's current slots. It does not claim atomicity; failed cancellation leaves the original booking unchanged.
4. Bulk slot operations run sequentially over the currently loaded rows, use each row's version, stop on the first failure, and report partial progress. A new server batch endpoint was rejected for this bounded catalogue.
5. Schedule grouping and counts are derived from loaded slot DTOs and never invent availability beyond Resource Service state.

## Risks / Trade-offs

- [Favorites do not roam across devices] → Present them as browser-local convenience.
- [Rescheduling temporarily releases capacity before a replacement is chosen] → Require explicit confirmation and describe the behavior in the dialog.
- [Bulk updates can partially succeed] → Stop on failure, preserve successes, and refresh from the server.
- [Calendar clients vary] → Emit conservative UTC timestamps, escaped text, stable UID, and CRLF line endings.

## Migration Plan

No migration is needed. The browser assets can be rolled back independently without changing stored business facts.

## Open Questions

None.
