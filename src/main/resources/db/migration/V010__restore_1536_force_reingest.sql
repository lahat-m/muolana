-- Restore embedding column to 1536 dims and clear stale data.
-- V009 (now removed) switched the column to 768 dims for text-embedding-004.
-- This migration restores 1536 dims for gemini-embedding-001 and clears all
-- existing vectors and document records so DocumentSeeder re-ingests on startup.

DROP INDEX IF EXISTS idx_vector_store_embedding;

ALTER TABLE legal_documents.vector_store DROP COLUMN IF EXISTS embedding;
ALTER TABLE legal_documents.vector_store ADD COLUMN embedding vector(1536);

CREATE INDEX idx_vector_store_embedding
    ON legal_documents.vector_store
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Clear stale data — DocumentSeeder will re-ingest all documents on next startup.
TRUNCATE TABLE legal_documents.vector_store;
DELETE FROM legal_documents.chunks;
DELETE FROM legal_documents.documents;
