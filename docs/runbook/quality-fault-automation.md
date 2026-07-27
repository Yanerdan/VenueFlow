# Quality and fault automation

## Deterministic policy

CI and local development use:

```bash
sh scripts/quality/test-verify-repository.sh
```

The command starts no infrastructure. It checks fixed image tags, credential signatures,
migration naming and published-migration modification, Compose profile boundaries, OpenSpec
structure, and `git diff --check`.

## Generate a safe plan

List scenario IDs in `scripts/fault-injection/scenarios.json`, then generate a dry-run manifest:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File scripts/fault-injection/invoke-fault.ps1 `
  -Scenario elasticsearch-outage
```

The default performs no mutation and writes status `PLANNED` under the ignored
`artifacts/fault-evidence` directory. A plan is not test evidence and must not be reported as a
successful exercise.

Validate every scenario and dry-run boundary with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File scripts/fault-injection/test-fault-driver.ps1
```

## Optional isolated execution

Only `elasticsearch-outage` and `redis-failure` are automated live actions. They require the
explicit `-Execute` switch and an existing untracked `.env`. The driver prints recovery first,
stops exactly one allowlisted Compose service, holds for at most 30 seconds, restarts it, and
checks that Compose reports it running.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File scripts/fault-injection/invoke-fault.ps1 `
  -Scenario redis-failure -Execute -EnvFile .env
```

Other scenarios remain plan-only until their isolated process/test fixture exists. Never point the
driver at shared or production infrastructure. If execution is interrupted, use the exact recovery
text printed before mutation. Stop optional profiles with `docker compose ... down --timeout 30`;
do not add `--volumes`.

Evidence manifests intentionally contain only scenario, exact target, status, UTC timestamps,
exit code, and a bounded note. They contain no credentials, payloads, raw logs, fabricated
latency, or fabricated recovery claim.
