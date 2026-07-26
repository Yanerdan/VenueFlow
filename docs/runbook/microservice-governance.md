# Microservice governance runbook

## Prepare Nacos

Use the existing base Compose profile and publish every tracked file under `deploy/nacos/` to the
matching Data ID in the configured namespace and `VENUEFLOW_GROUP`. These files contain only
non-secret defaults. Database credentials and JWT keys remain environment variables.

Required environment values:

```text
NACOS_SERVER_ADDR
NACOS_NAMESPACE
NACOS_USERNAME
NACOS_PASSWORD
VENUEFLOW_NACOS_GROUP
```

## Start two Resource instances

Start the normal persistence dependencies, then use distinct ports and instance IDs:

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence,governance"
$env:SERVER_PORT = "18083"
$env:VENUEFLOW_INSTANCE_ID = "resource-1"
java -jar venueflow-resource-service/target/venueflow-resource-service-0.1.0-SNAPSHOT.jar

$env:SERVER_PORT = "28083"
$env:VENUEFLOW_INSTANCE_ID = "resource-2"
java -jar venueflow-resource-service/target/venueflow-resource-service-0.1.0-SNAPSHOT.jar
```

Start User and Booking with `persistence,governance`; start Auth with the same profile and its
existing key/database variables. Start Gateway with `gateway,governance`. Gateway keeps an
explicit route allowlist and Booking resolves only the named User and Resource services.

## Verify and rollback

Send several Resource requests through Gateway, stop either Resource process, wait for Nacos to
remove it, then repeat the requests. The remaining instance must continue serving them. Verify
that `X-Trace-Id` is the same UUID at Gateway, Booking, and Resource.

Writes are never automatically retried. A capacity timeout is resolved through its operation ID
and existing reconciliation path. Roll back governance without data migration by removing the
`governance` profile and restoring static URI environment variables.
