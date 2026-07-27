#!/usr/bin/env sh
set -eu

repo="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
sh "$repo/scripts/quality/verify-repository.sh" "$repo"

grep -Eq 'mutable image tag found' "$repo/scripts/quality/verify-repository.sh"
grep -Eq 'credential signature found' "$repo/scripts/quality/verify-repository.sh"
grep -Eq 'published migration modified' "$repo/scripts/quality/verify-repository.sh"

printf '%s\n' 'quality-policy-tests: PASS'
