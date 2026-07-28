## Context

The local seed currently creates three generic English venues and future slots. It does not populate representative campus users, historical reservations, approval actions, notifications, or reporting distributions. The showcase must be repeatable on an existing local database without deleting user-created records or becoming a production migration.

## Goals / Non-Goals

**Goals:**

- Produce a credible semester of synthetic campus operations in one local seed command.
- Cover multiple departments, resource types, statuses, approval outcomes, and dates.
- Keep showcase rows recognizable and safely replaceable through reserved identifiers.
- Make applicant and management views immediately useful after startup.

**Non-Goals:**

- Fabricating real institutional performance claims or real personal records.
- Migrating showcase data into production environments.
- Resetting Docker volumes or deleting records outside the reserved showcase namespace.
- Adding a general fixture framework or changing public APIs.

## Decisions

### Use a versioned SQL fixture for historical facts

Service APIs correctly own live workflows but intentionally do not permit arbitrary historical timestamps. A local-only SQL fixture will insert historical reservations, approval actions, profiles, and notifications after Flyway has completed. The existing API seed remains responsible for future slots and search rebuild.

### Reserve stable showcase identifiers

Synthetic profiles use `showcase-*` external IDs, resources use `VF-CAMPUS-*`, and bookings use `VF-SHOW-*`. Reseeding deletes and recreates only dependent rows under those identifiers. This provides idempotency without affecting ad hoc user data.

### Keep all records visibly synthetic

Names are plausible but generic, contact values use reserved example ranges/domains, and documentation states that the dataset is synthetic. The interface presents operational history without claiming a real university deployment.

### Model a balanced semester

The fixture spans roughly 120 days and includes completed, confirmed, pending, rejected/cancelled, and expired applications. Resource and department distributions are deliberately non-uniform so reports look realistic rather than mechanically generated.

## Risks / Trade-offs

- [Risk] Direct SQL can drift from schema migrations. → Keep the fixture in local tooling, validate it through startup smoke, and use explicit column lists.
- [Risk] Repeated startup could duplicate history. → Delete only reserved showcase rows before reinserting stable records.
- [Risk] Synthetic data could be mistaken for real usage. → Add a visible “演示数据” disclosure and document reserved identifiers.
- [Risk] Search projection may lag after direct resource inserts. → Trigger the existing search rebuild after seeding.
