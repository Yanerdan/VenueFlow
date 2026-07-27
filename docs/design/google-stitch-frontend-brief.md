# VenueFlow — Google Stitch frontend brief

Design and generate a production-quality responsive web application for **VenueFlow**, a venue
and shared-resource reservation platform. The visual design should feel calm, capable, modern,
and trustworthy rather than corporate or generic.

## Product context

VenueFlow lets a user:

1. register and sign in;
2. resolve or create their profile;
3. discover and search venues/resources;
4. inspect available time slots;
5. create a pending reservation for a quantity of people;
6. confirm, cancel, or check in to a reservation when the current state permits;
7. review booking history;
8. read booking notifications;
9. refresh an expired session once and sign out.

The backend already exists. The frontend must integrate only through a configurable API Gateway,
defaulting locally to `http://127.0.0.1:8080`. Do not invent a separate backend, Firebase,
Supabase, mock database, payment flow, admin console, social login, or third-party runtime.

## Required pages and states

Create a coherent application with these responsive experiences:

- **Authentication landing**
  - strong product introduction;
  - login and registration modes;
  - registration fields: display name, username, password;
  - clear validation and server-error presentation;
  - configurable Gateway URL in a low-prominence developer setting.
- **Authenticated application shell**
  - product mark, user identity, primary navigation, logout;
  - desktop and mobile navigation behavior;
  - discover, bookings, and notifications destinations.
- **Discover**
  - prominent search;
  - resource cards showing name, location, description, capacity, and availability/status;
  - loading, empty, error, and populated states;
  - selected-resource detail with available slots;
  - quantity selection and booking confirmation.
- **Booking history**
  - newest-first booking list;
  - booking number, slot, quantity, state, and important timestamps;
  - state-aware actions:
    - `PENDING_CONFIRMATION`: confirm or cancel;
    - `CONFIRMED`: check in or cancel;
    - terminal states: no invalid actions;
  - distinguish pending, confirmed, cancelled, expired, and completed without relying only on
    color.
- **Notification inbox**
  - newest-first notification list;
  - title, body, event type, booking reference, and timestamp;
  - empty and loading states.
- **System feedback**
  - inline field errors;
  - visible API errors containing a safe message and optional trace ID;
  - non-blocking success feedback;
  - skeleton/loading states;
  - session-expired state that returns the user to login.

## Visual direction

Avoid a generic dashboard template. Use editorial hospitality aesthetics:

- warm off-white or mineral background;
- deep ink text;
- one sophisticated evergreen/teal primary accent;
- restrained warm accent for important moments;
- generous space, strong typographic hierarchy, subtle borders, and low-elevation surfaces;
- venue imagery only where it improves discovery; provide graceful image-free fallbacks;
- cards should feel like venue listings rather than SaaS statistics;
- subtle motion for navigation, selection, loading, and state changes;
- accessible focus indicators and readable contrast;
- responsive from 360 px mobile through wide desktop;
- use a distinctive but practical Google Font pairing if the generated solution can load it
  gracefully; otherwise use robust system fallbacks.

Do not use excessive gradients, glassmorphism, neon colors, dense charts, metric tiles, huge
rounded pills, or decorative UI that obscures actions.

## Existing API contract

All browser requests go to the configured Gateway origin:

- `POST /api/v1/auth/register`
  - body: `{ "username": string, "password": string }`
  - returns envelope data `{ "userId": UUID, "username": string }`
- `POST /api/v1/auth/login`
  - body: `{ "username": string, "password": string }`
  - returns envelope data with `accessToken`, `refreshToken`, `tokenType`, `expiresInSeconds`
- `POST /api/v1/auth/refresh`
  - body: `{ "refreshToken": string }`
- `POST /api/v1/auth/logout`
  - body: `{ "refreshToken": string }`
- `GET /api/v1/users/me`
  - authenticated; Gateway derives identity from JWT
- `POST /api/v1/users`
  - body: `{ "externalUserId": UUID string, "displayName": string }`
- `GET /api/v1/resources?page=0&size=50`
- `GET /api/v1/search/resources?text=...&page=0&size=50`
- `GET /api/v1/resources/{resourceId}/slots?from={ISO instant}&to={ISO instant}&page=0&size=50`
- `POST /api/v1/bookings`
  - header: `Idempotency-Key: {UUID}`
  - body: `{ "userId": number, "slotId": number, "quantity": number }`
- `GET /api/v1/bookings?userId={id}&pageNumber=0&pageSize=50`
- `POST /api/v1/bookings/{bookingNo}/confirmation`
- `POST /api/v1/bookings/{bookingNo}/cancellation`
- `POST /api/v1/bookings/{bookingNo}/check-in`
- `GET /api/v1/notifications?userId={id}&pageNumber=0&pageSize=50`

Authenticated requests use `Authorization: Bearer {accessToken}`. Access and refresh tokens remain
in `sessionStorage`, never URLs or logs. After one `401`, refresh once and retry once; clear the
session if refresh fails. Successful APIs may return either a direct DTO or a
`{ code, message, data, traceId }` envelope. Errors expose safe `message`, `code`, and `traceId`.

## Implementation constraints

The repository currently contains a dependency-free `venueflow-web` application using native
HTML, CSS, and ES modules. Prefer generating a polished implementation that can replace or
incrementally improve it while retaining:

- no mandatory package installation;
- no build step;
- no runtime framework dependency;
- semantic HTML and keyboard accessibility;
- modular `src/api.js` and `src/app.js`;
- safe rendering of all server-provided text;
- deterministic Node built-in tests with mocked `fetch`;
- no tracked credential or environment-specific production host.

If Stitch cannot emit framework-free ES modules cleanly, prioritize the complete visual system,
page layouts, component states, responsive behavior, and design tokens. Codex will adapt the
result into the existing native application and wire the real APIs.

## Requested Stitch output

Produce:

1. a complete high-fidelity desktop design;
2. the corresponding mobile design;
3. all required populated, loading, empty, error, and terminal booking states;
4. a reusable visual system: colors, typography, spacing, radii, shadows, icons, and motion;
5. implementation-ready HTML/CSS/component code or exportable design/code artifacts;
6. no invented backend behavior.
