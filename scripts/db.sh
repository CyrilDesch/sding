#!/usr/bin/env sh
# Start postgres in Docker
set -e

docker compose up postgres -d
