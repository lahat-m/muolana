-- ============================================================
-- V002 — Conversation module (sessions + messages)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS conversations;

CREATE TABLE conversations.sessions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title          TEXT,
    status         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    last_active_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE', 'IDLE', 'ENDED'))
);

CREATE INDEX idx_conv_sessions_user_id ON conversations.sessions(user_id);
CREATE INDEX idx_conv_sessions_status  ON conversations.sessions(status);

CREATE TABLE conversations.messages (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID         NOT NULL REFERENCES conversations.sessions(id) ON DELETE CASCADE,
    role            VARCHAR(10)  NOT NULL,
    content         TEXT         NOT NULL,
    law_chunks_used UUID[]       DEFAULT '{}',
    cosine_scores   FLOAT8[]     DEFAULT '{}',
    guard_triggered BOOLEAN      NOT NULL DEFAULT FALSE,
    model           VARCHAR(100),
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT chk_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_conv_messages_session_id ON conversations.messages(session_id, created_at);
