#!/bin/sh
set -eu

require_env() {
  if [ -z "$(eval "printf '%s' \"\${$1:-}\"")" ]; then
    echo "Required environment variable is missing: $1" >&2
    exit 2
  fi
}

require_env WORKOPS_DB_URL
require_env WORKOPS_DB_USERNAME
require_env WORKOPS_DB_PASSWORD
require_env WORKOPS_FLYWAY_LOCATIONS

# WorkOps keeps DB environment names aligned with the web task and maps them to Flyway CLI flags here.
exec flyway \
  -url="${WORKOPS_DB_URL}" \
  -user="${WORKOPS_DB_USERNAME}" \
  -password="${WORKOPS_DB_PASSWORD}" \
  -locations="${WORKOPS_FLYWAY_LOCATIONS}" \
  migrate
