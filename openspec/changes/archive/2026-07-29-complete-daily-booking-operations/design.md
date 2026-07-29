## Context

The current dependency-free web application exposes complete applicant and management journeys, while Resource Service already owns versioned catalog and slot mutations. Daily friction comes from narrow UI operations rather than a missing architectural layer. The change must remain executable with the existing static frontend and current MySQL schemas.

## Goals / Non-Goals

**Goals:**

- Make availability discovery and application follow-up faster for applicants.
- Let managers correct public resource facts and publish repeated opening hours efficiently.
- Preserve optimistic concurrency and existing service ownership.
- Keep all additions bounded, dependency-free, and easy to verify.

**Non-Goals:**

- Formal SSO, organization synchronization, arbitrary approval graphs, attachments, security hardening, load testing, or release packaging.
- A new calendar service, notification write model, or server-side export subsystem.
- Database migrations or changes to booking lifecycle states.

## Decisions

1. Resource core-fact editing uses a dedicated versioned PATCH operation in Resource Service. This matches existing status, ownership, and rules mutations and prevents lost updates. Reusing resource creation or allowing unrestricted partial maps was rejected because both weaken validation and API clarity.
2. Recurring slot publication is orchestrated by the management browser as a bounded sequence of existing slot-create calls. This avoids a new distributed batch contract; the UI reports partial failure and caps the generated occurrences.
3. Applicant discovery combines server-owned resource/search reads with browser-side capacity, category, and date filtering. This is sufficient for the current bounded catalogue and avoids coupling Search Service to live slot availability.
4. Drafts and inbox read markers use namespaced local storage. They are comfort features rather than authoritative domain facts, remain scoped to the signed-in identity, and require no notification schema migration.
5. CSV export is generated from already authorized management reads in the browser. This avoids a duplicate reporting API while keeping spreadsheet compatibility.
6. Public catalogue hygiene is enforced by requiring active resources to have a usable name, location, capacity, and responsible department before display.

## Risks / Trade-offs

- [Recurring creation can partially succeed] → Cap occurrences, publish sequentially, keep successful rows, and report the failed occurrence.
- [Date filtering performs additional slot reads] → Only fetch after a date filter is supplied and use the small existing resource page.
- [Local read/draft state does not roam across devices] → Label it as browser-local convenience and keep authoritative booking facts server-side.
- [CSV reflects the loaded page] → Name the export accordingly and include explicit column headers.
- [Legacy active resources may disappear from applicants] → Keep them visible to managers so their facts can be corrected and republished.

## Migration Plan

No schema migration is required. Deploy Resource Service before the updated management UI. Rolling back the frontend removes the convenience controls; rolling back Resource Service only disables core-fact editing and does not invalidate stored data.

## Open Questions

None for this bounded increment.
