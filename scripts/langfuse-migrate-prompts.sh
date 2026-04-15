#!/usr/bin/env sh
# Migrate prompts into Langfuse (diff + new version only when changed)
set -e

cd "$(dirname "$0")/.."

if [ -f ".env" ]; then
  set -a
  . ".env"
  set +a
fi

scala-cli scripts/langfuse-migrate-prompts.scala
