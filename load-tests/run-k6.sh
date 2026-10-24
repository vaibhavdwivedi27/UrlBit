#!/usr/bin/env bash
# ==============================================================================
# Urlbit k6 Docker Runner (Linux / macOS / Git Bash)
# Executes k6 load testing scenarios via official Docker container (no install needed).
# Usage:
#   ./run-k6.sh [scenario] [target_host]
# Examples:
#   ./run-k6.sh smoke.js
#   ./run-k6.sh ramp.js http://localhost:8080
#   ./run-k6.sh hot-cache.js http://192.168.1.50:8080
# ==============================================================================
set -e

SCENARIO=${1:-smoke.js}
TARGET_HOST=${2:-http://host.docker.internal:8080}
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Verify scenario file exists
if [ ! -f "${ROOT_DIR}/load-tests/scenarios/${SCENARIO}" ]; then
  echo "ERROR: Scenario '${SCENARIO}' not found in load-tests/scenarios/"
  echo "Available scenarios:"
  ls -1 "${ROOT_DIR}/load-tests/scenarios/"
  exit 1
fi

echo "======================================================================"
echo " Running k6 Scenario: ${SCENARIO}"
echo " Target Host:         ${TARGET_HOST}"
echo "======================================================================"

# Run via official Docker image
docker run --rm -i \
  --add-host=host.docker.internal:host-gateway \
  -v "${ROOT_DIR}/load-tests:/load-tests:ro" \
  grafana/k6:latest run \
  -e TARGET_HOST="${TARGET_HOST}" \
  "/load-tests/scenarios/${SCENARIO}" "${@:3}"
