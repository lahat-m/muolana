-- ============================================================
--  V4 — Admin module: legal document registry
--
--  Tables:
--    legal_documents    Master registry of all uploaded legal texts
--    document_chunks    Chunked + embedded segments (owned by Admin,
--                       read by RAG via pgvector similarity search)
--
--  Ownership note: ingestion pipeline (Admin module) writes
--  document_chunks. RAG module reads them. Separate tables to
--  preserve module ownership boundaries.
-- ============================================================

-- ── Legal documents ──────────────────────────────────────────

CREATE TYPE document_status AS ENUM (
    'PENDING',      -- uploaded, awaiting admin verification
    'VERIFIED',     -- verified — triggers ingestion pipeline
    'INGESTED',     -- chunked and embedded in pgvector
    'SUPERSEDED',   -- replaced by a newer version
    'REJECTED'      -- failed verification
);

CREATE TABLE legal_documents (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    uploaded_by         UUID        NOT NULL REFERENCES users (id),
    title               TEXT        NOT NULL,
    short_name          TEXT        NOT NULL,               -- e.g. Land Act 2009
    jurisdiction        TEXT        NOT NULL DEFAULT 'South Sudan',
    category            TEXT        NOT NULL,               -- e.g. Constitutional, Land, Commercial
    version_label       TEXT        NOT NULL,               -- e.g. 2011, 2015-amendment
    source_url          TEXT,                               -- official gazette or ministry URL
    file_path           TEXT        NOT NULL,               -- object storage path
    file_mime_type      TEXT        NOT NULL,               -- application/pdf or application/vnd.openxmlformats...
    file_size_bytes     BIGINT,
    status              document_status NOT NULL DEFAULT 'PENDING',
    verified_by         UUID        REFERENCES users (id),
    verified_at         TIMESTAMPTZ,
    rejection_reason    TEXT,
    ingested_at         TIMESTAMPTZ,
    superseded_by       UUID        REFERENCES legal_documents (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_legal_docs_status    ON legal_documents (status);
CREATE INDEX idx_legal_docs_category  ON legal_documents (category);
CREATE INDEX idx_legal_docs_short     ON legal_documents (short_name);

-- ── Document chunks (written by ingestion, read by RAG) ──────

CREATE TABLE document_chunks (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id     UUID        NOT NULL REFERENCES legal_documents (id) ON DELETE CASCADE,
    chunk_index     INTEGER     NOT NULL,               -- ordinal position within document
    article_ref     TEXT,                               -- e.g. "Art. 18(2)" parsed from text
    section_ref     TEXT,                               -- e.g. "Part III, Section 4"
    chunk_text      TEXT        NOT NULL,               -- raw text of the chunk
    embedding       vector(1536) NOT NULL,              -- text-embedding-3-small dimensions
    token_count     INTEGER     NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (document_id, chunk_index)
);

-- ivfflat index for fast approximate nearest-neighbour search
-- lists=100 is appropriate for up to ~1M vectors; tune upward as corpus grows
CREATE INDEX idx_chunks_embedding ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_chunks_document  ON document_chunks (document_id);
CREATE INDEX idx_chunks_article   ON document_chunks (article_ref);
