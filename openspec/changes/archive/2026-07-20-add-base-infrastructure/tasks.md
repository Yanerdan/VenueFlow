## 1. Image and Environment Baseline

- [x] 1.1 Resolve exact MySQL 8.4, Redis 7.4, RabbitMQ 4.1 management and Nacos 3.1.1 image tags from official registries, verify target architecture manifests, and record the validated values in `deploy/versions.env` without `latest` or floating ranges.
  - Acceptance: every Compose image variable resolves to one pullable exact tag inside the approved baseline, and the source/validation date is documented; no fake digest lock is created.
  - Test: run registry manifest inspection or `docker compose pull`, then run the version-policy static check against both valid values and a temporary `latest` fixture that must fail.
- [x] 1.2 Extend `.env.example` with `INFRA_BIND_ADDRESS` and the required MySQL, Redis, RabbitMQ and Nacos authentication variables using loopback-safe, non-production placeholders.
  - Acceptance: a developer can copy the example to `.env` and identify every required local value, while tracked files contain no usable secret or machine-specific absolute path.
  - Test: run the environment preflight with the example, then run repository secret/path scans and `git check-ignore .env`.

## 2. Base Compose Topology

- [x] 2.1 Add `deploy/compose/compose.yml` with a fixed project convention, private network and an explicit `base` profile containing only MySQL, Redis, RabbitMQ management and Nacos standalone.
  - Acceptance: selecting `base` renders exactly four services; omitting profiles does not start them; no Java service, Elasticsearch or observability component is defined in the profile.
  - Test: run `docker compose config --profiles`, render the selected model, and assert the expected service set.
- [x] 2.2 Configure `${INFRA_BIND_ADDRESS}` port bindings, environment-based authentication and component-specific safe startup settings without logging or embedding actual credentials.
  - Acceptance: the example configuration binds only to `127.0.0.1`; a temporary Host-only value renders without editing Compose; no published port binds implicitly to all interfaces.
  - Test: inspect rendered port bindings for loopback and a temporary Host-only fixture, and run the missing/placeholder credential negative checks.
- [x] 2.3 Add `mysql-data`, `redis-data`, `rabbitmq-data` and `nacos-data` named volumes plus explicit CPU/memory limits and lightweight component settings within the documented 4~5GB infra-node budget.
  - Acceptance: every stateful service owns one named volume, configured limits sum within the base budget, and automated commands contain no `down --volumes` or equivalent deletion.
  - Test: inspect the rendered Compose model, run the lifecycle-policy static check, and review the resolved volume/project names.

## 3. Health and Runtime Verification

- [x] 3.1 Add bounded healthchecks for all four components using the exact images' supported commands/endpoints, including start period, interval, per-check timeout and finite retries.
  - Acceptance: each service can transition from starting to healthy without depending only on container process state or Compose start order.
  - Test: start the base profile, wait under the global deadline, and inspect all four Docker health states; temporarily use an invalid credential/configuration and confirm the check becomes unhealthy.
- [x] 3.2 Add equivalent PowerShell and POSIX preflight/static-validation entry points for Docker/Compose v2, environment completeness, image policy, profiles, bindings, healthchecks, resources and non-destructive lifecycle rules.
  - Acceptance: Windows and Linux entry points enforce the same policies before containers start and never print secret values.
  - Test: execute both entry points where supported and run fixtures for missing tools/variables, `latest`, wildcard binding and destructive cleanup policy; every invalid fixture returns nonzero.
- [x] 3.3 Add equivalent PowerShell and POSIX base smoke entry points that start the fixed Compose project, enforce a global readiness timeout, and execute read-only MySQL `SELECT 1`, Redis `PING`, RabbitMQ diagnostics/management and Nacos liveness checks.
  - Acceptance: repeated smoke runs converge on the same project and volumes; success prints component/status/duration only, while failure returns nonzero with bounded health and log diagnostics.
  - Test: run a successful smoke, run it again for idempotency, then stop or misconfigure one component and verify bounded failure without volume deletion.
- [x] 3.4 Verify the non-destructive stop/restart lifecycle and document the observed base-profile resource footprint.
  - Acceptance: normal stop and restart reuse all four named volumes, and the measured container memory/CPU summary fits or explicitly explains any deviation from the 4~5GB budget.
  - Test: capture volume identities before and after `down` without `--volumes`, restart, rerun smoke, and compare Docker resource output.

## 4. Documentation and CI Gate

- [x] 4.1 Add a base-infrastructure Runbook and update README with prerequisites, copy/edit/start/status/smoke/stop commands, local versus VMware binding, troubleshooting and prominent no-auto-delete guidance.
  - Acceptance: a clean-checkout developer can operate the base profile without this conversation and can distinguish normal stop from explicitly destructive volume removal.
  - Test: follow every non-destructive command from the documentation on a clean environment and verify all referenced paths and variables exist.
- [x] 4.2 Extend GitHub Actions with an independent infrastructure job that performs static validation, starts the base profile on an isolated runner, runs bounded smoke, uploads sanitized diagnostics on failure and always stops containers without deleting volumes.
  - Acceptance: the Maven job remains Docker-independent; the infrastructure job has an explicit timeout and never prints secrets or uses `down --volumes`.
  - Test: validate workflow syntax, run the job-equivalent commands locally, and inspect failure-path conditions and cleanup commands.

## 5. Regression and Handoff

- [x] 5.1 Run the complete C02 acceptance suite: Windows/POSIX static checks, Compose render/policy checks, runtime smoke, restart persistence check, `mvnw.cmd clean verify`, OpenSpec strict validation and repository hygiene scans.
  - Acceptance: all applicable checks pass with captured command summaries; no generated container data, logs, build outputs, `.env` or secrets are tracked.
  - Test: preserve the exact commands and results in `.agent/HANDOFF.md`, review `git diff --check` and inspect the complete Git diff.
- [x] 5.2 Update README/HANDOFF and all task checkboxes to reflect verified reality, including exact image tags, environment tested, known limitations and the next Change `add-resource-service-skeleton`.
  - Acceptance: documentation makes no unsupported availability, security, backup or performance claims, and every completed checkbox has corresponding evidence.
  - Test: cross-check tasks, version files, Compose output, test results and HANDOFF; run `openspec validate --all --strict` as the final planning/implementation consistency gate.
