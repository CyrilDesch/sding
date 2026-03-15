#!/usr/bin/env sh
# Backend hot-reload (sbt ~reStart, loads .env)
set -e

if [ -f "$(dirname "$0")/../.env" ]; then
  set -a
  . "$(dirname "$0")/../.env"
  set +a
fi

sbt "~server/reStart"
