-- ============================================================
-- V008 — Missing modules from architecture spec
--
-- Adapts docs/V2–V7 to the existing multi-schema structure:
--   auth / conversations / legal_documents / analytics / lawyers
--
-- Covers:
--   1. auth.token_revocations (new)
--   2. lawyers.lawyers        — missing columns
--   3. lawyers.referrals      — missing column
--   4. legal_documents.documents — missing columns
--   5. legal_documents.chunks    — missing columns
--   6. analytics.query_logs   — missing columns
--   7. analytics.analytics_events (new)
--   8. analytics.audit_log (new)
--   9. Triggers: lawyer rating recompute + referral counter
-- ============================================================


-- ── 1. auth.token_revocations ────────────────────────────────
--  Short-lived revocation table for access tokens.
--  A background job should prune rows where expires_at < NOW().

CREATE TABLE auth.token_revocations (
    jti         TEXT        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES auth.users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_revocations_expires ON auth.token_revocations (expires_at);
CREATE INDEX idx_token_revocations_user    ON auth.token_revocations (user_id);


-- ── 2. lawyers.lawyers — missing columns ─────────────────────

ALTER TABLE lawyers.lawyers
    ADD COLUMN IF NOT EXISTS location_state   TEXT,
    ADD COLUMN IF NOT EXISTS years_experience INTEGER,
    ADD COLUMN IF NOT EXISTS approved_by      UUID REFERENCES auth.users (id),
    ADD COLUMN IF NOT EXISTS approved_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS total_referrals  INTEGER NOT NULL DEFAULT 0;


-- ── 3. lawyers.referrals — missing column ────────────────────

ALTER TABLE lawyers.referrals
    ADD COLUMN IF NOT EXISTS context_summary TEXT;


-- ── 4. legal_documents.documents — missing columns ───────────

ALTER TABLE legal_documents.documents
    ADD COLUMN IF NOT EXISTS uploaded_by     UUID REFERENCES auth.users (id),
    ADD COLUMN IF NOT EXISTS jurisdiction    TEXT NOT NULL DEFAULT 'South Sudan',
    ADD COLUMN IF NOT EXISTS file_mime_type  TEXT,
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS verified_by     UUID REFERENCES auth.users (id),
    ADD COLUMN IF NOT EXISTS verified_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ingested_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS superseded_by   UUID REFERENCES legal_documents.documents (id);

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by
    ON legal_documents.documents (uploaded_by);


-- ── 5. legal_documents.chunks — missing columns ──────────────

ALTER TABLE legal_documents.chunks
    ADD COLUMN IF NOT EXISTS article_ref TEXT,
    ADD COLUMN IF NOT EXISTS section_ref TEXT,
    ADD COLUMN IF NOT EXISTS token_count INTEGER;

CREATE INDEX IF NOT EXISTS idx_chunks_article_ref
    ON legal_documents.chunks (article_ref)
    WHERE article_ref IS NOT NULL;


-- ── 6. analytics.query_logs — missing columns ────────────────

ALTER TABLE analytics.query_logs
    ADD COLUMN IF NOT EXISTS message_id        UUID REFERENCES conversations.messages (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS chunks_retrieved  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS top_cosine_score  FLOAT,
    ADD COLUMN IF NOT EXISTS document_ids_cited UUID[],
    ADD COLUMN IF NOT EXISTS model             TEXT,
    ADD COLUMN IF NOT EXISTS latency_ms        INTEGER;

CREATE INDEX IF NOT EXISTS idx_query_logs_message_id
    ON analytics.query_logs (message_id)
    WHERE message_id IS NOT NULL;


-- ── 7. analytics.analytics_events ────────────────────────────
--  Thin event stream for dashboards and analytics queries.
--  event_type examples: query.submitted, lawyer.referral.sent,
--                       doc.ingested, session.started

CREATE TABLE analytics.analytics_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  TEXT        NOT NULL,
    actor_id    UUID        REFERENCES auth.users (id) ON DELETE SET NULL,
    session_id  UUID        REFERENCES conversations.sessions (id) ON DELETE SET NULL,
    entity_type TEXT,
    entity_id   UUID,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_events_type    ON analytics.analytics_events (event_type);
CREATE INDEX idx_analytics_events_created ON analytics.analytics_events (created_at);
CREATE INDEX idx_analytics_events_entity  ON analytics.analytics_events (entity_type, entity_id);
CREATE INDEX idx_analytics_events_session ON analytics.analytics_events (session_id)
    WHERE session_id IS NOT NULL;


-- ── 8. analytics.audit_log ───────────────────────────────────
--  Append-only. No UPDATE or DELETE ever issued against this table.

CREATE TABLE analytics.audit_log (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    action       TEXT        NOT NULL,
    performed_by UUID        NOT NULL REFERENCES auth.users (id),
    entity_type  TEXT        NOT NULL,
    entity_id    UUID        NOT NULL,
    before_state JSONB,
    after_state  JSONB,
    ip_address   INET,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_action  ON analytics.audit_log (action);
CREATE INDEX idx_audit_log_actor   ON analytics.audit_log (performed_by);
CREATE INDEX idx_audit_log_entity  ON analytics.audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON analytics.audit_log (created_at);


-- ── 9. Triggers ──────────────────────────────────────────────
--  Note: updated_at on JPA-managed tables is handled by Hibernate
--  @UpdateTimestamp — no DB trigger needed for those.
--  These triggers cover business logic only.

-- Recompute lawyers.average_rating and review_count on review insert/delete.
CREATE OR REPLACE FUNCTION recompute_lawyer_rating()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE v_lawyer_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_lawyer_id := OLD.lawyer_id;
    ELSE
        v_lawyer_id := NEW.lawyer_id;
    END IF;

    UPDATE lawyers.lawyers
    SET average_rating = (
            SELECT ROUND(AVG(rating)::NUMERIC, 2)
            FROM lawyers.reviews
            WHERE lawyer_id = v_lawyer_id
        ),
        review_count = (
            SELECT COUNT(*)
            FROM lawyers.reviews
            WHERE lawyer_id = v_lawyer_id
        )
    WHERE id = v_lawyer_id;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_lawyer_rating
    AFTER INSERT OR DELETE ON lawyers.reviews
    FOR EACH ROW EXECUTE FUNCTION recompute_lawyer_rating();

-- Increment lawyers.total_referrals on each new referral.
CREATE OR REPLACE FUNCTION increment_lawyer_referrals()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE lawyers.lawyers
    SET total_referrals = total_referrals + 1
    WHERE id = NEW.lawyer_id;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_lawyer_referral_count
    AFTER INSERT ON lawyers.referrals
    FOR EACH ROW EXECUTE FUNCTION increment_lawyer_referrals();
