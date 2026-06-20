-- ============================================================
--  V6 — Analytics and audit
--
--  Tables:
--    query_log           Every query attempt with RAG outcome
--    analytics_events    Aggregable event stream for the analytics engine
--    audit_log           Immutable record of all admin actions
-- ============================================================

-- ── Query log (RAG module writes, analytics engine reads) ────

CREATE TYPE query_outcome AS ENUM (
    'ANSWERED',         -- LLM called, citation returned
    'NOT_FOUND',        -- hallucination guard triggered — no chunk above threshold
    'ERROR'             -- upstream failure
);

CREATE TABLE query_log (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id          UUID            NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    message_id          UUID            REFERENCES messages (id) ON DELETE SET NULL,
    query_text          TEXT            NOT NULL,
    outcome             query_outcome   NOT NULL,
    chunks_retrieved    INTEGER         NOT NULL DEFAULT 0,
    top_cosine_score    FLOAT,
    document_ids_cited  UUID[],                        -- legal_documents referenced
    model               TEXT,
    latency_ms          INTEGER,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_query_log_session  ON query_log (session_id);
CREATE INDEX idx_query_log_outcome  ON query_log (outcome);
CREATE INDEX idx_query_log_created  ON query_log (created_at);

-- ── Analytics events ─────────────────────────────────────────
--  Thin event stream; the analytics engine aggregates on the fly.
--  One row per user action worth tracking.

CREATE TABLE analytics_events (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type      TEXT        NOT NULL,   -- e.g. query.submitted, lawyer.referral.sent,
                                            --      doc.ingested, session.started
    actor_id        UUID        REFERENCES users (id) ON DELETE SET NULL,
    session_id      UUID        REFERENCES sessions (id) ON DELETE SET NULL,
    entity_type     TEXT,                   -- e.g. legal_document, lawyer, session
    entity_id       UUID,
    metadata        JSONB,                  -- event-specific payload (flexible)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_type     ON analytics_events (event_type);
CREATE INDEX idx_analytics_created  ON analytics_events (created_at);
CREATE INDEX idx_analytics_entity   ON analytics_events (entity_type, entity_id);

-- ── Audit log (append-only, no deletes, no updates) ──────────

CREATE TYPE audit_action AS ENUM (
    'USER_CREATED',
    'USER_ROLE_CHANGED',
    'USER_DEACTIVATED',
    'DOC_UPLOADED',
    'DOC_VERIFIED',
    'DOC_REJECTED',
    'DOC_INGESTED',
    'DOC_SUPERSEDED',
    'LAWYER_CREATED',
    'LAWYER_APPROVED',
    'LAWYER_REJECTED',
    'LAWYER_SUSPENDED',
    'LAWYER_REMOVED',
    'ADMIN_LOGIN',
    'ADMIN_LOGOUT'
);

CREATE TABLE audit_log (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    action          audit_action    NOT NULL,
    performed_by    UUID            NOT NULL REFERENCES users (id),
    entity_type     TEXT            NOT NULL,
    entity_id       UUID            NOT NULL,
    before_state    JSONB,          -- snapshot before change
    after_state     JSONB,          -- snapshot after change
    ip_address      INET,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- No UPDATE or DELETE index hints — this table is append-only
CREATE INDEX idx_audit_action  ON audit_log (action);
CREATE INDEX idx_audit_actor   ON audit_log (performed_by);
CREATE INDEX idx_audit_entity  ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_log (created_at);
