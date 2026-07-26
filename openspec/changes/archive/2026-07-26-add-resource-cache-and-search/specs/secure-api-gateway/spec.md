## MODIFIED Requirements

### Requirement: Gateway exposes only explicit bounded routes

The explicit `gateway` profile SHALL route `/api/v1/auth/**`, `/api/v1/users/**`,
`/api/v1/resources/**`, `/api/v1/bookings/**`, and `/api/v1/search/**` to separately configured
bounded HTTP base URIs. When `governance` is also active, the same allowlist SHALL resolve only the
configured Auth, User, Resource, Booking, and Search service identities through Spring Cloud
LoadBalancer. It MUST NOT enable discovery locator, infer routes, expose admin paths, or retry
writes automatically.

#### Scenario: A known path is requested

- **WHEN** a request matches one configured prefix
- **THEN** Gateway proxies it only to that route's configured static URI or governance service
  identity

#### Scenario: An unknown path is requested

- **WHEN** a request matches no explicit route or health endpoint
- **THEN** Gateway returns a safe non-success response without a downstream call
