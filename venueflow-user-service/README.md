# VenueFlow User Service

`venueflow-user-service` is the independently executable User Service for
VenueFlow.  It owns user profile and booking-eligibility state; it does not
share its database with other services.

The service currently supports:

- creating a user with an immutable `externalUserId`;
- reading a user profile;
- updating the display name, account status, and booking eligibility with
  optimistic locking; and
- checking whether the user is currently eligible to make a booking.

There is deliberately **no authentication or authorization** in this stage.
The endpoints below are local development APIs, not a completed identity or
access-control solution.

## Requirements

- JDK 21 (the Maven build enforces it)
- No Docker or database for the default `test` / `verify` build
- An accessible local MySQL instance only when using the `persistence` profile
  or the `mysql-it` verification profile

## Runtime profiles

| Profile | Purpose | Database required |
| --- | --- | --- |
| `skeleton` (default) | Starts the executable service and exposes health endpoints without persistence. | No |
| `persistence` | Enables MySQL, Flyway, and MyBatis-Plus for user profiles. | Yes |

`application-persistence.yml` deliberately requires all three User DB
variables.  It uses Flyway migration `V001__create_user_profile.sql` to create
the `user_profile` table and the Flyway history table.  It does **not** create a
database schema or a database account, and it does not enable Hibernate DDL or
Spring SQL initialization.

Set these local-only environment variables before starting the persistence
profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence"
$env:VENUEFLOW_USER_DB_URL = "jdbc:mysql://127.0.0.1:3306/venueflow_user"
$env:VENUEFLOW_USER_DB_USERNAME = "replace-with-local-user-service-db-user"
$env:VENUEFLOW_USER_DB_PASSWORD = "replace-with-local-user-service-db-password"
```

The repository's [`.env.example`](../.env.example) contains placeholders only.
It is a template, not an automatically imported configuration file; do not
commit a real `.env` file or a real password.

For local schema preparation and operational checks, see
[the User Service runbook](../docs/runbook/user-service.md).

## Run the service

Build the module:

```powershell
.\mvnw.cmd -pl venueflow-user-service clean package
```

Start the default Docker-free skeleton:

```powershell
java -jar venueflow-user-service\target\venueflow-user-service-0.1.0-SNAPSHOT.jar
```

It listens on port `8082` by default.  To start with persistence, set the
variables above first and then run the same JAR command.

## User profile API

All paths are rooted at `http://localhost:8082`.  The following commands use a
local, unauthenticated development service.

Create a user:

```powershell
curl.exe -X POST http://localhost:8082/api/v1/users `
  -H "Content-Type: application/json" `
  -d '{"externalUserId":"employee-001","displayName":"Alice"}'
```

Read a profile:

```powershell
curl.exe http://localhost:8082/api/v1/users/{userId}
```

Update a display name, account status, or booking eligibility.  `expectedVersion`
must be the current version returned by a prior read; a stale value is rejected
instead of silently overwriting a newer update.

```powershell
curl.exe -X PATCH http://localhost:8082/api/v1/users/{userId}/profile `
  -H "Content-Type: application/json" `
  -d '{"displayName":"Alice Chen","expectedVersion":0}'

curl.exe -X PATCH http://localhost:8082/api/v1/users/{userId}/account-status `
  -H "Content-Type: application/json" `
  -d '{"accountStatus":"SUSPENDED","expectedVersion":1}'

curl.exe -X PATCH http://localhost:8082/api/v1/users/{userId}/booking-eligibility `
  -H "Content-Type: application/json" `
  -d '{"bookingEligibility":"INELIGIBLE","expectedVersion":2}'
```

Check booking eligibility:

```powershell
curl.exe http://localhost:8082/api/v1/users/{userId}/booking-eligibility
```

`externalUserId` cannot be changed after creation.  A user is booking-eligible
only when its account status is `ACTIVE` and its booking eligibility is
`ELIGIBLE`.

## Health endpoints

```powershell
curl.exe http://localhost:8082/actuator/health
curl.exe http://localhost:8082/actuator/health/liveness
curl.exe http://localhost:8082/actuator/health/readiness
```

The default profile intentionally reports only the process-level health checks;
it does not test a database connection.

## Verification

The standard module build is Docker-free:

```powershell
.\mvnw.cmd -pl venueflow-user-service clean verify
```

The optional MySQL integration suite uses Testcontainers and therefore requires
Docker to be running:

```powershell
.\mvnw.cmd -pl venueflow-user-service -Pmysql-it verify
```

The `mysql-it` profile verifies migration execution, persistence round-trips,
unique external-user-ID handling, booking-eligibility state, and stale-version
rejection against a real MySQL container.

## Module boundary

The service may depend directly on its executable, web, validation, actuator,
MyBatis-Plus, Flyway, MySQL, and test dependencies.  It must not introduce
authentication/security frameworks, shared user entities, cross-service
database access, service clients, cache or messaging infrastructure, or Booking
domain implementation.  The build enforces these boundaries for direct
dependencies.
