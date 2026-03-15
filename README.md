# sding

AI-powered design thinking assistant. Guides projects through a structured innovation workflow — from problem framing to competitive analysis and report generation — with LLM-driven steps and human-in-the-loop checkpoints.

## Workflow

The core pipeline follows established design thinking methods:

1. **Problem framing** — weird problem generation → reformulation → trend analysis → problem selection *(human checkpoint)*
2. **User research** — user interviews → empathy map → JTBD definition *(human checkpoint)*
3. **Ideation** — How Might We → Crazy 8s → SCAMPER → competitive analysis *(human checkpoint)*
4. **Prototyping** — prototype builds *(human checkpoint)*
5. **Reporting** — premium report → Markdown generation

Each LLM node is implemented as a typed `Agent[F]` call; human checkpoints suspend the workflow and resume on user input via SSE.

## Stack

### Backend (JVM)
- Scala 3.8.1 + Cats Effect 3
- http4s (Ember) — HTTP server & client
- fs2 — streaming (SSE)
- Circe — JSON
- Quill (JDBC) + Flyway — PostgreSQL access and migrations
- LangGraph4j + LangChain4j — agent workflow graph
- LLM providers: Gemini, OpenAI, Anthropic
- Web search tools: LangSearch, Exa
- JWT (jwt-scala) + bcrypt — authentication
- otel4s (OpenTelemetry) — observability
- Scribe — structured JSON logging
- Ciris — typed configuration from environment variables

### Frontend (Scala.js)
- Laminar — reactive UI
- Vite + `@scala-js/vite-plugin-scalajs` — dev server and bundling

### Infrastructure
- PostgreSQL 17
- Docker (eclipse-temurin:21-jre-alpine base image)

## Prerequisites

- JDK 21+
- sbt 1.x
- Node.js (for frontend dev server)
- PostgreSQL (or Docker)

## Development

### Start the database

```bash
docker compose up postgres -d
```

### Run backend and frontend together

```bash
cp .env.example .env   # once: edit .env with POSTGRES_* and LLM_*
make db                # once: start Postgres
make dev
```

Or without Make: `sbt dev` (set env vars or export them first).

Runs `npm install` in `client/` if `node_modules` is missing, starts the backend in the background, then the Vite dev server (frontend at http://localhost:5173, API at :8080). Stop with Ctrl+C (backend process is then shut down).

### Run the backend only

```bash
sbt server/run
```

### Run the frontend dev server only (with HMR and API proxy to :8080)

```bash
cd client && npm install && npm run dev   # http://localhost:5173
```

### Run tests

```bash
sbt test
```

### Format and compile (CI-equivalent)

```bash
GITHUB_ACTIONS=TRUE sbt scalafmtAll Test/compile scalafmtAll
```

## Configuration

All settings are read from environment variables. Copy `.env.example` to `.env`, then fill in `POSTGRES_*`. The Makefile loads `.env` automatically for `make dev`.

LLM API keys are **per-user**: each user registers their own key via `PUT /api/user/llm-config` (provider + api key + model). No global LLM key is required on the server.

Key variables:

| Variable | Description | Default |
|---|---|---|
| `HOST` / `PORT` | Server bind address | `0.0.0.0:8080` |
| `POSTGRES_HOST` | PostgreSQL host | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |
| `POSTGRES_DATABASE` | Database name | — |
| `POSTGRES_USER` | Database user | — |
| `POSTGRES_PASSWORD` | Database password | — |
| `JWT_SECRET` | JWT signing secret | `change-me-in-production` |
| `LLM_ENCRYPTION_KEY` | 32-byte key for encrypting stored user LLM keys | `change-me-in-production-32-bytes` |
| `ALLOWED_ORIGINS` | CORS allowed origins (comma-separated) | `*` |
| `LANGSEARCH_API_KEY` | LangSearch web search key | — |
| `EXA_API_KEY` | Exa web search key | — |

## Build

```bash
# Fat JAR
sbt assembly
java -jar target/sding-uber.jar

# Docker image
sbt Docker/publishLocal
docker compose up
```

## Project structure

```
├── shared/      # Circe protocol models (cross JVM + JS)
├── server/      # http4s server, agent workflow, repositories
│   └── src/main/scala/sding/
│       ├── agent/       # LLM agent abstraction + LangChain4j integration
│       ├── workflow/    # LangGraph4j state graph (ProjectContextGraph)
│       ├── service/     # ChatService, auth, encryption, DB migration
│       ├── repository/  # Quill-based repositories (users, projects, chats…)
│       ├── config/      # Ciris-based typed config
│       └── domain/      # Domain models, IDs, errors
└── client/      # Scala.js + Laminar frontend
    └── src/main/scala/sding/client/
        ├── api/         # HTTP + SSE client
        └── components/  # UI components
```
