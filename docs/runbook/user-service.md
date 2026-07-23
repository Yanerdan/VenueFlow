# User Service persistence runbook

This runbook covers the local `persistence` profile of
`venueflow-user-service`.  The default `skeleton` profile and the standard
Maven build do not need MySQL or Docker.

## Scope and ownership

The User Service owns the `venueflow_user` schema and its `user_profile` table.
Do not point it at the Resource Service schema or configure another service to
read this schema directly.  Flyway migration `V001__create_user_profile.sql`
creates tables inside an existing schema; schema creation and database-account
provisioning remain local operator responsibilities.

## Prepare local MySQL

Use a local MySQL server (for example, the project's development Docker
environment) and create an empty schema plus a least-privilege application
account.  The account needs to create and alter the Flyway history table and
the User Service tables within `venueflow_user`; do not use a production root
credential for the service.

Choose a local-only password and keep it out of version control.  The exact
host and grant syntax depend on the MySQL environment, so use your local
administrator workflow rather than copying a shared secret into this document.

## Configure and start

Copy the User Service placeholder values from the repository-root
`.env.example` into your local environment.  The application does not load a
`.env` file automatically, so export the values in the terminal that launches
the service:

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence"
$env:VENUEFLOW_USER_DB_URL = "jdbc:mysql://127.0.0.1:3306/venueflow_user"
$env:VENUEFLOW_USER_DB_USERNAME = "replace-with-local-user-service-db-user"
$env:VENUEFLOW_USER_DB_PASSWORD = "replace-with-local-user-service-db-password"

.\mvnw.cmd -pl venueflow-user-service spring-boot:run
```

On startup Flyway validates and applies `V001__create_user_profile.sql`.  The
configuration disables `flyway clean`, Spring SQL initialization, and
Hibernate schema generation, so migrations are the only schema-management path.

## Verify locally

Confirm that the service has started and that Flyway completed without a
validation error:

```powershell
curl.exe http://localhost:8082/actuator/health
```

Create a test user with the API examples in
[`venueflow-user-service/README.md`](../../venueflow-user-service/README.md),
then read it back and use the returned version for a PATCH request.  A duplicate
`externalUserId` and a stale `expectedVersion` should be rejected.

For an isolated database verification, Docker must be available:

```powershell
.\mvnw.cmd -pl venueflow-user-service -Pmysql-it verify
```

This Testcontainers suite uses its own temporary MySQL instance; it does not
need or modify the local `venueflow_user` schema.

## Troubleshooting

- Missing `VENUEFLOW_USER_DB_*` values: start with the `skeleton` profile, or
  set all three values before activating `persistence`.
- Flyway cannot connect: check the JDBC host, port, schema, and least-privilege
  account; do not switch to root as a permanent workaround.
- Flyway validation fails: inspect the existing schema history and migration
  state. Do not use `flyway clean`; it is intentionally disabled.
- `mysql-it` cannot start: start Docker Desktop, then rerun the optional Maven
  profile.
