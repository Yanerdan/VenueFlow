# VenueFlow User Service

`venueflow-user-service` is an independently executable Spring Boot MVC
service skeleton for the VenueFlow platform.

This module currently establishes the User Service boundary, executable
artifact, minimal HTTP runtime, health probes, dependency restrictions,
and Docker-free verification baseline.

It does not yet implement user-domain business capabilities.

## Requirements

- JDK 21
- Maven Wrapper included in this repository
- No Docker
- No database
- No external infrastructure services

## Module boundary

The module directly depends only on:

- Spring Boot Web MVC
- Spring Boot Actuator
- Spring Boot test support
- Spring MVC test support

The Maven build rejects dependencies associated with:

- persistence frameworks and database drivers
- schema migration tools
- Spring Security and token libraries
- Spring Cloud, Nacos, Sentinel, and Feign
- Redis and distributed caching clients
- RabbitMQ and Kafka
- tracing and Prometheus exporters
- Elasticsearch and OpenSearch
- Testcontainers
- other VenueFlow business-service modules

## Configuration

The application name is:

```text
venueflow-user-service
```

The default Spring profile is:

```text
skeleton
```

The default HTTP port is:

```text
8082
```

The port is configured in `application.yml` as:

```yaml
server:
  port: ${SERVER_PORT:8082}
```

When `SERVER_PORT` is not set, the application listens on port `8082`.

When `SERVER_PORT` is set, its value overrides the default port.

The default `skeleton` profile contains no:

- datasource configuration
- database credentials
- external configuration imports
- service-discovery configuration
- infrastructure-client configuration
- tracked secrets

## Build

Run the following command from the repository root:

```powershell
.\mvnw.cmd `
  -pl venueflow-user-service `
  -am `
  clean verify
```

The build must complete without Docker, a database, or any external
infrastructure service.

Expected result:

```text
VenueFlow Parent ............... SUCCESS
VenueFlow User Service ......... SUCCESS
BUILD SUCCESS
```

## Package

Build the executable Spring Boot JAR from the repository root:

```powershell
.\mvnw.cmd `
  -pl venueflow-user-service `
  -am `
  package
```

The generated artifact is:

```text
venueflow-user-service/target/venueflow-user-service-0.1.0-SNAPSHOT.jar
```

Its Spring Boot start class must be:

```text
com.yanerdan.venueflow.user.UserServiceApplication
```

## Run on the default port

Start the packaged application from the repository root:

```powershell
java -jar `
  .\venueflow-user-service\target\venueflow-user-service-0.1.0-SNAPSHOT.jar
```

The application should start with the default `skeleton` profile and
listen on port `8082`.

Expected startup information includes:

```text
Started UserServiceApplication
Tomcat started on port 8082
```

Stop the application with:

```text
Ctrl+C
```

## Override the port

Set `SERVER_PORT` before starting the application:

```powershell
$env:SERVER_PORT = "18082"

java -jar `
  .\venueflow-user-service\target\venueflow-user-service-0.1.0-SNAPSHOT.jar
```

The application should listen on port `18082`.

Clear the environment variable after the application stops:

```powershell
Remove-Item Env:SERVER_PORT `
  -ErrorAction SilentlyContinue
```

Do not create or commit a local `.env` file for this module.

## Health probes

The supported operational health probes are:

```text
GET /actuator/health/liveness
GET /actuator/health/readiness
```

A healthy probe response has this shape:

```json
{
  "status": "UP"
}
```

### Liveness

PowerShell example:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8082/actuator/health/liveness"
```

Expected response:

```json
{
  "status": "UP"
}
```

### Readiness

