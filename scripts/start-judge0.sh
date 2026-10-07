#!/usr/bin/env bash
# Start self-hosted Judge0 CE (Docker) for local multi-language code execution.
# API will be available at http://localhost:2358

set -euo pipefail
cd "$(dirname "$0")/.."

echo "Starting Judge0 CE (db → redis → server → workers)…"
echo "First run may take several minutes while compiler images download."

docker compose up -d judge0-db judge0-redis
echo "Waiting for Judge0 database and Redis…"
sleep 12
docker compose up -d judge0-server judge0-workers

echo ""
echo "Judge0 CE starting on http://localhost:2358"
echo "Check status: curl http://localhost:2358/about"
echo "Stop: docker compose stop judge0-server judge0-workers judge0-db judge0-redis"
