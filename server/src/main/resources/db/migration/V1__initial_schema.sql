CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');
CREATE TYPE project_status AS ENUM ('draft', 'in_progress', 'completed', 'archived');
CREATE TYPE sender_type AS ENUM ('USER', 'SYSTEM', 'AGENT');
CREATE TYPE content_type AS ENUM ('TEXT', 'MARKDOWN', 'HTML');
CREATE TYPE llm_provider_type AS ENUM ('Gemini', 'OpenAI', 'Anthropic');

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT,
    google_sub      TEXT UNIQUE,
    first_name      TEXT NOT NULL DEFAULT '',
    last_name       TEXT NOT NULL DEFAULT '',
    role            user_role NOT NULL DEFAULT 'USER',
    llm_provider    llm_provider_type,
    encrypted_api_key TEXT,
    llm_model       TEXT,
    email_verified_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    name            TEXT NOT NULL DEFAULT 'Untitled Project',
    requirements    JSONB,
    status          project_status NOT NULL DEFAULT 'draft',
    language        TEXT NOT NULL DEFAULT 'en',
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_user ON projects(user_id);

CREATE TABLE steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id),
    step_type       TEXT NOT NULL DEFAULT 'project_context',
    json_state      JSONB NOT NULL DEFAULT '{}',
    is_finished     BOOLEAN NOT NULL DEFAULT false,
    current_task    TEXT,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_steps_project ON steps(project_id);

CREATE TABLE chats (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id),
    current_step_id UUID UNIQUE REFERENCES steps(id),
    title           TEXT NOT NULL DEFAULT 'Brainstorming Session',
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chats_project ON chats(project_id);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id         UUID NOT NULL REFERENCES chats(id),
    sender_id       UUID REFERENCES users(id),
    sender_type     sender_type NOT NULL DEFAULT 'SYSTEM',
    content         TEXT NOT NULL DEFAULT '',
    content_type    content_type NOT NULL DEFAULT 'TEXT',
    source_node     TEXT,
    is_source_node_completed BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_chat ON messages(chat_id);

CREATE TABLE input_request_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL UNIQUE REFERENCES messages(id),
    input_type      TEXT NOT NULL DEFAULT 'text',
    options         JSONB,
    required        BOOLEAN NOT NULL DEFAULT true,
    min_value       INTEGER,
    max_value       INTEGER
);

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id         UUID UNIQUE REFERENCES steps(id),
    name            TEXT NOT NULL,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    author_id       UUID NOT NULL REFERENCES users(id),
    num             INTEGER NOT NULL DEFAULT 1,
    blob            OID,
    filename        TEXT,
    content_type    TEXT,
    checksum        TEXT,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_versions_document ON versions(document_id);