PowerShell example:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8082/actuator/health/readiness"
```

Expected response:

```json
{
  "status": "UP"
}
```

### Aggregate health

The aggregate health endpoint is also available:

```text
GET /actuator/health
```

It reports the overall status and may list the configured health-group
names:

```json
{
  "status": "UP",
  "groups": [
    "liveness",
    "readiness"
  ]
}
```

The response must not expose component details, diagnostic details, disk
information, environment values, or application configuration.

## Actuator exposure boundary

The Actuator discovery page is disabled:

```text
GET /actuator
```

Sensitive management endpoints are not exposed. The following requests
must return `404`:

```text
GET /actuator/env
GET /actuator/configprops
GET /actuator/loggers
GET /actuator/mappings
GET /actuator/metrics
GET /actuator/beans
GET /actuator/conditions
GET /actuator/info
GET /actuator/heapdump
GET /actuator/threaddump
GET /actuator/scheduledtasks
```

The module does not introduce Spring Security merely to protect these
endpoints. They are excluded from the exposed management surface.

## Manual smoke test

Build and start the application:

```powershell
.\mvnw.cmd `
  -pl venueflow-user-service `
  -am `
  package

java -jar `
  .\venueflow-user-service\target\venueflow-user-service-0.1.0-SNAPSHOT.jar
```

In another PowerShell terminal, run:

```powershell
$liveness = Invoke-WebRequest `
  -Uri "http://localhost:8082/actuator/health/liveness"

$readiness = Invoke-WebRequest `
  -Uri "http://localhost:8082/actuator/health/readiness"

Write-Host "Liveness:" $liveness.StatusCode $liveness.Content
Write-Host "Readiness:" $readiness.StatusCode $readiness.Content
```

Expected result:

```text
Liveness: 200 {"status":"UP"}
Readiness: 200 {"status":"UP"}
```

Check that a sensitive endpoint is absent:

```powershell
curl.exe `
  -s `
  -o NUL `
  -w "%{http_code}" `
  "http://localhost:8082/actuator/env"
```

Expected result:

```text
404
```

## Automated verification

The module verification suite includes:

- default-profile Spring context startup
- application-name and default-profile checks
- absence of a `DataSource` bean
- configuration and secret boundary checks
- random-port HTTP health-probe tests
- sensitive Actuator endpoint rejection tests
- packaged executable-JAR process tests
- `SERVER_PORT` environment override tests
- Maven dependency-boundary enforcement
- packaged runtime-library inspection

Run all automated checks from the repository root:

```powershell
Remove-Item Env:SERVER_PORT `
  -ErrorAction SilentlyContinue

Remove-Item Env:SPRING_PROFILES_ACTIVE `
  -ErrorAction SilentlyContinue

.\mvnw.cmd `
  -pl venueflow-user-service `
  -am `
  clean verify
```

No Docker daemon or external service is required.

## Current non-goals

This skeleton does not implement:

- User entities
- User repositories
- database tables or migrations
- registration
- credential storage
- password processing
- login
- authentication
- JWT creation or validation
- roles or permissions
- authorization rules
- booking eligibility
- Gateway integration
- service discovery
- Nacos integration
- Redis integration
- RabbitMQ integration
- Kafka integration
- Feign clients
- cross-service calls
- application Docker images
- application Compose definitions

These capabilities require separate approved changes. They must not be
added implicitly to the default `skeleton` profile.

## Project structure

```text
venueflow-user-service/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/yanerdan/venueflow/user/
    │   │       └── UserServiceApplication.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-skeleton.yml
    └── test/
        └── java/
            └── com/yanerdan/venueflow/user/
                ├── UserServiceApplicationTest.java
                ├── UserServiceConfigurationBoundaryTest.java
                ├── UserServiceHealthIT.java
                └── UserServiceExecutableJarIT.java
```

## Completion criteria

The User Service skeleton is valid when all of the following are true:

- the module is included in the root Maven reactor
- the module builds as an independently executable Spring Boot JAR
- the JAR starts with `UserServiceApplication`
- the default profile is `skeleton`
- the default port is `8082`
- `SERVER_PORT` overrides the default port
- liveness and readiness return `200` with status `UP`
- health responses expose no component details
- sensitive Actuator endpoints return `404`
- startup requires no database or external infrastructure
- dependency-boundary checks pass
- `clean verify` succeeds with Docker unavailable
