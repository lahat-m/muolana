/*
 * ============================================================
 *  Muolana (DigitalMuolana) — C4 Model Architecture
 *  Method  : Simon Brown C4 Model
 *  Stack   : Spring Boot 4.0.7 · Java 25 · Spring AI 2.0.0
 *            Spring Security OAuth2 Resource Server (RS256 JWT)
 *            PostgreSQL 17 + pgvector · MinIO (S3-compatible)
 *            Thymeleaf server-side rendering (mobile-first UI)
 *  LLM     : Google Gemini (default) · Anthropic Claude · OpenAI
 *            — switched via Spring profile (AI_PROVIDER env var)
 *  Embed   : gemini-embedding-001 (1536 dims, COSINE_DISTANCE)
 *  Deploy  : Docker (GHCR) → Render.com or self-hosted nginx/WSL
 *  Tool    : Structurizr DSL  https://structurizr.com/dsl
 * ============================================================
 */

workspace "Muolana" "C4 architecture — South Sudan legal AI platform" {

    model {

        /* ── External actors ── */

        citizen = person "Citizen" "Asks legal questions via the web/mobile browser." {
            tags "External"
        }

        admin = person "Admin" "Uploads laws, manages lawyers, reviews analytics via the admin UI." {
            tags "External"
        }

        lawyer = person "Lawyer" "Listed in the directory; receives referrals from citizens." {
            tags "External"
        }

        /* ── External systems ── */

        llmApi = softwareSystem "LLM API" "AI inference: Google Gemini (default), Anthropic Claude, or OpenAI. Selected via AI_PROVIDER env var / Spring profile. Streams tokens via SSE." {
            tags "External"
        }

        minioStorage = softwareSystem "MinIO / Cloudflare R2" "S3-compatible object storage for uploaded legal documents (PDF, DOCX). Self-hosted MinIO in dev/prod-VPS; Cloudflare R2 on Render." {
            tags "External"
        }

        /* ── System under design ── */

        muolana = softwareSystem "Muolana" "RAG legal Q&A assistant, verified lawyer directory, and admin tooling for South Sudan law." {
            tags "Internal"

            /* ── Single Spring Boot monolith ── */

            app = container "Spring Boot Application" "Modular monolith: Thymeleaf UI + REST API. All modules (auth, conversation, RAG, lawyers, legal docs, analytics) run in-process." {
                technology "Spring Boot 4.0.7 · Java 25 · Spring AI 2.0.0 · Thymeleaf · Spring MVC"
                tags "App"

                /* ── Web / UI layer ── */

                webUi = component "Web / UI layer" "Thymeleaf controllers serve mobile-first HTML pages: landing, chat, lawyers directory, lawyer profile, admin dashboard, documents, analytics." {
                    technology "Spring MVC @Controller · Thymeleaf · Vanilla JS · CSS"
                    tags "Component"
                }

                /* ── Auth module ── */

                authModule = component "Auth module" "User registration, login, JWT issuance and refresh. RS256 JWTs signed with RSA private key; validated by Spring Security OAuth2 Resource Server using public key. Rate-limited with AuthRateLimitFilter." {
                    technology "Spring Security · OAuth2 Resource Server · BCrypt · RSA keypair"
                    tags "Component"
                }

                /* ── Conversation module ── */

                conversationModule = component "Conversation module" "Session lifecycle (create, list, delete) and message routing. Stores history in PostgreSQL. Anonymous sessions — no login required." {
                    technology "Spring Data JPA · SessionEntity · MessageEntity"
                    tags "Component"
                }

                /* ── RAG module ── */

                ragModule = component "RAG module" "Retrieves relevant law chunks from pgvector, runs hallucination guard (cosine threshold 0.3), assembles prompt, streams LLM response as SSE tokens, records citation refs." {
                    technology "Spring AI 2.0.0 · PgVectorStore · SseEmitter · ChatClient"
                    tags "Component"
                }

                /* ── Lawyer module ── */

                lawyerModule = component "Lawyer module" "SSLS-verified lawyer directory. Supports filtering by specialisation, location, fee type, and language. Citizen actions: referral dispatch, reviews. Admin: CRUD + approve/suspend workflow." {
                    technology "Spring Data JPA · Spring Events · LawyerEntity"
                    tags "Component"
                }

                /* ── Legal documents module ── */

                legalDocumentsModule = component "Legal documents module" "Admin uploads PDF/DOCX → stored in MinIO. On verify, ingestion pipeline runs: Tika parse → TokenTextSplitter (512 token chunks, 64 overlap) → gemini-embedding-001 → pgvector insert." {
                    technology "Spring AI TikaDocumentReader · TokenTextSplitter · PgVectorStore · MinIO SDK 8.5.13"
                    tags "Component"
                }

                /* ── Analytics module ── */

                analyticsModule = component "Analytics module" "Records every query with outcome (ANSWERED / NOT_FOUND / ERROR), latency, and category. Admin read endpoints: summary, top categories, knowledge gaps, session stats, lawyer referral funnel." {
                    technology "Spring Data JPA · QueryLogEntity"
                    tags "Component"
                }

                /* ── Shared ── */

                shared = component "Shared kernel" "BaseEntity, UserId value object, PageResponse wrapper, DomainEvent, SpringEventPublisher, AssertUtil, GlobalExceptionHandler." {
                    technology "Spring ApplicationEventPublisher · Jakarta Validation"
                    tags "Component"
                }

                /* ── Intra-monolith relationships ── */

                webUi              -> authModule            "Login, register, token refresh" "In-process"
                webUi              -> conversationModule    "Session + message CRUD" "In-process"
                webUi              -> lawyerModule          "Lawyer directory, referrals, reviews" "In-process"
                webUi              -> legalDocumentsModule  "Document upload, list, file download" "In-process"
                webUi              -> analyticsModule       "Analytics read (dashboard)" "In-process"

                conversationModule -> ragModule             "Invoke RAG pipeline for user query" "In-process"
                conversationModule -> shared                "Events, base types" "In-process"

                ragModule          -> analyticsModule       "Record query outcome" "In-process"
                ragModule          -> shared                "Events" "In-process"

                legalDocumentsModule -> shared              "Events (DocIngested)" "In-process"
                lawyerModule       -> shared                "Events (LawyerRequested)" "In-process"
            }

            /* ── Databases ── */

            postgresql = container "PostgreSQL 17 + pgvector" "All relational data: users, refresh tokens, sessions, messages, lawyers, legal documents, analytics logs. pgvector extension hosts law chunk embeddings in schema legal_documents.vector_store (1536 dims, cosine)." {
                technology "PostgreSQL 17 · pgvector/pgvector:pg17 · Flyway migrations · Spring Data JPA"
                tags "Database"
            }
        }

        /* ── System context relationships ── */

        citizen -> muolana "Legal Q&A (anonymous chat), lawyer directory, referrals" "HTTPS"
        admin   -> muolana "Upload laws, manage lawyers, view analytics" "HTTPS"
        lawyer  -> muolana "Listed in directory; receives referral emails" "Email / HTTPS"

        muolana -> llmApi        "LLM inference (token streaming, embeddings)" "HTTPS / SSE"
        muolana -> minioStorage  "Store & retrieve uploaded legal documents" "HTTPS (S3 API)"

        /* ── Container relationships ── */

        app -> postgresql    "JPA / JDBC — sessions, users, lawyers, documents, analytics, vector store" "TCP 5432"
        app -> llmApi        "Chat completions + embeddings (Spring AI ChatClient / EmbeddingModel)" "HTTPS"
        app -> minioStorage  "Upload / download documents (MinIO Java SDK)" "HTTPS"
    }

    views {

        systemContext muolana "SystemContext" {
            include *
            autoLayout tb
            title "L1 — System context: Muolana"
            description "Actors, external LLM API, and object storage."
        }

        container muolana "Containers" {
            include *
            autoLayout tb
            title "L2 — Containers: Muolana modular monolith"
            description "Single Spring Boot 4.0.7 app + PostgreSQL 17 + pgvector."
        }

        component app "Components" {
            include *
            autoLayout tb
            title "L3 — Components: Spring Boot monolith internals"
            description "Modules inside the monolith: UI, auth, conversation, RAG, lawyers, legal docs, analytics."
        }

        styles {

            /*
             * Global rule: ALL elements white background, dark text.
             * Type and boundary distinguished by stroke colour only.
             *
             * Stroke key:
             *   External actors / systems  #6B7280  gray
             *   Internal system            #0284C7  sky blue  (3px)
             *   App container              #0284C7  sky blue
             *   Database container         #1E40AF  dark blue
             *   Components                 #534AB7  indigo
             *   Shared kernel              #64748B  slate
             */

            element "Element" {
                background "#ffffff"
                color "#000000"
                fontSize 13
                border Solid
            }

            element "Person" {
                shape Person
                background "#ffffff"
                color "#000000"
                stroke "#6B7280"
                strokeWidth 2
            }

            element "External" {
                background "#ffffff"
                color "#6B7280"
                stroke "#6B7280"
                strokeWidth 2
                shape RoundedBox
            }

            element "softwareSystem" {
                background "#ffffff"
                color "#000000"
                stroke "#0284C7"
                strokeWidth 3
                shape RoundedBox
            }

            element "Internal" {
                background "#ffffff"
                color "#000000"
                stroke "#0284C7"
                strokeWidth 10
                shape RoundedBox
            }

            element "App" {
                background "#ffffff"
                color "#000000"
                stroke "#0284C7"
                strokeWidth 2
                shape RoundedBox
            }

            element "Database" {
                background "#ffffff"
                color "#000000"
                stroke "#1E40AF"
                strokeWidth 2
                shape Cylinder
            }

            element "Component" {
                color "#000000"
                stroke "#534AB7"
                strokeWidth 2
                shape Component
            }

            relationship "In-process" {
                style Solid
                color "#534AB7"
                thickness 1
            }

            relationship "HTTPS" {
                color "#0284C7"
                thickness 1
            }

            relationship "TCP 5432" {
                color "#1E40AF"
                thickness 1
            }

            relationship "HTTPS / SSE" {
                style Dashed
                color "#0284C7"
                thickness 2
            }
        }

        /*
         * ── LEVEL 4 CODE REFERENCE ──
         *
         * Vector store (AiConfig.java):
         *   PgVectorStore — schema: legal_documents, table: vector_store
         *   dimensions: 1536, distanceType: COSINE_DISTANCE
         *   Embedding model: gemini-embedding-001 (task: RETRIEVAL_QUERY)
         *
         * RAG pipeline (RagPipeline.java):
         *   hallucination threshold: app.rag.hallucination-threshold=0.3
         *   top-k: app.rag.top-k=10
         *   streaming: SseEmitter with 300 s timeout
         *
         * Ingestion (IngestionPipeline.java):
         *   TikaDocumentReader → TokenTextSplitter(512, 64 overlap)
         *   → EmbeddingModel → PgVectorStore
         *   Downloads from MinIO via DocumentStorageService (try-with-resources)
         *
         * Auth (JwtTokenProvider.java):
         *   Custom RS256 JWT, RSA keypair from PEM files
         *   Spring Security OAuth2 Resource Server validates with public key
         *   Access token TTL: 900 000 ms (15 min)
         *   Refresh token TTL: 604 800 000 ms (7 days)
         *   Rate limiting on /api/v1/auth/** via AuthRateLimitFilter
         *
         * Deployment:
         *   Dockerfile: eclipse-temurin:25.0.3_9-jdk-alpine (build)
         *              eclipse-temurin:25.0.3_9-jre-alpine  (runtime)
         *   Image registry: ghcr.io/{owner}/muolana
         *   Render.com: render.yaml blueprint (free tier, Cloudflare R2 for storage)
         *   Self-hosted: docker-compose.prod.yml + nginx/nginx.conf
         *   Health check: GET /actuator/health (Spring Boot Actuator, no auth)
         *   PORT: injected by Render via $PORT env var → server.port=${PORT:8080}
         */
    }
}
