## Why

VenueFlow's workflows are complete, but a fresh local environment still looks like an engineering demo rather than a campus platform that has accumulated a semester of operational history. A repeatable synthetic showcase dataset will make product review, screenshots, and stakeholder demonstrations immediately credible.

## What Changes

- Replace the small English venue seed with a representative Chinese university resource catalog spanning teaching, meeting, student activity, sports, and public-service spaces.
- Add synthetic campus profiles across multiple schools and administrative departments.
- Add an idempotent semester history of completed, confirmed, pending, rejected, cancelled, and expired applications with approval actions and notifications.
- Seed future open slots and realistic resource-level booking notices, ownership, approval modes, and time limits.
- Add a compact operations identity and demo-data disclosure to the web and documentation.
- Preserve user-created records; only rows with reserved showcase identifiers are replaced on reseed.

## Capabilities

### New Capabilities

- `semester-showcase-data`: Repeatable, clearly synthetic campus operations data for local demonstrations and acceptance.

### Modified Capabilities

- `web-application`: The local showcase identifies itself as a semester operations environment and presents realistic seeded content without claiming that records belong to real people.

## Impact

- Local development seed script and a versioned SQL showcase fixture.
- Resource, User, Booking, and Notification local databases only.
- Applicant and management web copy, README, and campus administration runbook.
- No production migration, external dependency, or API contract change.
