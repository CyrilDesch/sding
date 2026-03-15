# Database Schema

> **Role in the project:** Relational database design for the Sding application. Entities and relationships correspond to the data produced and consumed by the LangGraph pipeline.
>
> **Preceded by:** [LangGraph Pipeline Spec](./langgraph-pipeline-spec.md)

---

## 1. Entities & attributes

### **User**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK, ULID/UUIDv7 recommended |
| email | varchar(254) | unique, NOT NULL |
| password_hash | varchar | bytea | NULL when login via Google |
| google_sub | varchar(64) | Google “sub” claim, unique, NULL for email users |
| first_name | varchar(80) |  |
| last_name | varchar(80) |  |
| email_verified_at | timestamptz |  |
| created_at | timestamptz | default = now() |
| updated_at | timestamptz | auto-update trigger |

---

### **Project**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| user_id | UUID | FK → User(id), ON DELETE CASCADE |
| name | varchar(160) |  |
| requirements | jsonb | free-form brief / spec |
| status | enum(draft, active, archived) | default = draft |
| created_at | timestamptz |  |
| updated_at | timestamptz |  |
| deleted_at | timestamptz | NULL ⇒ live (soft delete) |

---

### **Chat**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| project_id | UUID | FK → Project(id) |
| current_step_id | UUID | FK → Step(id), NULL until a step is linked  |
| title | varchar(160) | optional |
| created_at | timestamptz |  |
| updated_at | timestamptz |  |
| deleted_at | timestamptz | NULL ⇒ live (soft delete) |

---

### **Message**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| chat_id | UUID | FK → Chat(id) |
| sender_id | UUID | FK → User(id), NULL for system/agent |
| sender_type | enum(user, agent, system) |  |
| idx | int | monotonically increasing; unique(chat_id, idx) |
| content | text | jsonb |  |
| created_at | timestamptz | indexed for DESC paging |

---

### **StepType** *(static lookup)*

| col | type | notes |
| --- | --- | --- |
| **id** | smallint | PK |
| name | varchar(80) | unique |

---

### **Step**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| project_id | UUID | FK → Project(id) |
| step_type_id | smallint | FK → StepType(id) |
| sub_step_name | varchar(160) | name of the last langgraph task  |
| json_state | jsonb | computed / UI state |
| is_finished | boolean | default = false |
| document_id | UUID | FK (unique) → Document(id), NULL if none yet |
| created_at | timestamptz |  |
| updated_at | timestamptz |  |

---

### **Document**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| step_id | UUID | FK → Step(id), unique |
| name | varchar(160) |  |
| created_at | timestamptz |  |
| updated_at | timestamptz |  |

---

### **Version**

| col | type | notes |
| --- | --- | --- |
| **id** | UUID | PK |
| document_id | UUID | FK → Document(id) |
| num | int | version counter; unique(document_id, num) |
| blob | bytea | full binary snapshot |
| author_id | UUID | FK → User(id), NULL for system |
| change_note | text | optional |
| checksum | varchar(128) | SHA-256, optional |
| created_at | timestamptz |  |

---

## 2. Relationships

User has many Projects
Project belongs to User

User has many Messages
Message belongs to User

Project has many Chats
Chat belongs to Project

Project has many Steps
Step belongs to Project

Chat has many Messages
Message belongs to Chat

Chat has zero-or-one Step
Step belongs to one Chat

StepType has many Steps
Step belongs to StepType

Step has zero-or-one Document
Document belongs to Step

Document has many Versions
Version belongs to Document

---

## 3. Minimal auth logic (Quarkus friendly)

| scenario | columns required | notes |
| --- | --- | --- |
| **Email + password** | `email`, `password_hash`, `google_sub=NULL` | Verify via bcrypt/argon2; set `email_verified_at` when link clicked |
| **Google OAuth** | `email`, `google_sub` (from ID-token) | Skip `password_hash`; keep row unique on *either* (`email`) or (`google_sub`) |

In Quarkus you can:

- Use **quarkus-oidc** for Google (external provider) → map ID-token claims to `google_sub` / `email`;
- Use **quarkus-security-jdbc** (or Hibernate w/ Panache) for local email/password log-in.

---

### Indices you should create now

- `idx_message_chat_created` on **Message** (chat_id, created_at DESC)
- FK columns on every table (`project_id`, `chat_id`…)
- `idx_version_doc_num` on **Version** (document_id, num DESC)
- `idx_project_user_status` on **Project** (user_id, status)