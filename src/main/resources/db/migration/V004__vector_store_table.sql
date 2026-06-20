-- Spring AI vector store table — owned by Flyway so Spring AI initialize-schema stays false.
-- Uses gen_random_uuid() (PG 13+ built-in) instead of uuid_generate_v4() from uuid-ossp.
CREATE TABLE IF NOT EXISTS legal_documents.vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSON,
    embedding vector(1536)
);

-- IVFFlat index for cosine similarity (matches COSINE_DISTANCE distance type)
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON legal_documents.vector_store
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
