# Booking capacity reconciliation runbook

C14 reconciles Booking reservations with Resource capacity operations. It is opt-in and only
exists when both `persistence` and `reconciliation` profiles are active. The default skeleton
and plain `persistence` runtime do not schedule reconciliation.

## Enable and stop

Set the Booking database and collaborator variables from `.env.example`, then enable:

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence,reconciliation"
$env:VENUEFLOW_RECONCILIATION_ENABLED = "true"
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar
```

`VENUEFLOW_RECONCILIATION_ENABLED=false` keeps the scheduler idle while retaining preview/run
support. On shutdown, new claims stop; an unfinished leased intent becomes reclaimable after its
bounded lease expires.

## Preview and bounded run

No admin action is the safe default. Preview reads only due intent metadata:

```powershell
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence,reconciliation `
  --venueflow.booking.reconciliation.admin.action=PREVIEW
```

A repair run requires both an operator reason and explicit confirmation:

```powershell
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence,reconciliation `
  --venueflow.booking.reconciliation.admin.action=RUN `
  --venueflow.booking.reconciliation.admin.reason="incident-123" `
  --venueflow.booking.reconciliation.admin.confirm=true
```

Each invocation claims at most `VENUEFLOW_RECONCILIATION_BATCH_SIZE` due intents. Never edit intent
state manually while a worker owns an unexpired lease.

## Observe and handle issues

Actuator records `venueflow.booking.reconciliation.due`,
`venueflow.booking.reconciliation.oldest.seconds`, and outcome counters. Logs contain bounded
identifiers and outcome codes, not booking payloads.

Inspect unresolved work in the Booking database only:

```sql
SELECT id, workflow_type, state, attempt_count, next_check_at, last_error_code
FROM booking_reconciliation_intent
WHERE state IN ('OPEN', 'LEASED', 'EXHAUSTED')
ORDER BY next_check_at, id;

SELECT intent_id, issue_code, severity, occurrence_count, last_seen_at
FROM reconciliation_issue
WHERE state = 'OPEN'
ORDER BY severity DESC, last_seen_at;
```

`OPERATION_MISMATCH` and exhausted work require operator investigation. Do not issue a blind
Resource write: verify the stored operation IDs and Resource operation lookup first, then rerun a
bounded confirmed batch after the underlying fault is corrected.

## Migration, verification, and rollback

Flyway V003 owns the four reconciliation tables and is immutable after release. Verify with:

```powershell
.\mvnw.cmd -pl venueflow-booking-service test
.\mvnw.cmd -pl venueflow-booking-service -Preconciliation-it verify
```

For operational rollback, set `VENUEFLOW_RECONCILIATION_ENABLED=false` and restart. Keep V003 and
its evidence; do not down-migrate or drop recovery records. Application rollback may deploy the
previous version after reconciliation is disabled.
