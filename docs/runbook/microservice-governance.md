# Microservice governance runbook

## Start governed mode

The regular local command remains static and does not require Nacos. Use the governed switch to
start authenticated Nacos, initialize its administrator and namespace, publish all tracked Data
IDs, and launch two Resource instances:

```powershell
.\scripts\local-dev\start.ps1 -Governance
.\scripts\local-dev\status.ps1 -Governance
```

Local Nacos credentials are generated in the ignored `secrets/local-dev/local-dev.env`. Tracked
files under `deploy/nacos/` contain non-secret defaults only. Running the command again is safe:
the namespace and Data IDs are reconciled idempotently.

## Verify failover

```powershell
.\scripts\local-dev\governance-smoke.ps1
```

The bounded smoke test verifies both Resource registrations, stops only the managed second
instance, reads Resource data through Gateway while one instance remains, and restores the second
instance in a `finally` block. Writes are not retried. Return to static mode without data migration
by stopping the stack and running `start.ps1` without `-Governance`.
