# Booking check-in completion runbook

## Scope

C16 adds a minimal arrival check-in for a confirmed reservation. It does not add QR codes,
authentication, Gateway routing, payment, reviews, Resource writes, Redis, or search.

## API and window

```http
POST /api/v1/bookings/{bookingNo}/check-in
```

Booking first returns an existing `COMPLETED` reservation idempotently. For `CONFIRMED`, it reads
`GET /api/v1/resource-slots/{slotId}` and permits check-in from `startAt - earlyWindow` through
`endAt + lateWindow`. Configure non-negative windows up to 24 hours:

```text
VENUEFLOW_CHECK_IN_EARLY_WINDOW=PT30M
VENUEFLOW_CHECK_IN_LATE_WINDOW=PT30M
```

An unavailable or invalid Resource response leaves Booking unchanged. Outside-window requests
return `BOOKING_CHECK_IN_WINDOW_INVALID`.

## Transaction and delivery

The winning status/version update commits `CONFIRMED -> COMPLETED`, `completed_at`, one status
audit, and one `BOOKING_RESERVATION_COMPLETED` Outbox row in one MySQL transaction. It performs no
capacity write or release. Notification accepts `booking.reservation.completed.v1` through the
existing durable inbox/retry/DLQ path, so repeated delivery creates one notification.

## Verification

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am clean verify
.\mvnw.cmd -pl venueflow-booking-service verify -Pcheckin-it
.\mvnw.cmd -pl venueflow-booking-service verify -Poutbox-it
.\mvnw.cmd -pl venueflow-notification-service verify -Pconsumer-it
```

Default verification is Docker-free. `checkin-it` uses MySQL 8.4.10 and a bounded local HTTP
stub. Rollback keeps additive V005/V003 migrations and deploys the prior binaries; older code
ignores the nullable completion field and does not emit the new route.
