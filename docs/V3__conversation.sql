-- ============================================================
--  V3 — Conversation module
--
--  Tables:
--    sessions    One row per chat session (citizen or anonymous)
--    messages    Full turn-by-turn chat history per session
-- ============================================================

-- ── Sessions ─────────────────────────────────────────────────

CREATE TYPE session_status AS ENUM ('ACTIVE', 'IDLE', 'ENDED');

CREATE TABLE sessions (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID        REFERENCES users (id) ON DELETE SET NULL,  -- null = anonymous
    title           TEXT,                          -- auto-generated from first query
    status          session_status NOT NULL DEFAULT 'ACTIVE',
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user    ON sessions (user_id);
CREATE INDEX idx_sessions_status  ON sessions (status);

-- ── Messages ─────────────────────────────────────────────────

CREATE TYPE message_role AS ENUM ('USER', 'ASSISTANT', 'SYSTEM');

CREATE TABLE messages (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id      UUID        NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    role            message_role NOT NULL,
    content         TEXT        NOT NULL,
    -- RAG metadata (populated for ASSISTANT turns only)
    law_chunks_used UUID[],                        -- array of document_chunk ids cited
    cosine_scores   FLOAT[],                       -- parallel array of retrieval scores
    model           TEXT,                          -- e.g. claude-sonnet-4-6
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    -- hallucination guard outcome
    guard_triggered BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_session  ON messages (session_id, created_at);
CREATE INDEX idx_messages_role     ON messages (role);
