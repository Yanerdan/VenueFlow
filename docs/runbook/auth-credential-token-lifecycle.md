# Auth credential and token lifecycle

## Runtime boundary

Default `skeleton` starts on 8081 without a database, RSA key, or Security filter. Enable C18 only
with `SPRING_PROFILES_ACTIVE=persistence` and untracked Auth database plus RSA PEM environment
values. V001 creates only `auth_credentials` and `auth_refresh_tokens`.

## Key generation

Generate an RSA 2048-bit-or-stronger PKCS#8 private key and matching X.509 public key outside the
repository. Supply their PEM text through `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY`; never place real
keys in `.env.example`, tracked YAML, logs, commands committed to Git, or test fixtures.

## Lifecycle

- Register normalizes the username and stores only BCrypt.
- Five consecutive failures lock login for 15 minutes; external errors do not reveal whether a
  username exists or is locked.
- Login returns a 15-minute RS256 Access JWT and a seven-day opaque Refresh Token.
- Refresh atomically revokes the submitted hash and creates one replacement; replay fails.
- Logout idempotently revokes the submitted refresh session. Existing Access JWTs remain valid
  until their short expiry; the `ver` claim supports later global-revocation enforcement.

## Verification

```powershell
.\mvnw.cmd -pl venueflow-auth-service -am clean verify
.\mvnw.cmd -pl venueflow-auth-service verify -Pauth-it
```

The first command is infrastructure-free. `auth-it` uses an isolated MySQL 8.4.10 container and
ephemeral in-memory RSA keys.

## Rollback

Disable the `persistence` profile and roll back the application. Do not edit or delete V001 or
automatically drop Auth tables; retain them for forward recovery.
