#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${1:-"$REPO_ROOT/.env.example"}
VERSIONS_FILE=${2:-"$REPO_ROOT/deploy/versions.env"}
ALLOW_PLACEHOLDERS=${ALLOW_PLACEHOLDERS:-false}
COMPOSE_FILE="$REPO_ROOT/deploy/compose/compose.yml"

fail() {
  printf '%s\n' "ERROR: $1" >&2
  exit 1
}

env_value() {
  awk -F= -v wanted="$2" '
    $0 !~ /^[[:space:]]*#/ && $1 == wanted {
      sub(/^[^=]*=/, "")
      print
      exit
    }
  ' "$1"
}

require_value() {
  value=$(env_value "$1" "$2")
  [ -n "$value" ] || fail "Required variable is missing: $2"
  printf '%s' "$value"
}

command -v docker >/dev/null 2>&1 || fail "Docker CLI is required"
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 or newer is required"
[ -f "$ENV_FILE" ] || fail "Environment file not found"
[ -f "$VERSIONS_FILE" ] || fail "Versions file not found"
[ -f "$COMPOSE_FILE" ] || fail "Compose file not found"

MYSQL_IMAGE=$(require_value "$VERSIONS_FILE" MYSQL_IMAGE)
REDIS_IMAGE=$(require_value "$VERSIONS_FILE" REDIS_IMAGE)
RABBITMQ_IMAGE=$(require_value "$VERSIONS_FILE" RABBITMQ_IMAGE)
NACOS_IMAGE=$(require_value "$VERSIONS_FILE" NACOS_IMAGE)
INFRA_BIND_ADDRESS=$(require_value "$ENV_FILE" INFRA_BIND_ADDRESS)

printf '%s\n' "$MYSQL_IMAGE" | grep -Eq '^mysql:8\.4\.[0-9]+(-[A-Za-z0-9._-]+)?$' || fail "MYSQL_IMAGE must use an approved exact tag"
printf '%s\n' "$REDIS_IMAGE" | grep -Eq '^redis:7\.4\.[0-9]+(-[A-Za-z0-9._-]+)?$' || fail "REDIS_IMAGE must use an approved exact tag"
printf '%s\n' "$RABBITMQ_IMAGE" | grep -Eq '^rabbitmq:4\.1\.[0-9]+-management(-[A-Za-z0-9._-]+)?$' || fail "RABBITMQ_IMAGE must use an approved exact management tag"
printf '%s\n' "$NACOS_IMAGE" | grep -Eq '^nacos/nacos-server:v3\.1\.1(-[A-Za-z0-9._-]+)?$' || fail "NACOS_IMAGE must use the approved exact tag"
case "$MYSQL_IMAGE $REDIS_IMAGE $RABBITMQ_IMAGE $NACOS_IMAGE" in *:latest*) fail "latest image tags are forbidden" ;; esac
case "$INFRA_BIND_ADDRESS" in 0.0.0.0|::|'[::]'|'*') fail "INFRA_BIND_ADDRESS must not bind every host interface" ;; esac

required_env='MYSQL_ROOT_PASSWORD REDIS_PASSWORD RABBITMQ_USERNAME RABBITMQ_PASSWORD NACOS_AUTH_ENABLE NACOS_AUTH_TOKEN NACOS_AUTH_IDENTITY_KEY NACOS_AUTH_IDENTITY_VALUE'
for name in $required_env; do
  value=$(require_value "$ENV_FILE" "$name")
  if [ "$ALLOW_PLACEHOLDERS" != true ]; then
    case "$value" in replace-with*|change-me*|placeholder*) fail "Required variable still contains a placeholder: $name" ;; esac
  fi
done
for name in MYSQL_PORT REDIS_PORT RABBITMQ_PORT RABBITMQ_MANAGEMENT_PORT NACOS_HTTP_PORT NACOS_GRPC_PORT NACOS_GRPC_TLS_PORT; do
  value=$(require_value "$ENV_FILE" "$name")
  case "$value" in *[!0-9]*|'') fail "Host port must be an integer from 1 to 65535: $name" ;; esac
  [ "$value" -ge 1 ] && [ "$value" -le 65535 ] || fail "Host port must be an integer from 1 to 65535: $name"
done
[ "$(env_value "$ENV_FILE" NACOS_AUTH_ENABLE)" = true ] || fail "Nacos authentication must be enabled"

profiles=$(docker compose --env-file "$VERSIONS_FILE" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --profiles)
[ "$profiles" = base ] || fail "Compose must expose only the base profile"
services=$(docker compose --env-file "$VERSIONS_FILE" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile base config --services | sort)
expected=$(printf '%s\n' mysql nacos rabbitmq redis)
[ "$services" = "$expected" ] || fail "The base profile has an unexpected service set"

[ "$(grep -c 'profiles: \[base\]' "$COMPOSE_FILE")" -eq 4 ] || fail "Every service must require the base profile"
[ "$(grep -c '^    healthcheck:' "$COMPOSE_FILE")" -eq 4 ] || fail "Every service must define a healthcheck"
[ "$(grep -c '^    mem_limit:' "$COMPOSE_FILE")" -eq 4 ] || fail "Every service must define a memory limit"
grep -Fq 'http://127.0.0.1:8080/v3/console/health/liveness' "$COMPOSE_FILE" || fail "Nacos healthcheck must target the internal console listener on port 8080"
for volume in mysql-data redis-data rabbitmq-data nacos-data; do
  grep -q "^  $volume:" "$COMPOSE_FILE" || fail "Missing named volume: $volume"
done

docker compose --env-file "$VERSIONS_FILE" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile base config --quiet

for automation in "$REPO_ROOT/.github/workflows/ci.yml" "$REPO_ROOT/scripts/smoke-test/base-infrastructure-smoke.sh" "$REPO_ROOT/scripts/smoke-test/base-infrastructure-smoke.ps1"; do
  if [ -f "$automation" ] && grep -E 'down[[:space:]].*(--volumes|-v([[:space:]]|$))' "$automation" >/dev/null 2>&1; then
    fail "Automated infrastructure commands must not delete volumes"
  fi
done

printf '%s\n' "Base infrastructure static validation passed (4 services, bounded health/resources, safe bind)."
