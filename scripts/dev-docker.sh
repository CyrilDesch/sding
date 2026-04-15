#!/usr/bin/env sh
# Start postgres + Langfuse, auto-init prompts if empty
set -e

SCRIPT_DIR="$(dirname "$0")"

if [ -f "$SCRIPT_DIR/../.env" ]; then
  set -a
  . "$SCRIPT_DIR/../.env"
  set +a
fi

"$SCRIPT_DIR/db.sh"
"$SCRIPT_DIR/langfuse.sh"

LF_URL="${LANGFUSE_BASE_URL:-http://localhost:3000}"
LF_PUB="${LANGFUSE_PUBLIC_KEY:-pk-lf-sding-local-public-key}"
LF_SEC="${LANGFUSE_SECRET_KEY:-sk-lf-sding-local-secret-key}"

printf "Waiting for Langfuse to be ready"
until curl -sf -u "$LF_PUB:$LF_SEC" "$LF_URL/api/public/v2/prompts" > /dev/null 2>&1; do
  printf "."
  sleep 2
done
echo " OK"

echo "Migrating prompts from prompts.yaml..."
LANGFUSE_BASE_URL="$LF_URL" LANGFUSE_PUBLIC_KEY="$LF_PUB" LANGFUSE_SECRET_KEY="$LF_SEC" \
  scala-cli "$SCRIPT_DIR/langfuse-migrate-prompts.scala"
