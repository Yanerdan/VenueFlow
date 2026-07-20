#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${1:-"$REPO_ROOT/.env"}
TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-300}
VERSIONS_FILE="$REPO_ROOT/deploy/versions.env"
COMPOSE_FILE="$REPO_ROOT/deploy/compose/compose.yml"
VALIDATOR="$REPO_ROOT/scripts/bootstrap/validate-base-infrastructure.sh"
SERVICES='mysql redis rabbitmq nacos'

compose() {
  docker compose --env-file "$VERSIONS_FILE" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile base "$@"
}

diagnostics() {
  compose ps || true
  for service in $SERVICES; do
    printf '%s\n' "Diagnostics: $service"
    compose logs --tail 80 "$service" || true
  done
}

"$VALIDATOR" "$ENV_FILE" "$VERSIONS_FILE"
started_at=$(date +%s)
compose up -d
deadline=$((started_at + TIMEOUT_SECONDS))

while :; do
  all_healthy=true
  for service in $SERVICES; do
    container_id=$(compose ps -q "$service")
    if [ -z "$container_id" ]; then
      all_healthy=false
      continue
    fi
    status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")
    [ "$status" = healthy ] || all_healthy=false
  done
  [ "$all_healthy" = true ] && break
  if [ "$(date +%s)" -ge "$deadline" ]; then
    diagnostics
    printf '%s\n' "ERROR: base infrastructure did not become healthy within ${TIMEOUT_SECONDS}s" >&2
    exit 1
  fi
  sleep 3
done

compose exec -T mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql --protocol=TCP --host=127.0.0.1 --user=root --batch --skip-column-names --execute=SELECT/**/1 | grep -qx 1'
compose exec -T redis sh -c 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli ping | grep -qx PONG'
compose exec -T rabbitmq rabbitmq-diagnostics -q check_running
compose exec -T nacos sh -c 'curl --fail --silent http://127.0.0.1:8080/v3/console/health/liveness >/dev/null'

duration=$(( $(date +%s) - started_at ))
printf '%s\n' "Base infrastructure smoke passed (4/4 healthy, read-only protocols passed, ${duration}s)."
