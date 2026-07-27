# Application user journey

C24 adds a dependency-free browser application in `venueflow-web`. It uses only the public
Gateway at `http://127.0.0.1:8080`; the browser never calls an individual service.

## Build and test

```powershell
node --test venueflow-web/test/*.test.js
.\mvnw.cmd clean verify
```

The Web files need no package installation or build step. The Node tests cover Gateway URL
construction, access-token attachment, one refresh attempt, error propagation, and booking
idempotency keys.

## Local application

On Windows with Docker Desktop and JDK 21, the repository-owned local launcher prepares untracked
local credentials and RSA keys, starts MySQL/Redis/RabbitMQ/Elasticsearch, provisions five
service-owned schemas, starts all seven applications with their functional profiles, and adds
three demo venues with open slots:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/local-dev/start.ps1
```

Check or stop the stack without deleting data:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/local-dev/status.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/local-dev/stop.ps1
```

Run a disposable end-to-end acceptance journey through Gateway:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
```

Pass `-Infrastructure` to `stop.ps1` only when the local infrastructure containers should also
stop. Volumes are retained. Generated files stay under ignored `secrets/local-dev` and `.local`.
The default ports are:

| Component | Port |
| --- | ---: |
| Gateway | 8080 |
| Auth | 8081 |
| User | 8082 |
| Resource | 8083 |
| Booking | 8084 |
| Notification | 8085 |
| Search | 8086 |

Serve the browser files on the Gateway's default allowed origin:

```powershell
python -m http.server 3000 --directory venueflow-web
```

Open `http://127.0.0.1:3000`. The Gateway field may be changed before login. Registering creates an
Auth identity, logs in, and creates the matching User profile. A returning user loads the profile
from the trusted JWT subject. The application then supports resource search/listing, slot
selection, booking creation, confirmation/cancellation/check-in, history, notification inbox,
logout, and a single automatic token refresh.

Resource and slot seed data are still operator-owned. An empty catalog or inbox is displayed as an
empty state rather than treated as an application error.

## Scope

This change completes the application path and deterministic build checks. It does not claim load,
security-hardening, real-user, accessibility-audit, production deployment, or release evidence.
