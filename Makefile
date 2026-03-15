# Load .env then run command. Usage: $(call with_env,cmd,arg)
with_env = set -a && { [ -f .env ] && . ./.env || true; } && set +a && $1 $2

.PHONY: dev db help

dev:
	$(call with_env,sbt,dev)

db:
	docker compose up postgres -d

help:
	@echo "  make db   - Start PostgreSQL (docker compose up postgres -d)"
	@echo "  make dev  - Backend + frontend (loads .env, sbt dev)"
