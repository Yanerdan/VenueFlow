# Secure API Gateway runbook

## Start

Keep Auth's private key only in Auth. Provide Gateway only the matching X.509 public key.

```powershell
$env:SPRING_PROFILES_ACTIVE = "gateway"
$env:JWT_PUBLIC_KEY = "<Auth X.509 public key PEM>"
$env:VENUEFLOW_GATEWAY_ALLOWED_ORIGINS = "http://127.0.0.1:3000"
java -jar venueflow-gateway/target/venueflow-gateway-0.1.0-SNAPSHOT.jar
```

The default downstream origins are Auth `8081`, User `8082`, Resource `8083`, and Booking `8084`.
Override the corresponding `VENUEFLOW_GATEWAY_*_URI` variables when necessary. Each value must be
a plain HTTP(S) origin without credentials or a path.

## Verify

```powershell
Invoke-WebRequest http://127.0.0.1:8080/actuator/health/liveness
Invoke-WebRequest http://127.0.0.1:8080/actuator/health/readiness
.\mvnw.cmd -pl venueflow-gateway -am clean verify
```

Missing or invalid business-route tokens return bounded JSON `401`; unknown authenticated paths
return a non-success response. A declared body larger than
`VENUEFLOW_GATEWAY_MAX_REQUEST_BYTES` returns `413` before routing. Do not configure wildcard CORS
origins or place private keys in Gateway configuration.
