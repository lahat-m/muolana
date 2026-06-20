-- ============================================================
-- V001 — Admin module initial schema
-- ============================================================

-- Schemas
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS lawyers;
CREATE SCHEMA IF NOT EXISTS legal_documents;
CREATE SCHEMA IF NOT EXISTS analytics;

-- ── auth.users ───────────────────────────────────────────────
CREATE TABLE auth.users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'CITIZEN',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email   ON auth.users (email);
CREATE INDEX idx_users_role    ON auth.users (role);

-- ── auth.refresh_tokens ──────────────────────────────────────
CREATE TABLE auth.refresh_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    jti        VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version    INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_refresh_tokens_user_id ON auth.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_jti     ON auth.refresh_tokens (jti);

-- ── lawyers.lawyers ──────────────────────────────────────────
CREATE TABLE lawyers.lawyers (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name      VARCHAR(255)   NOT NULL,
    bar_number     VARCHAR(100)   NOT NULL UNIQUE,
    location_city  VARCHAR(255),
    fee_type       VARCHAR(20)    NOT NULL DEFAULT 'PAID',
    bio            TEXT,
    is_verified    BOOLEAN        NOT NULL DEFAULT FALSE,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    average_rating NUMERIC(3, 2)  NOT NULL DEFAULT 0.00,
    review_count   INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    version        INT            NOT NULL DEFAULT 0
);

CREATE INDEX idx_lawyers_status        ON lawyers.lawyers (status);
CREATE INDEX idx_lawyers_location_city ON lawyers.lawyers (location_city);

-- ── lawyers.specialisations ──────────────────────────────────
CREATE TABLE lawyers.specialisations (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lawyer_id  UUID         NOT NULL REFERENCES lawyers.lawyers (id) ON DELETE CASCADE,
    area       VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version    INT          NOT NULL DEFAULT 0
);

-- ── lawyers.languages ────────────────────────────────────────
CREATE TABLE lawyers.languages (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lawyer_id  UUID         NOT NULL REFERENCES lawyers.lawyers (id) ON DELETE CASCADE,
    language   VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version    INT          NOT NULL DEFAULT 0
);

-- ── lawyers.contact_methods ──────────────────────────────────
CREATE TABLE lawyers.contact_methods (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lawyer_id  UUID         NOT NULL REFERENCES lawyers.lawyers (id) ON DELETE CASCADE,
    channel    VARCHAR(20)  NOT NULL,
    value      VARCHAR(255) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version    INT          NOT NULL DEFAULT 0
);

-- ── lawyers.reviews ──────────────────────────────────────────
CREATE TABLE lawyers.reviews (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lawyer_id       UUID        NOT NULL REFERENCES lawyers.lawyers (id) ON DELETE CASCADE,
    citizen_user_id UUID        NOT NULL,
    session_id      UUID,
    rating          INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text     TEXT,
    is_anonymous    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_reviews_lawyer_id ON lawyers.reviews (lawyer_id);

-- ── lawyers.referrals ────────────────────────────────────────
CREATE TABLE lawyers.referrals (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lawyer_id       UUID        NOT NULL REFERENCES lawyers.lawyers (id) ON DELETE CASCADE,
    citizen_user_id UUID        NOT NULL,
    session_id      UUID        NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    consent_given   BOOLEAN     NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_referrals_session_id ON lawyers.referrals (session_id);
CREATE INDEX idx_referrals_lawyer_id  ON lawyers.referrals (lawyer_id);

-- ── legal_documents.documents ────────────────────────────────
CREATE TABLE legal_documents.documents (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(500)  NOT NULL,
    short_name       VARCHAR(100)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    version_label    VARCHAR(50),
    source_url       VARCHAR(1000),
    file_path        VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version          INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_documents_status   ON legal_documents.documents (status);
CREATE INDEX idx_documents_category ON legal_documents.documents (category);

-- ── legal_documents.chunks ───────────────────────────────────
CREATE TABLE legal_documents.chunks (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES legal_documents.documents (id) ON DELETE CASCADE,
    chunk_index INT         NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version     INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_chunks_document_id ON legal_documents.chunks (document_id);

-- ── analytics.query_logs ─────────────────────────────────────
CREATE TABLE analytics.query_logs (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id       UUID,
    user_id          UUID,
    query_text       TEXT        NOT NULL,
    outcome          VARCHAR(20) NOT NULL,
    category         VARCHAR(100),
    response_time_ms BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version          INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_query_logs_outcome    ON analytics.query_logs (outcome);
CREATE INDEX idx_query_logs_created_at ON analytics.query_logs (created_at);
CREATE INDEX idx_query_logs_category   ON analytics.query_logs (category);
