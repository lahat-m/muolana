/*
 * ============================================================
 *  DigitalMuolana (Muolana) — C4 Model Architecture
 *  Method  : Simon Brown C4 Model
 *  Stack   : Spring Boot 3.x, Spring AI 1.1, Spring Security
 *            OAuth2/JWT, PostgreSQL + PGVector
 *  Tool    : Structurizr DSL  https://structurizr.com/dsl
 * ============================================================
 */

workspace "DigitalMuolana Muolana" "C4 architecture — South Sudan legal AI platform" {

    model {

        /* ── External actors ── */

        citizen = person "Citizen" "Asks legal questions via web or mobile." {
            tags "External"
        }

        admin = person "Admin" "Uploads laws, manages lawyers, reviews analytics." {
            tags "External"
        }

        lawyer = person "Lawyer" "Receives user referrals. External actor." {
            tags "External"
        }

        /* ── External systems ── */

        anthropicApi = softwareSystem "Anthropic Claude API" "LLM inference, streaming SSE." {
            tags "External"
        }

        /* ── System under design ── */

        DigitalMuolana = softwareSystem "DigitalMuolana (Muolana)" "RAG legal Q&A, lawyer directory, admin tooling." {
            tags "Internal"

            webApp = container "Web / PWA" "Mobile-first PWA. Offline via service worker." {
                technology "React, TypeScript, Vite, IndexedDB"
                tags "Frontend"
            }

            authServer = container "Auth server" "Issues RS256 JWTs signed with RSA private key. Validates with public key." {
                technology "Spring Security, JJWT, RSA256 keypair"
                tags "Auth"
            }

            apiGateway = container "API facade" "JWT auth, rate limiting, disclaimer injection." {
                technology "Spring Boot 3.x, Spring Security, Spring MVC"
                tags "Api"
            }

            conversationModule = container "Conversation module" "Session lifecycle and message routing." {
                technology "Spring Boot, Spring AI ChatMemoryAdvisor"
                tags "Module"
            }

            ragModule = container "RAG module" "Retrieves law chunks, guards hallucinations, calls LLM." {
                technology "Spring AI 1.1, QuestionAnswerAdvisor, PgVectorStore"
                tags "Module"

                queryProcessor = component "Query processor" "Embeds query, runs top-k PGVector search." {
                    technology "Spring AI EmbeddingModel, PgVectorStore"
                    tags "Component"
                }

                hallucinationGuard = component "Hallucination guard" "Blocks LLM if cosine score below 0.75." {
                    technology "Spring @Component, cosine threshold"
                    tags "Component"
                }

                promptBuilder = component "Prompt builder" "Assembles chunks, history and user query." {
                    technology "Spring AI PromptTemplate, ChatMemoryAdvisor"
                    tags "Component"
                }

                llmClient = component "LLM client" "Calls Claude API with SSE streaming." {
                    technology "Spring AI ChatClient, Spring Retry"
                    tags "Component"
                }

                responseFormatter = component "Response formatter" "Formats citations and disclaimer, publishes AnswerReady." {
                    technology "Spring @Component, @TransactionalEventListener"
                    tags "Component"
                }

                queryProcessor     -> hallucinationGuard "Chunks + scores"
                hallucinationGuard -> promptBuilder      "Chunks above threshold"
                hallucinationGuard -> responseFormatter  "Not-found short-circuit"
                promptBuilder      -> llmClient          "Assembled prompt"
                llmClient          -> responseFormatter  "Streamed LLM output"
            }

            lawyerModule = container "Lawyer module" "Verified lawyer directory and referral dispatch." {
                technology "Spring Boot, Spring Data JPA, Spring Events"
                tags "Module"
            }

            adminModule = container "Admin module" "Document registry, analytics, lawyer approval." {
                technology "Spring Boot, Spring Batch, Spring Data JPA"
                tags "Module"

                documentRegistry = component "Document registry" "Tracks upload, version and verification status." {
                    technology "Spring Data JPA, PostgreSQL"
                    tags "Component"
                }

                ingestionPipeline = component "Ingestion pipeline" "Parse, chunk, embed and store legal docs." {
                    technology "TikaDocumentReader, TokenTextSplitter, Spring Batch"
                    tags "Component"
                }

                analyticsEngine = component "Analytics engine" "Query trends, knowledge gaps, funnel metrics." {
                    technology "Spring Data JPA, PostgreSQL"
                    tags "Component"
                }

                lawyerRegistry = component "Lawyer registry" "CRUD and approval workflow for lawyers." {
                    technology "Spring Data JPA, PostgreSQL"
                    tags "Component"
                }

                documentRegistry -> ingestionPipeline "Triggers on doc verified"
            }

            eventBus = container "Event bus" "In-process async events between modules." {
                technology "Spring ApplicationEventPublisher, @TransactionalEventListener"
                tags "Bus"
            }

            postgresql = container "PostgreSQL 16" "Sessions, users, lawyers, documents, analytics." {
                technology "PostgreSQL 16, Spring Data JPA, Flyway"
                tags "Database"
            }

            pgvectorStore = container "PGVector store" "Law chunk embeddings, cosine similarity search." {
                technology "pgvector 0.7+, Spring AI PgVectorStore"
                tags "Database"
            }

            /* ── Container relationships ── */

            webApp             -> authServer         "OAuth2 PKCE, token refresh" "HTTPS"
            webApp             -> apiGateway         "REST + streaming SSE" "HTTPS"

            apiGateway         -> authServer         "Validate JWT" "HTTPS"
            apiGateway         -> conversationModule "Route message" "In-process"
            apiGateway         -> lawyerModule       "Lawyer queries" "In-process"
            apiGateway         -> adminModule        "Admin operations" "In-process"

            conversationModule -> eventBus           "QuerySubmitted / AnswerReady" "Spring Events"

            ragModule          -> eventBus           "Subscribe / publish" "Spring Events"
            ragModule          -> pgvectorStore      "Similarity search" "JDBC"
            ragModule          -> anthropicApi       "LLM inference" "HTTPS"
            ragModule          -> postgresql         "Document metadata" "JDBC"

            lawyerModule       -> eventBus           "LawyerRequested" "Spring Events"
            lawyerModule       -> postgresql         "Lawyer profiles" "JDBC"

            adminModule        -> eventBus           "DocIngested" "Spring Events"
            adminModule        -> postgresql         "Registry, analytics, audit" "JDBC"
            adminModule        -> pgvectorStore      "Write embeddings" "JDBC"

            postgresql         -> pgvectorStore      "pgvector extension" "SQL"
        }

        /* ── System context relationships ── */

        citizen -> DigitalMuolana "Legal Q&A, lawyer referral" "HTTPS"
        admin   -> DigitalMuolana "Upload laws, manage lawyers" "HTTPS"
        lawyer  -> DigitalMuolana "Receives referral context" "HTTPS"
    }

    views {

        systemContext DigitalMuolana "SystemContext" {
            include *
            autoLayout tb
            title "L1 - System context: DigitalMuolana (Muolana)"
            description "Actors and external systems."
        }

        container DigitalMuolana "Containers" {
            include *
            autoLayout tb
            title "L2 - Containers: DigitalMuolana modular monolith"
            description "Spring Boot modular monolith."
        }

        component ragModule "RAGComponents" {
            include *
            autoLayout tb
            title "L3 - Components: RAG module"
            description "Retrieval, hallucination guard, prompt, LLM, formatter."
        }

        component adminModule "AdminComponents" {
            include *
            autoLayout tb
            title "L3 - Components: Admin module"
            description "Document registry, ingestion pipeline, analytics, lawyer registry."
        }

        styles {

            /*
             * Global rule: ALL elements white background, dark text.
             * Type and boundary distinguished by stroke colour only.
             *
             * Stroke key:
             *   External actors / systems  #6B7280  gray
             *   Internal system            #1D4ED8  blue   (3px — heavier boundary)
             *   Frontend                   #0F6E56  green
             *   Auth                       #7C3AED  violet
             *   API facade                 #1D4ED8  blue
             *   Modules                    #534AB7  indigo
             *   Event bus                  #B45309  amber
             *   Database                   #1E40AF  dark blue
             *   Cache                      #D85A30  coral
             *   Components                 #3C3489  deep indigo
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
                stroke "#1D4ED8"
                strokeWidth 3
                shape RoundedBox
            }

            element "Internal" {
                background "#ffffff"
                color "#000000"
                stroke "#1D4ED8"
                strokeWidth 10
                shape RoundedBox
            }

            element "Frontend" {
                background "#ffffff"
                color "#000000"
                stroke "#0F6E56"
                strokeWidth 2
                shape WebBrowser
            }

            element "Auth" {
                background "#ffffff"
                color "#000000"
                stroke "#7C3AED"
                strokeWidth 2
                shape RoundedBox
            }

            element "Api" {
                background "#ffffff"
                color "#000000"
                stroke "#1D4ED8"
                strokeWidth 2
                shape RoundedBox
            }

            element "Module" {
                color "#000000"
                stroke "#534AB7"
                strokeWidth 2
                shape Component
            }

            element "Bus" {
                background "#ffffff"
                color "#000000"
                stroke "#B45309"
                strokeWidth 2
                shape Pipe
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
                stroke "#3C3489"
                strokeWidth 2
                shape Component
            }

            relationship "Spring Events" {
                style Dashed
                color "#B45309"
                thickness 2
            }

            relationship "HTTPS" {
                color "#1D4ED8"
                thickness 1
            }

            relationship "JDBC" {
                color "#1E40AF"
                thickness 1
            }

        }

        /*
         * ── LEVEL 4 CODE REFERENCE ──
         *
         * @Bean VectorStore vectorStore(JdbcTemplate jdbc, EmbeddingModel em) {
         *     return PgVectorStore.builder(jdbc, em)
         *         .dimensions(1536)
         *         .distanceType(COSINE_DISTANCE)
         *         .build();
         * }
         *
         * @Bean ChatClient chatClient(ChatClient.Builder b,
         *                             VectorStore vs, ChatMemory cm) {
         *     return b.defaultAdvisors(
         *         new QuestionAnswerAdvisor(vs),
         *         new MessageChatMemoryAdvisor(cm)).build();
         * }
         *
         * @Bean VectorStoreDocumentIngestor ingestor(VectorStore vs) {
         *     return VectorStoreDocumentIngestor.builder()
         *         .documentTransformer(
         *             new TokenTextSplitter(512, 64, 5, 10000, true))
         *         .vectorStore(vs).build();
         * }
         *
         * JWT: Custom RS256. Auth server signs with RSA private key (PEM file or
         * keystore). All other services validate using the RSA public key only.
         * Use JJWT (io.jsonwebtoken) — Jwts.builder().signWith(privateKey, RS256).
         * Public key distributed as PEM or JWK endpoint for resource servers.
         */
    }
}
