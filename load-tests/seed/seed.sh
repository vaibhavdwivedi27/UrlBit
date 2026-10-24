#!/usr/bin/env bash
# ==============================================================================
# Urlbit Benchmark Seeding Script (Unix / macOS / Git Bash)
# Seeds the PostgreSQL container with 100 hot links and 10,000 cold links.
# Usage: ./seed.sh [container_name]
# ==============================================================================
set -e

CONTAINER_NAME=${1:-urlbit-postgres}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/seed-data.sql"

echo "Checking if PostgreSQL container '${CONTAINER_NAME}' is running..."
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "ERROR: Container '${CONTAINER_NAME}' is not running."
  echo "Start infrastructure first with: docker compose -f infra/compose.local.yml up -d postgres"
  exit 1
fi

echo "Seeding database with benchmark data..."
docker exec -i "${CONTAINER_NAME}" psql -U urlbit -d urlbit < "${SQL_FILE}"

echo "Database successfully seeded!"
echo "- User: benchmarks@urlbit.in (Password: BenchmarkPassword123!)"
echo "- 100 Hot Links: hot-0001 through hot-0100"
echo "- 10,000 Cold Links: cold-00001 through cold-10000"
