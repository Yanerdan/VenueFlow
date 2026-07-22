# C04 verification record

## 5.6 Default Docker-free verification

Command (2026-07-22):

```powershell
$env:DOCKER_HOST = 'npipe:////./pipe/venueflow-docker-disabled'
.\mvnw.cmd clean verify
```

Result: `BUILD SUCCESS` in 1 minute 37 seconds. The default build ran 48 Surefire
tests and 3 pre-existing Failsafe tests. The `ResourceCatalogMysqlSuite` was not
selected, and no Docker/MySQL integration suite was started.

## 5.7 Opt-in real MySQL verification

Command (2026-07-22):

```powershell
.\mvnw.cmd -Pmysql-it clean verify
```

Result: `BUILD SUCCESS` in 2 minutes 45 seconds. Testcontainers connected to Docker
Server 29.6.1 and started an isolated MySQL 8.4.10 container. Flyway reported an
empty `venueflow_resource` schema, applied `V001__init_resource_catalog.sql`, and
then `ResourceCatalogMysqlSuite` completed 5 tests with 0 failures and 0 errors.
The suite covers migration, category/resource creation and retrieval, duplicate
resource number enforcement, category foreign-key enforcement, and optimistic status
transitions.

## Task 6 manual acceptance

On 2026-07-22, Resource Service was started from its executable jar on port 8083.
In default `skeleton` mode, `GET /actuator/health` returned `UP` without a database
connection. In explicit `persistence` mode, it connected to the local
`venueflow_resource` database and the following HTTP checks passed using fresh,
uniquely named Category and Resource data:

- Create Category and Resource; retrieve Resource detail and a filtered first page
  with the documented default size of 20; transition `DRAFT` version 1 to `ACTIVE`
  version 2.
- Verify safe five-field error envelopes for duplicate resource number (409), unknown
  Category (404), unknown Resource (404), oversized page (400), invalid capacity
  (400), invalid status transition (409), and stale version (409).
- Inspect the runtime dependency tree, V001 schema, and controller mappings. Only
  the Resource catalog dependencies, two catalog tables, and the documented catalog
  endpoints were present; no forbidden C04 scope was introduced.
