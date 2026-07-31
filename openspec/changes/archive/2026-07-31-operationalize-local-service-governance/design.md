## Context

The repository's governance layer is code-complete but operationally manual. Static local startup
uses fixed HTTP origins, omits the Nacos container, does not publish tracked Data IDs, and launches
one Resource process. Nacos 3 also requires explicit administrator initialization before its
authenticated Admin API can publish configuration. The existing lightweight path is stable and
must remain available on machines that cannot afford another JVM-based container.

## Goals / Non-Goals

**Goals:**

- Provide one repeatable switch that starts authenticated Nacos and governed services.
- Reuse the existing discovery, Config, Feign, LoadBalancer, trace, and reconciliation code.
- Prove two Resource instances and bounded failover without changing public APIs or data.
- Keep static local startup behavior unchanged.
- Prevent telemetry exporters from connecting unless observation is explicitly requested.

**Non-Goals:**

- Nacos clustering, production credentials, dynamic secret distribution, or public exposure.
- Kubernetes, Sentinel dashboard operation, production load claims, or automatic write retries.
- Replacing RabbitMQ, Redis, Elasticsearch, MySQL, or the current service boundaries.

## Decisions

- Extend `start.ps1` with `-Governance` instead of making Nacos mandatory. This preserves the
  known-good development path and respects the documented local memory budget.
- Add an idempotent Nacos bootstrap script that first attempts authenticated login, initializes
  the local administrator only when needed, checks/creates a fixed local namespace, publishes all
  tracked YAML files with the Nacos 3 Admin API, and reads them back.
- Keep credentials and administrator password in `secrets/local-dev/local-dev.env`; tracked Data
  IDs remain non-secret.
- In governed mode, launch Resource on ports 8083 and 18083 with distinct instance IDs. Other
  services retain their current ports, while Gateway and Booking resolve service identities.
- Extend PID/log/status conventions to treat `resource-2` as a normal managed process. Stopping a
  single Resource instance for acceptance must not stop infrastructure or mutate data.
- Add a separate governance smoke script. The existing business smoke remains the compatibility
  gate; the governance smoke verifies Nacos configuration/registration and read failover.
- Explicitly disable OTLP metrics in base configuration and enable it only in `observe`. Disabling
  OpenTelemetry tracing alone is insufficient because an OTLP meter registry can still publish.

## Risks / Trade-offs

- **Nacos API or image behavior changes** → Pin Nacos 3.1.1 and validate exact v3 endpoints.
- **First-run administrator initialization races** → Bootstrap runs before Java services and is
  idempotent on subsequent starts.
- **Two instances increase local memory** → Only governed mode launches the second instance.
- **Instance removal is eventually consistent** → Acceptance uses bounded polling and restores
  the stopped instance in `finally`.
- **Static and governed modes drift** → Run the same business smoke in both modes and retain
  explicit profile-specific tests.
- **A failed governed startup leaves processes running** → Existing stop tooling tracks every PID,
  and bootstrap performs no volume or schema deletion.

## Migration Plan

1. Add safe environment defaults and Nacos authentication settings.
2. Add and test idempotent Nacos bootstrap.
3. Extend local process orchestration and status output.
4. Run the current static mode smoke unchanged.
5. Run governed startup, registration checks, business smoke, and single-instance failover.
6. Roll back by stopping the stack and starting without `-Governance`; no database migration is
   involved.

## Open Questions

None. The governed mode remains explicit and uses the pinned Nacos 3.1.1 contract.
