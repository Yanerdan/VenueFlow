# VenueFlow Booking Service

`venueflow-booking-service` is an independently executable Spring Boot MVC service
skeleton. It establishes the Booking Service runtime and verification boundary for future
work; it does not yet implement a reservation workflow.

## Requirements

- JDK 21
- Maven Wrapper from this repository
- No Docker, database, or external infrastructure for the default build and runtime profile

## Scope

The default `skeleton` profile is secret-free, listens on port `8084`, and exposes only
health probes. This increment deliberately contains no Booking API, entity, DTO, database,
migration, Resource/User call, authentication, messaging, caching, discovery, or Gateway
integration.

## Build and verify

From the repository root:

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am clean verify
```

The command completes without Docker or another external service. To verify the entire
reactor, run:

```powershell
.\mvnw.cmd clean verify
```

## Run

Package the module, then run the generated JAR:

```powershell
.\mvnw.cmd -pl venueflow-booking-service package
java -jar venueflow-booking-service\target\venueflow-booking-service-0.1.0-SNAPSHOT.jar
```

Override a locally occupied port without changing tracked configuration:

```powershell
$env:SERVER_PORT = "18084"
java -jar venueflow-booking-service\target\venueflow-booking-service-0.1.0-SNAPSHOT.jar
```

## Health probes

```powershell
curl.exe http://localhost:8084/actuator/health
curl.exe http://localhost:8084/actuator/health/liveness
curl.exe http://localhost:8084/actuator/health/readiness
```

Each probe reports `UP`. Sensitive Actuator endpoints, including `/actuator/env`,
`/actuator/configprops`, `/actuator/loggers`, `/actuator/mappings`, and
`/actuator/metrics`, are intentionally unavailable.
