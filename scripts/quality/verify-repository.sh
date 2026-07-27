#!/usr/bin/env sh
set -eu

repo="${1:-.}"
cd "$repo"

fail() {
  printf 'quality-policy: %s\n' "$1" >&2
  exit 1
}

git diff --check

if grep -E '^[A-Z0-9_]*_IMAGE=.*(:latest|:edge|:main)$' deploy/versions.env >/dev/null; then
  fail "mutable image tag found"
fi

if git grep -n -E 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{20,}' \
  -- ':!**/*Test.java' ':!scripts/quality/verify-repository.sh' >/dev/null; then
  fail "credential signature found"
fi

for spec in openspec/specs/*/spec.md; do
  grep -q '^### Requirement:' "$spec" || fail "requirement missing in $spec"
  grep -q '^#### Scenario:' "$spec" || fail "scenario missing in $spec"
done

for migration in $(find venueflow-* -path '*/target' -prune -o \
  -path '*/src/main/resources/db/migration/*.sql' -type f -print); do
  basename "$migration" | grep -Eq '^V[0-9]{3}__.+\.sql$' ||
    fail "invalid migration name: $migration"
done

base_ref="${QUALITY_BASE_REF:-HEAD^}"
if git rev-parse --verify "$base_ref" >/dev/null 2>&1; then
  changed_migrations="$(git diff --diff-filter=M --name-only "$base_ref" HEAD -- \
    '*/src/main/resources/db/migration/V*.sql')"
  [ -z "$changed_migrations" ] || fail "published migration modified: $changed_migrations"
fi

grep -q 'profiles: \[base\]' deploy/compose/compose.yml ||
  fail "base Compose profile missing"
grep -q 'profiles: \[search\]' deploy/compose/compose.yml ||
  fail "search Compose profile missing"
grep -q 'profiles: \[observe\]' deploy/compose/compose.yml ||
  fail "observe Compose profile missing"

printf '%s\n' 'quality-policy: PASS'
