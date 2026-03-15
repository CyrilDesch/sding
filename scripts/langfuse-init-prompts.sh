#!/usr/bin/env sh
# Import prompts into Langfuse
set -e

cd "$(dirname "$0")/.."

if [ -f ".env" ]; then
  set -a
  . ".env"
  set +a
fi

scala-cli scripts/langfuse-init-prompts.scala
