# Digital Muolana — LegalBuddy South Sudan

Free, instant legal information from South Sudan law. Ask any question, get a cited answer, connect with a verified lawyer.

---

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 25, Spring Boot 4.0.7 |
| AI / RAG | Spring AI 2.0.0 — Gemini, Claude, or GPT-4o (switchable) |
| Vector store | PostgreSQL + pgvector (`gemini-embedding-001`, 1536 dims) |
| Database | PostgreSQL, Flyway migrations |
| Frontend | Thymeleaf, vanilla JS, SSE streaming |
| Auth | Spring Security, JWT (RSA-256), refresh tokens |

---

## Prerequisites

- Java 25+
- Maven 3.9+
- PostgreSQL 15+ with the [pgvector](https://github.com/pgvector/pgvector) extension
- An API key for at least one AI provider (Gemini, Anthropic, or OpenAI)

---

## Local setup

### 1. Clone and configure

```bash
git clone <repo-url>
cd muolana
```

Copy the environment template and fill in your values:

```bash
cp .env.example .env
```

### 2. Enable pgvector

Connect to your PostgreSQL instance and run:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Flyway handles the rest of the schema on first boot.

### 3. Generate RSA keys for JWT

The keys are **not** in the repository. Generate them once locally:

```bash
mkdir -p src/main/resources/certs

openssl genrsa -out src/main/resources/certs/private.pem 2048
openssl rsa -in src/main/resources/certs/private.pem \
            -pubout -out src/main/resources/certs/public.pem
```

### 4. Set environment variables

Minimum required:

| Variable | Default | Description |
|---|---|---|
| `AI_PROVIDER` | `gemini` | Active LLM profile: `gemini`, `anthropic`, or `openai` |
| `GEMINI_API_KEY` | — | Required when `AI_PROVIDER=gemini` |
| `ANTHROPIC_API_KEY` | — | Required when `AI_PROVIDER=anthropic` |
| `OPENAI_API_KEY` | — | Required when `AI_PROVIDER=openai` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/postgres` | PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | |
| `ADMIN_EMAIL` | `admin@muolana.ss` | Seeded admin account |
| `ADMIN_PASSWORD` | `Admin@2024!` | Seeded admin password |

### 5. Run

```bash
./mvnw spring-boot:run
```

App starts at `http://localhost:8080`.

---

## Switching AI providers

Change one environment variable — no code changes needed:

```bash
# Google Gemini (default)
AI_PROVIDER=gemini
GEMINI_API_KEY=your-key
GEMINI_CHAT_MODEL=gemini-2.5-flash        # optional override

# Anthropic Claude
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=your-key
ANTHROPIC_MODEL=claude-sonnet-4-6         # optional override

# OpenAI
AI_PROVIDER=openai
OPENAI_API_KEY=your-key
OPENAI_CHAT_MODEL=gpt-4o                  # optional override
```

> Embeddings always use `gemini-embedding-001` (1536 dims) regardless of the chat provider, except when `AI_PROVIDER=openai` which uses `text-embedding-3-small`.

---

## Project structure

```
src/main/java/com/lahat/muolana/
├── auth/               JWT auth, RSA config, admin seeder
├── conversation/       Chat sessions & messages, SSE streaming
├── lawyers/            Lawyer directory & profiles
├── legaldocuments/     Document upload, chunking, ingestion
├── rag/                RAG pipeline (search, prompt build, streaming)
├── analytics/          Query logging & dashboard data
└── config/             AI model config, JPA config
```

```
src/main/resources/
├── templates/          Thymeleaf pages (landing, chat, lawyers, admin/*)
├── static/css/         Per-page stylesheets
├── static/icons/       SVG icons
├── db/migration/       Flyway SQL migrations
└── certs/              RSA keypair — gitignored, generated locally
```

---

## Database schema

Managed by Flyway. Schemas:

| Schema | Tables |
|---|---|
| `auth` | `users`, `refresh_tokens` |
| `lawyers` | `lawyers`, `specialisations`, `contact_methods`, `reviews`, `referrals` |
| `legal_documents` | `documents`, `chunks` |
| `analytics` | `query_logs` |
| `conversation` | `sessions`, `messages` |
| `legal_documents` | `vector_store` (pgvector) |

---

## Key API endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/sessions` | Public | Create chat session |
| `POST` | `/api/v1/sessions/{id}/messages` | Public | Send message |
| `GET` | `/api/v1/sessions/{id}/messages/{mid}/stream` | Public | SSE stream LLM response |
| `GET` | `/api/v1/lawyers` | Public | List lawyers |
| `GET` | `/api/v1/lawyers/{id}` | Public | Lawyer profile |
| `POST` | `/api/v1/auth/login` | Public | Admin login |
| `POST` | `/api/v1/auth/refresh` | Public | Refresh JWT |
| `GET` | `/api/v1/admin/**` | Admin JWT | Admin dashboard data |

---

## Production deployment

### Keys

Store PEM files outside the repository and point to them via environment variables:

```bash
JWT_PUBLIC_KEY=file:/etc/muolana/certs/public.pem
JWT_PRIVATE_KEY=file:/etc/muolana/certs/private.pem
```

### CI / GitHub Actions

Store the PEM content as repository secrets, write to a temp file at deploy time:

```yaml
- name: Write JWT keys
  run: |
    mkdir -p /etc/muolana/certs
    echo "${{ secrets.JWT_PRIVATE_KEY }}" > /etc/muolana/certs/private.pem
    echo "${{ secrets.JWT_PUBLIC_KEY }}"  > /etc/muolana/certs/public.pem
```

### Docker

```dockerfile
FROM eclipse-temurin:25-jre
COPY target/muolana-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Pass secrets as environment variables or mount the certs directory as a volume.

---

## Admin dashboard

Access at `/admin/login`. The seeded admin account uses `ADMIN_EMAIL` / `ADMIN_PASSWORD` from environment variables.

Admin features:
- Lawyer management (approve, reject, edit)
- Legal document upload and ingestion into the vector store
- Analytics: query volume, outcomes, knowledge gaps, lawyer funnel

---

## Logging

Chat request/response logging is on by default at `INFO` level:

```
MSG_SUBMIT    session=… len=42 query="What are my rights under the Land Act?"
MSG_CREATED   session=… messageId=…
STREAM_START  session=… msgId=… chunks=4 guard=false query="…"
STREAM_DONE   session=… msgId=… elapsed=3241ms answer_chars=1184 chunks=4
```

Adjust verbosity in `application.properties`:

```properties
logging.level.com.lahat.muolana.conversation=DEBUG
```
