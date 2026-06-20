-- ============================================================
--  V5 — Lawyer module
--
--  Tables:
--    lawyers                  SSLS-verified advocate profiles
--    lawyer_specialisations   Many-to-many practice areas per lawyer
--    lawyer_languages         Languages spoken by a lawyer
--    lawyer_contact_methods   Available contact channels per lawyer
-- ============================================================

-- ── Lawyers ──────────────────────────────────────────────────

CREATE TYPE lawyer_status AS ENUM (
    'PENDING',      -- submitted by admin, awaiting approval
    'ACTIVE',       -- verified and visible to citizens
    'SUSPENDED',    -- temporarily hidden
    'REMOVED'       -- permanently delisted
);

CREATE TYPE fee_type AS ENUM ('FREE', 'PAID', 'PRO_BONO');

CREATE TABLE lawyers (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name           TEXT        NOT NULL,
    bar_number          TEXT        NOT NULL UNIQUE,       -- SSLS registration number
    bio                 TEXT,
    location_city       TEXT        NOT NULL,              -- e.g. Juba, Wau, Malakal
    location_state      TEXT,
    fee_type            fee_type    NOT NULL DEFAULT 'PAID',
    years_experience    INTEGER,
    status              lawyer_status NOT NULL DEFAULT 'PENDING',
    approved_by         UUID        REFERENCES users (id),
    approved_at         TIMESTAMPTZ,
    rejection_reason    TEXT,
    average_rating      NUMERIC(3,2),                     -- computed, updated on review insert
    total_reviews       INTEGER     NOT NULL DEFAULT 0,
    total_referrals     INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lawyers_status    ON lawyers (status);
CREATE INDEX idx_lawyers_city      ON lawyers (location_city);
CREATE INDEX idx_lawyers_fee       ON lawyers (fee_type);
CREATE INDEX idx_lawyers_bar       ON lawyers (bar_number);

-- ── Practice area specialisations ────────────────────────────

CREATE TABLE lawyer_specialisations (
    id          UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    lawyer_id   UUID    NOT NULL REFERENCES lawyers (id) ON DELETE CASCADE,
    area        TEXT    NOT NULL,                          -- e.g. Family, Land, Criminal, Commercial
    UNIQUE (lawyer_id, area)
);

CREATE INDEX idx_lawyer_spec_lawyer ON lawyer_specialisations (lawyer_id);
CREATE INDEX idx_lawyer_spec_area   ON lawyer_specialisations (area);

-- ── Languages ────────────────────────────────────────────────

CREATE TABLE lawyer_languages (
    id          UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    lawyer_id   UUID    NOT NULL REFERENCES lawyers (id) ON DELETE CASCADE,
    language    TEXT    NOT NULL,                          -- e.g. English, Dinka, Arabic, Nuer
    UNIQUE (lawyer_id, language)
);

-- ── Contact methods ──────────────────────────────────────────

CREATE TYPE contact_channel AS ENUM ('PHONE', 'EMAIL', 'CALLBACK');

CREATE TABLE lawyer_contact_methods (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    lawyer_id   UUID            NOT NULL REFERENCES lawyers (id) ON DELETE CASCADE,
    channel     contact_channel NOT NULL,
    value       TEXT            NOT NULL,                  -- phone number or email address
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    UNIQUE (lawyer_id, channel, value)
);

-- ── Lawyer reviews (from citizens) ───────────────────────────

CREATE TABLE lawyer_reviews (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    lawyer_id   UUID        NOT NULL REFERENCES lawyers (id) ON DELETE CASCADE,
    session_id  UUID        REFERENCES sessions (id) ON DELETE SET NULL,
    rating      SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT,
    is_anonymous BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_lawyer ON lawyer_reviews (lawyer_id);

-- ── Lawyer referrals ─────────────────────────────────────────

CREATE TYPE referral_status AS ENUM ('SENT', 'VIEWED', 'RESPONDED');

CREATE TABLE lawyer_referrals (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id      UUID            NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    lawyer_id       UUID            NOT NULL REFERENCES lawyers (id),
    channel         contact_channel NOT NULL,
    context_summary TEXT            NOT NULL,              -- AI-generated summary shared with consent
    consent_given   BOOLEAN         NOT NULL DEFAULT FALSE,
    status          referral_status NOT NULL DEFAULT 'SENT',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_referrals_session ON lawyer_referrals (session_id);
CREATE INDEX idx_referrals_lawyer  ON lawyer_referrals (lawyer_id);
