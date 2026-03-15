# Load .env then run command. Usage: $(call with_env,cmd,arg)
with_env = set -a && { [ -f .env ] && . ./.env || true; } && set +a && $1 $2

.PHONY: dev-server dev-client db help

dev-server:
	$(call with_env,sbt,"~server/reStart")

dev-client:
	cd client && npm install --prefer-offline && npm run dev

db:
	docker compose up postgres -d

help:
	@echo "  make db          - Start PostgreSQL (docker compose up postgres -d)"
	@echo "  make dev-server  - Backend hot-reload (sbt ~reStart, loads .env)"
	@echo "  make dev-client  - Frontend hot-reload (Vite + Scala.js watch)"
