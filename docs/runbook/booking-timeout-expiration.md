# Booking timeout expiration runbook

## Runtime boundary

Creation under `persistence` returns `PENDING_CONFIRMATION` and `expireAt`. Confirmation uses
`POST /api/v1/bookings/{bookingNo}/confirmation`. Automatic timeout work exists only with
`SPRING_PROFILES_ACTIVE=persistence,expiration` and is disabled by default.

## Preview and one bounded run

Set `venueflow.booking.expiration.admin.action=PREVIEW` to log booking number and attempt metadata
without claiming work. For one bounded run, set action `RUN`, a reason of at most 256 characters,
and `venueflow.booking.expiration.admin.confirm=true`.

## Safety and recovery

The worker claims due pending rows with an expiring lease. It checks the deterministic Resource
release operation first, performs at most one idempotent release when absence is definitive, and
commits `EXPIRED` only after release is proven. Unknown or conflicting outcomes retain retry facts;
expired leases are reclaimable. Confirmation, cancellation, and expiration use status/version and
lease predicates so one path wins.

## Rollout and rollback

Deploy Notification expiration routing first, then Booking V004 with expiration disabled. Verify
preview and enable a small batch while monitoring due age, retries, mismatches, releases, Outbox,
and notification consumption. To stop, disable new scans and wait for leases to expire. Migrations
are immutable; do not roll back the binary while pending reservations remain.
