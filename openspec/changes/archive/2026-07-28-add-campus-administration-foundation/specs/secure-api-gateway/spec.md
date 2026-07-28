## MODIFIED Requirements

### Requirement: Gateway exposes only explicit bounded routes

The explicit `gateway` profile SHALL route `/api/v1/auth/**`, `/api/v1/users/**`,
`/api/v1/resources/**`, `/api/v1/resource-categories/**`, `/api/v1/resource-slots/**`,
`/api/v1/bookings/**`, `/api/v1/search/**`, and `/api/v1/notifications/**` to separately
configured bounded HTTP base URIs. When `governance` is also active, the same allowlist SHALL
resolve only the configured Auth, User, Resource, Booking, Search, and Notification service
identities through Spring Cloud LoadBalancer. Search, Notification, and Resource management
routes MUST be authenticated business routes. Gateway MUST NOT enable discovery locator, infer
routes, expose infrastructure admin paths, or retry writes automatically.

#### Scenario: A known path is requested

- **WHEN** a request matches one configured prefix
- **THEN** Gateway proxies it only to that route's configured static URI or governance service
  identity

#### Scenario: An unknown path is requested

- **WHEN** a request matches no explicit route or health endpoint
- **THEN** Gateway returns a safe non-success response without a downstream call

### Requirement: Gateway owns trusted identity and trace headers

Gateway MUST remove every client-supplied `X-User-Id` and `X-Role` before routing. For an
authenticated request it SHALL set exactly one `X-User-Id` from JWT `sub` and exactly one
`X-Role` from a supported JWT campus-role claim, defaulting a missing legacy claim to
`APPLICANT`. It SHALL accept only a bounded UUID `X-Trace-Id`, otherwise generate one, propagate
it downstream, return it in the response, and include it in safe error envelopes.

#### Scenario: A caller forges identity headers

- **WHEN** a valid token request also supplies forged user and role headers
- **THEN** downstream receives only the token subject as `X-User-Id`
- **AND** receives only the verified token role as `X-Role`
