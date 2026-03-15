#!/usr/bin/env sh
# Frontend hot-reload (Vite)
set -e

cd "$(dirname "$0")/../client"
pnpm install --prefer-offline && pnpm dev
