## Context

The backend owns the full reservation lifecycle but lacks user-facing aggregate reads and a Web
client. Auth subjects are UUID external identities, while User owns the internal numeric identity
used by Booking and Notification. The smallest complete flow therefore needs one trusted
current-profile lookup before history and inbox calls.

## Goals / Non-Goals

**Goals:**

- Complete registration/login through booking history and notification viewing.
- Reuse existing schemas and domain facts with bounded newest-first reads.
- Keep the Web client dependency-free and directly runnable from static files.
- Preserve connection-free Maven defaults and deterministic tests.

**Non-Goals:**

- Security hardening, roles/admin authorization, reviews, pressure testing, real-user validation,
  production application images, SSR, design-system dependencies, or release packaging.

## Decisions

1. User adds `GET /api/v1/users/me`, resolving the trusted `X-User-Id` UUID against the existing
   external ID index. It does not create a second identity store.
2. Booking adds `GET /api/v1/bookings?userId=&pageNumber=&pageSize=`. MyBatis-Plus performs a
   newest-first bounded query; responses contain page metadata and existing DTOs.
3. Notification adds `GET /api/v1/notifications?userId=&pageNumber=&pageSize=` using its existing
   JDBC table and index. No read-status migration is added.
4. Gateway adds only explicit Notification static/governed routes and authenticates Search and
   Notification alongside existing business paths.
5. `venueflow-web` uses browser-native HTML/CSS/ES modules. A tiny API adapter owns tokens,
   refresh-once behavior, UUID idempotency keys, envelopes, and errors. Node's built-in test runner
   validates the adapter without package installation.
6. The UI is a functional single-page shell rather than a styling framework: authentication,
   resource/slot selection, booking actions, history, and inbox are the only screens.

## Risks / Trade-offs

- [Numeric user ID remains visible to the client] -> `/me` derives it from the trusted external
  identity; later authorization hardening can move ownership checks into services.
- [Static UI has less component structure] -> ES modules and small render functions keep it
  maintainable without delaying the application.
- [Notification has no unread state] -> list-only behavior matches current schema and avoids a
  migration unrelated to completing the viewing flow.
- [Backend services are not all containerized] -> the runbook starts only required profiles from
  built JARs; deployment packaging stays deferred.

## Migration Plan

Deploy backward-compatible read endpoints and Gateway route, then serve the static Web directory.
Rollback removes the route/UI and read methods; no schema rollback or data conversion is needed.

## Open Questions

Role-aware administration and service-side ownership enforcement remain deferred to the later
security work explicitly excluded by the user.
