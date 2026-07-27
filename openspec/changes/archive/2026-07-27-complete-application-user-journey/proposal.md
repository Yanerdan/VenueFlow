## Why

VenueFlow's core service chain is implemented, but it is still a collection of APIs rather than a
complete usable application: users cannot list their booking history or inbox, Notification is not
routed through Gateway, and no Web client joins the workflow. This change closes those functional
gaps while deliberately postponing pressure, security-hardening, real-user, and release work.

## What Changes

- Add bounded Booking history queries by user, newest first.
- Add a read-only, bounded Notification inbox API over existing notification records.
- Add a current-user profile lookup from Gateway's trusted external identity header.
- Route and authenticate Search and Notification paths through Gateway.
- Add a dependency-free browser application for registration/login, resource discovery, slot
  selection, booking lifecycle, booking history, and notifications.
- Add deterministic backend and browser tests plus a minimal local application runbook.

## Capabilities

### New Capabilities

- `web-application`: browser-facing application shell, session handling, API adapter, user journey,
  and deterministic frontend tests.

### Modified Capabilities

- `booking-reservation-management`: add bounded per-user booking history.
- `user-profile-management`: add current-user profile lookup by external identity.
- `reliable-notification-consumption`: expose existing in-app notification records as a bounded
  read-only inbox.
- `secure-api-gateway`: add explicit authenticated Search and Notification routing.

## Impact

Booking persistence/application/web code, Notification read-side code, Gateway routes/config/tests,
the new `venueflow-web` directory, environment examples, README, and runbook are affected. No
existing migration, event contract, write state machine, external dependency, or infrastructure
service changes.
