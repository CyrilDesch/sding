#!/usr/bin/env sh
# Start Langfuse stack in Docker
set -e

docker compose --profile langfuse up -d
echo "Langfuse UI: http://localhost:3000  (admin@sding.local / admin-sding-local)"
