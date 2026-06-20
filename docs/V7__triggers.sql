-- ============================================================
--  V7 — Triggers
--
--  1. updated_at auto-maintenance on mutable tables
--  2. Lawyer average_rating recompute on review insert/delete
-- ============================================================

-- ── Generic updated_at trigger function ──────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Apply to every table with an updated_at column

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_legal_docs_updated_at
    BEFORE UPDATE ON legal_documents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_lawyers_updated_at
    BEFORE UPDATE ON lawyers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_referrals_updated_at
    BEFORE UPDATE ON lawyer_referrals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Lawyer rating recompute ───────────────────────────────────
--  Keeps lawyers.average_rating and lawyers.total_reviews in sync
--  whenever a review is inserted or deleted.
--  Using a trigger rather than a view keeps reads fast (no aggregation
--  on every read) and keeps writes consistent without a separate job.

CREATE OR REPLACE FUNCTION recompute_lawyer_rating()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_lawyer_id UUID;
BEGIN
    -- Determine which lawyer to update
    IF TG_OP = 'DELETE' THEN
        v_lawyer_id := OLD.lawyer_id;
    ELSE
        v_lawyer_id := NEW.lawyer_id;
    END IF;

    UPDATE lawyers
    SET
        average_rating = (
            SELECT ROUND(AVG(rating)::NUMERIC, 2)
            FROM   lawyer_reviews
            WHERE  lawyer_id = v_lawyer_id
        ),
        total_reviews = (
            SELECT COUNT(*)
            FROM   lawyer_reviews
            WHERE  lawyer_id = v_lawyer_id
        )
    WHERE id = v_lawyer_id;

    RETURN NULL; -- AFTER trigger; return value ignored
END;
$$;

CREATE TRIGGER trg_lawyer_rating
    AFTER INSERT OR DELETE ON lawyer_reviews
    FOR EACH ROW EXECUTE FUNCTION recompute_lawyer_rating();

-- ── Referral counter on lawyers ───────────────────────────────
--  Increments lawyers.total_referrals on each new referral row.

CREATE OR REPLACE FUNCTION increment_lawyer_referrals()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE lawyers
    SET total_referrals = total_referrals + 1
    WHERE id = NEW.lawyer_id;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_lawyer_referral_count
    AFTER INSERT ON lawyer_referrals
    FOR EACH ROW EXECUTE FUNCTION increment_lawyer_referrals();
