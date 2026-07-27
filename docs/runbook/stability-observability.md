# Stability and observability

## Safety model

- Default profiles expose only health and disable OpenTelemetry export.
- `stability` enables Sentinel only in Gateway, Booking, and Search.
- Rule templates under `deploy/sentinel` are deliberately disabled and have no capacity value.
- A blocked Booking write must return a non-success response; it is never retried or represented
  as a successful reservation.
- The checked timeout order is Gateway 5 s > Booking 4 s > collaborator read 2 s > connect 1 s.

## Start the local observation stack

Copy `.env.example` to the untracked `.env`, replace required infrastructure placeholders, then:

```powershell
docker compose --env-file deploy/versions.env --env-file .env `
  -f deploy/compose/compose.yml --profile observe up -d
```

Start any application with `observe` included in `SPRING_PROFILES_ACTIVE`. For example:

```powershell
$env:SPRING_PROFILES_ACTIVE = "gateway,observe"
$env:OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "http://127.0.0.1:4318/v1/traces"
java -jar venueflow-gateway/target/venueflow-gateway-0.1.0-SNAPSHOT.jar
```

Local endpoints are Prometheus `http://127.0.0.1:9090`, Grafana
`http://127.0.0.1:3000`, and Jaeger `http://127.0.0.1:16686`. Metrics are served from the
protected management path `/actuator/prometheus`; do not expose it publicly without network or
application authentication.

## Enable stability after measurement

Run a reproducible load test first, copy a rule template to the environment-owned Sentinel rule
source, set a measured threshold, and enable it outside Git. Do not commit credentials or claim a
production capacity from local results. Enable the application `stability` profile only after the
rule source is ready.

## Failure behavior

Collector, Prometheus, Grafana, and Jaeger availability is not part of application readiness.
Exporter failures may lose telemetry but must not fail business requests. Stop the optional stack
without deleting its named volumes:

```powershell
docker compose --env-file deploy/versions.env --env-file .env `
  -f deploy/compose/compose.yml --profile observe down --timeout 30
```
