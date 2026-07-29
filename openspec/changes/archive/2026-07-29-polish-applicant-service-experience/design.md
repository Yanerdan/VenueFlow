## Context

The current zero-build applicant workspace already exercises the backend correctly, but the showcase history belongs to non-login synthetic profiles. A new reviewer therefore sees an empty account, raw slot IDs in history, passive category labels, and a long form with limited selected-slot context. The change spans local provisioning and browser presentation but does not require a backend contract change.

## Goals / Non-Goals

**Goals:**

- Make one click sufficient to enter a credible applicant account with representative personal history.
- Present existing resource, slot, booking, approval, notification, and profile facts in user language.
- Make resource discovery and booking submission feel deliberate, safe, and responsive.
- Keep reseeding idempotent and keep credentials explicitly local-only.

**Non-Goals:**

- Production SSO, password distribution, notification read receipts, favorites, payments, file uploads, maps, or new backend aggregates.
- Inventing live service availability, real people, or institutional claims.

## Decisions

### Provision the applicant through the public Auth API, then bind data in the fixture

`seed.ps1` will idempotently register `campus.user` through Gateway before applying SQL. The SQL fixture can then read the Auth-owned user ID, upsert its User profile, and assign the existing `showcase-applicant-01` booking subset to it. This reuses password hashing and validation in Auth instead of tracking a hash or bypassing the service.

Alternative considered: inserting an Auth credential directly. Rejected because it would couple the fixture to password hashing details and weaken the public-flow demonstration.

### Resolve display facts client-side using existing bounded endpoints

The applicant client will fetch each distinct slot and resource referenced by the current page of bookings, cache those results for the session, and render the resource name, location, and use period. This avoids exposing raw slot IDs while requiring no booking-service denormalization.

Alternative considered: adding resource and time fields to booking responses. Deferred because the existing endpoints already supply the facts and the showcase page is bounded to 50 records.

### Use progressive guidance rather than a new multi-step router

The existing single page remains intact. A compact personal summary, functional category filters, selected-slot summary, profile-completeness notice, button busy states, and post-submit navigation are added with plain HTML/CSS/ES modules. This preserves zero-build delivery and keeps the change reversible.

### Keep synthetic disclosure and local credentials visible

The login helper explicitly labels both applicant and administrator credentials as local demonstration accounts. Historical data remains visibly synthetic after login.

## Risks / Trade-offs

- [Gateway is unavailable while manually reseeding] -> Keep the semester dataset seedable, emit a clear warning, and provision the applicant on the next seed when services are available.
- [Extra detail requests increase page calls] -> Deduplicate slot/resource IDs and cache successful lookups in memory.
- [Demo credentials might be mistaken for production defaults] -> Label them local-only in UI and documentation; do not add them to production configuration.
- [Showcase profile totals differ from login identities] -> Preserve the existing 16 operational personas and document the applicant credential as the login binding for one persona.
