# VenueFlow Gateway

The module is a reactive Spring Cloud Gateway entry point. Its default `skeleton` profile exposes
restricted health probes without keys or downstream connections. The explicit `gateway` profile
enables four static routes and validates Auth-issued RS256 access tokens.

```powershell
.\mvnw.cmd -pl venueflow-gateway -am clean verify

$env:SPRING_PROFILES_ACTIVE = "gateway"
$env:JWT_PUBLIC_KEY = "<Auth X.509 public key PEM>"
java -jar venueflow-gateway/target/venueflow-gateway-0.1.0-SNAPSHOT.jar
```

Public paths are `/api/v1/auth/**` and health probes. User, resource, and booking paths require a
valid JWT. Gateway replaces client identity headers, propagates a UUID trace, limits declared
request bodies to one MiB by default, and permits only configured CORS origins.

See [the secure Gateway runbook](../docs/runbook/secure-api-gateway.md).

With profiles `gateway,governance`, the same explicit allowlist uses `lb://` service identities
from Nacos; discovery locator remains disabled.
