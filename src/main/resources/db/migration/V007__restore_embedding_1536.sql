-- Restore OpenAI text-embedding-3-small (1536 dims), reverting V006's Gemini 768-dim column.
-- Uses DROP/ADD so it is safe whether V006 ran or not.
DROP INDEX IF EXISTS idx_vector_store_embedding;

ALTER TABLE legal_documents.vector_store DROP COLUMN IF EXISTS embedding;
ALTER TABLE legal_documents.vector_store ADD COLUMN embedding vector(1536);

CREATE INDEX idx_vector_store_embedding
    ON legal_documents.vector_store
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
